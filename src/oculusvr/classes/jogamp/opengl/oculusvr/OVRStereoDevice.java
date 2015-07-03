/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.opengl.oculusvr;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.OvrHmdContext;
import com.jogamp.oculusvr.ovrEyeRenderDesc;
import com.jogamp.oculusvr.ovrFovPort;
import com.jogamp.oculusvr.ovrHmdDesc;
import com.jogamp.oculusvr.ovrSizei;
import com.jogamp.opengl.math.FovHVHalves;
import com.jogamp.opengl.util.stereo.StereoDevice;
import com.jogamp.opengl.util.stereo.StereoDeviceFactory;
import com.jogamp.opengl.util.stereo.StereoDeviceRenderer;
import com.jogamp.opengl.util.stereo.StereoUtil;

public class OVRStereoDevice implements StereoDevice {
    /** 1.6 up, 5 forward */
    private static final float[] DEFAULT_EYE_POSITION_OFFSET = { 0.0f, 1.6f, -5.0f };

    private final StereoDeviceFactory factory;
    public final int deviceIndex;
    private final FovHVHalves[] defaultEyeFov;

    public ovrHmdDesc hmdDesc;
    public OvrHmdContext handle;
    private boolean sensorsStarted = false;
    private final int[] eyeRenderOrder;
    private final int supportedDistortionBits, recommendedDistortionBits, minimumDistortionBits;

    private final String deviceName;
    private final DimensionImmutable resolution;
    private final int requiredRotation;
    private final PointImmutable position;
    private final int dkVersion;

    public OVRStereoDevice(final StereoDeviceFactory factory, final ovrHmdDesc hmdDesc, final int deviceIndex) {
        if( null == hmdDesc ) {
            throw new IllegalArgumentException("Passed null hmdDesc");
        }
        final OvrHmdContext nativeContext = hmdDesc.getHandle();
        if( null == nativeContext ) {
            throw new IllegalArgumentException("hmdDesc has null OvrHmdContext");
        }
        this.factory = factory;
        this.handle = nativeContext;
        this.deviceIndex = deviceIndex;
        this.hmdDesc = hmdDesc;
        final ovrFovPort[] defaultOVREyeFov = hmdDesc.getDefaultEyeFov(0, new ovrFovPort[ovrHmdDesc.getEyeRenderOrderArrayLength()]);
        defaultEyeFov = new FovHVHalves[defaultOVREyeFov.length];
        for(int i=0; i<defaultEyeFov.length; i++) {
            defaultEyeFov[i] = OVRUtil.getFovHV(defaultOVREyeFov[i]);
        }
        eyeRenderOrder = new int[ovrHmdDesc.getEyeRenderOrderArrayLength()];
        hmdDesc.getEyeRenderOrder(0, eyeRenderOrder);
        supportedDistortionBits = OVRUtil.ovrDistCaps2DistBits(hmdDesc.getDistortionCaps());
        recommendedDistortionBits = supportedDistortionBits & ~StereoDeviceRenderer.DISTORTION_TIMEWARP;
        minimumDistortionBits = StereoDeviceRenderer.DISTORTION_BARREL;

        // DK1 delivers unrotated resolution in target orientation
        // DK2 delivers rotated resolution in target orientation, monitor screen is rotated 90deg clockwise
        deviceName = hmdDesc.getDisplayDeviceNameAsString();
        final ovrSizei res = hmdDesc.getResolution();
        resolution = new Dimension(res.getW(), res.getH());
        final int hmdType = hmdDesc.getType();
        switch( hmdType ) {
            case OVR.ovrHmd_DKHD:             // 4
            case 5:                           // OVR.ovrHmd_CrystalCoveProto:
            case OVR.ovrHmd_DK2:              // 6
                dkVersion = 2;
                requiredRotation = 90;
                break;
            default:
                dkVersion = 1;
                requiredRotation = 0;
                break;
        }
        position = OVRUtil.getVec2iAsPoint(hmdDesc.getWindowsPos());
    }

    @Override
    public final StereoDeviceFactory getFactory() { return factory; }

    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("OVRStereoDevice[product "+hmdDesc.getProductNameAsString());
        sb.append(", vendor "+hmdDesc.getManufacturerAsString());
        sb.append(", device "+deviceName);
        sb.append(", DK "+dkVersion);
        sb.append(", surfaceSize "+getSurfaceSize()+", reqRotation "+requiredRotation+" ccw-deg");
        sb.append(", surfacePos "+getPosition());
        sb.append(", distortionBits[supported ["+StereoUtil.distortionBitsToString(getSupportedDistortionBits())+
                      "], recommended ["+StereoUtil.distortionBitsToString(getRecommendedDistortionBits())+
                      "], minimum ["+StereoUtil.distortionBitsToString(getMinimumDistortionBits())+"]]]");
        return sb.toString();
    }

    @Override
    public final void dispose() {
       if( isValid() ) {
           OVR.ovrHmd_Destroy(hmdDesc);
           hmdDesc = null;
           handle = null;
       }
    }

    @Override
    public boolean isValid() {
        return null != hmdDesc && null != handle;
    }

    @Override
    public final PointImmutable getPosition() { return position; }

    @Override
    public final DimensionImmutable getSurfaceSize() { return resolution; }
    @Override
    public int getRequiredRotation() { return requiredRotation; }

    @Override
    public float[] getDefaultEyePositionOffset() {
        return DEFAULT_EYE_POSITION_OFFSET;
    }

    @Override
    public final FovHVHalves[] getDefaultFOV() {
        return defaultEyeFov;
    }

    @Override
    public final boolean startSensors(final boolean start) {
        if( start && !sensorsStarted ) {
            // Start the sensor which provides the Rift’s pose and motion.
            final int requiredTrackingCaps = 0;
            final int supportedTrackingCaps = requiredTrackingCaps |
                                            OVR.ovrTrackingCap_Orientation |
                                            OVR.ovrTrackingCap_MagYawCorrection |
                                            OVR.ovrTrackingCap_Position;
            if( OVR.ovrHmd_ConfigureTracking(hmdDesc, supportedTrackingCaps, requiredTrackingCaps) ) {
                sensorsStarted = true;
                return true;
            } else {
                sensorsStarted = false;
                return false;
            }
        } else if( sensorsStarted ) {
            OVR.ovrHmd_ConfigureTracking(hmdDesc, 0, 0); // STOP
            sensorsStarted = false;
            return true;
        } else {
            // No state change -> Success
            return true;
        }
    }
    @Override
    public final boolean getSensorsStarted() { return sensorsStarted; }

    @Override
    public final int[] getEyeRenderOrder() {
        return eyeRenderOrder;
    }

    @Override
    public final int getSupportedDistortionBits() {
        return supportedDistortionBits;
    };

    @Override
    public final int getRecommendedDistortionBits() {
        return recommendedDistortionBits;
    }

    @Override
    public final int getMinimumDistortionBits() {
        return minimumDistortionBits;
    }

    @Override
    public final StereoDeviceRenderer createRenderer(final int distortionBits,
                                                     final int textureCount, final float[] eyePositionOffset,
                                                     final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel, final int textureUnit) {
        final ovrFovPort ovrEyeFov0 = OVRUtil.getOVRFovPort(eyeFov[0]);
        final ovrFovPort ovrEyeFov1 = OVRUtil.getOVRFovPort(eyeFov[1]);

        final ovrEyeRenderDesc[] eyeRenderDesc = new ovrEyeRenderDesc[2];
        eyeRenderDesc[0] = OVR.ovrHmd_GetRenderDesc(hmdDesc, OVR.ovrEye_Left, ovrEyeFov0);
        eyeRenderDesc[1] = OVR.ovrHmd_GetRenderDesc(hmdDesc, OVR.ovrEye_Right, ovrEyeFov1);
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: eyeRenderDesc[0] "+OVRUtil.toString(eyeRenderDesc[0]));
            System.err.println("XXX: eyeRenderDesc[1] "+OVRUtil.toString(eyeRenderDesc[1]));
        }

        final DimensionImmutable eye0TextureSize = OVRUtil.getOVRSizei(OVR.ovrHmd_GetFovTextureSize(hmdDesc, OVR.ovrEye_Left,  eyeRenderDesc[0].getFov(), pixelsPerDisplayPixel));
        final DimensionImmutable eye1TextureSize = OVRUtil.getOVRSizei(OVR.ovrHmd_GetFovTextureSize(hmdDesc, OVR.ovrEye_Right, eyeRenderDesc[1].getFov(), pixelsPerDisplayPixel));
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: recommenedTex0Size "+eye0TextureSize);
            System.err.println("XXX: recommenedTex1Size "+eye1TextureSize);
        }
        // final int maxWidth = Math.max(eye0TextureSize.getWidth(), eye1TextureSize.getWidth());
        final int maxHeight = Math.max(eye0TextureSize.getHeight(), eye1TextureSize.getHeight());

        final DimensionImmutable[] eyeTextureSizes = new DimensionImmutable[] { eye0TextureSize, eye1TextureSize };
        final DimensionImmutable totalTextureSize = new Dimension(eye0TextureSize.getWidth() + eye1TextureSize.getWidth(), maxHeight);
        if( StereoDevice.DEBUG ) {
            System.err.println("XXX: textureSize Single "+eyeTextureSizes);
            System.err.println("XXX: textureSize Total  "+totalTextureSize);
        }

        final RectangleImmutable[] eyeViewports = new RectangleImmutable[2];
        if( 1 == textureCount ) { // validated in ctor below!
            // one big texture/FBO, viewport to target space
            eyeViewports[0] = new Rectangle(0, 0,
                                            eye0TextureSize.getWidth(),
                                            maxHeight);
            eyeViewports[1] = new Rectangle(eye0TextureSize.getWidth(), 0,
                                            eye1TextureSize.getWidth(),
                                            maxHeight);
        } else {
            // two textures/FBOs w/ postprocessing, which renders textures/FBOs into target space
            eyeViewports[0] = new Rectangle(0, 0,
                                            eye0TextureSize.getWidth(),
                                            eye0TextureSize.getHeight());
            eyeViewports[1] = new Rectangle(0, 0,
                                            eye1TextureSize.getWidth(),
                                            eye1TextureSize.getHeight());
        }
        return new OVRStereoDeviceRenderer(this, distortionBits, textureCount, eyePositionOffset,
                                           eyeRenderDesc, eyeTextureSizes, totalTextureSize, eyeViewports, textureUnit);
    }
}