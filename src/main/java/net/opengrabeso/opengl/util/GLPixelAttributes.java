package net.opengrabeso.opengl.util;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.*;

import java.nio.Buffer;

/**
 * Pixel attributes.
 */
public class GLPixelAttributes {
    /**
     * Undefined instance of {@link GLPixelAttributes}, having componentCount:=0, format:=0 and type:= 0.
     */
    public static final GLPixelAttributes UNDEF = new GLPixelAttributes(PixelFormat.LUMINANCE, 0, 0);

    /**
     * Returns the matching {@link PixelFormat} for the given GL format and type if exists,
     * otherwise returns <code>null</code>.
     */
    public static final PixelFormat getPixelFormat(final int glFormat, final int glDataType) {
        PixelFormat pixFmt = null;

        switch (glFormat) {
            case GL.GL_ALPHA:
            case GL.GL_LUMINANCE:
            case GL2ES2.GL_RED:
                pixFmt = PixelFormat.LUMINANCE;
                break;
            case GL.GL_RGB:
                switch (glDataType) {
                    case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
                        pixFmt = PixelFormat.RGB565;
                        break;
                    case GL.GL_UNSIGNED_SHORT_5_6_5:
                        pixFmt = PixelFormat.BGR565;
                        break;
                    case GL.GL_UNSIGNED_BYTE:
                        pixFmt = PixelFormat.RGB888;
                        break;
                }
                break;
            case GL.GL_RGBA:
                switch (glDataType) {
                    case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                        pixFmt = PixelFormat.RGBA5551;
                        break;
                    case GL.GL_UNSIGNED_SHORT_5_5_5_1:
                        pixFmt = PixelFormat.ABGR1555;
                        break;
                    case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
                        // fall through intended
                    case GL.GL_UNSIGNED_BYTE:
                        pixFmt = PixelFormat.RGBA8888;
                        break;
                    case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
                        pixFmt = PixelFormat.ABGR8888;
                        break;
                }
                break;
            case GL.GL_BGR:
                if (GL.GL_UNSIGNED_BYTE == glDataType) {
                    pixFmt = PixelFormat.BGR888;
                }
                break;
            case GL.GL_BGRA:
                switch (glDataType) {
                    case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
                        pixFmt = PixelFormat.ARGB8888;
                        break;
                    case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
                        // fall through intended
                    case GL.GL_UNSIGNED_BYTE:
                        pixFmt = PixelFormat.BGRA8888;
                        break;
                }
                break;
        }
        return pixFmt;
    }


    /**
     * Returns the matching {@link GLPixelAttributes} for the given {@link GLProfile}, {@link PixelFormat} and {@code pack} if exists,
     * otherwise returns {@code null}.
     *
     * @param glp    the corresponding {@link GLProfile}
     * @param pixFmt the to be matched {@link PixelFormat pixel format}
     * @param pack   {@code true} for read mode GPU -> CPU, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, Buffer) glReadPixels}.
     *               {@code false} for write mode CPU -> GPU, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, Buffer) glTexImage2D}.
     */
    public static final GLPixelAttributes convert(final GLProfile glp, final PixelFormat pixFmt, final boolean pack) {
        final int[] df = new int[1];
        final int[] dt = new int[1];
        convert(glp, pixFmt, pack, df, dt);
        if (0 != df[0]) {
            return new GLPixelAttributes(pixFmt, df[0], dt[0]);
        }
        return null;
    }

    private static final int convert(final GLProfile glp, final PixelFormat pixFmt, final boolean pack,
                                     final int[] dfRes, final int[] dtRes) {
        final boolean glesReadMode = pack && glp.isGLES();
        int df = 0; // format
        int dt = GL.GL_UNSIGNED_BYTE; // data type
        switch (pixFmt) {
            case LUMINANCE:
                if (!glesReadMode) {
                    if (glp.isGL3ES3()) {
                        // RED is supported on ES3 and >= GL3 [core]; ALPHA/LUMINANCE is deprecated on core
                        df = GL2ES2.GL_RED;
                    } else {
                        // ALPHA/LUMINANCE is supported on ES2 and GL2, i.e. <= GL3 [core] or compatibility
                        df = GL.GL_LUMINANCE;
                    }
                }
                break;
            case RGB565:
                if (glp.isGL2GL3()) {
                    df = GL.GL_RGB;
                    dt = GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV;
                }
                break;
            case BGR565:
                if (glp.isGL2GL3()) {
                    df = GL.GL_RGB;
                    dt = GL.GL_UNSIGNED_SHORT_5_6_5;
                }
                break;
            case RGBA5551:
                if (glp.isGL2GL3()) {
                    df = GL.GL_RGBA;
                    dt = GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV;
                }
                break;
            case ABGR1555:
                if (glp.isGL2GL3()) {
                    df = GL.GL_RGBA;
                    dt = GL.GL_UNSIGNED_SHORT_5_5_5_1;
                }
                break;
            case RGB888:
                if (!glesReadMode) {
                    df = GL.GL_RGB;
                }
                break;
            case BGR888:
                if (glp.isGL2GL3()) {
                    df = GL.GL_BGR;
                }
                break;
            case RGBx8888:
            case RGBA8888:
                df = GL.GL_RGBA;
                break;
            case ABGR8888:
                if (glp.isGL2GL3()) {
                    df = GL.GL_RGBA;
                    dt = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
                }
                break;
            case ARGB8888:
                if (glp.isGL2GL3()) {
                    df = GL.GL_BGRA;
                    dt = GL2GL3.GL_UNSIGNED_INT_8_8_8_8;
                }
                break;
            case BGRx8888:
            case BGRA8888:
                if (glp.isGL2GL3()) { // FIXME: or if( !glesReadMode ) ? BGRA n/a on GLES
                    df = GL.GL_BGRA;
                }
                break;
        }
        dfRes[0] = df;
        dtRes[0] = dt;
        return df;
    }

    /**
     * The OpenGL pixel data format
     */
    public final int format;
    /**
     * The OpenGL pixel data type
     */
    public final int type;

    /**
     * {@link PixelFormat} describing the {@link PixelFormat.Composition component} layout
     */
    public final PixelFormat pfmt;

    @Override
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = pfmt.hashCode();
        hash = ((hash << 5) - hash) + format;
        return ((hash << 5) - hash) + type;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof GLPixelAttributes) {
            final GLPixelAttributes other = (GLPixelAttributes) obj;
            return format == other.format &&
                    type == other.type &&
                    pfmt.equals(other.pfmt);
        } else {
            return false;
        }
    }

    /**
     * Create a new {@link GLPixelAttributes} instance based on GL format and type.
     *
     * @param dataFormat GL data format
     * @param dataType   GL data type

     */
    public GLPixelAttributes(final int dataFormat, final int dataType) {
        this(null, dataFormat, dataType);
    }

    /**
     * Create a new {@link GLPixelAttributes} instance based on {@link GLProfile}, {@link PixelFormat} and {@code pack}.
     *
     * @param pixFmt the to be matched {@link PixelFormat pixel format}

     */

    private GLPixelAttributes(final PixelFormat pixFmt,
                              final int dataFormat, final int dataType) {
        this.format = dataFormat;
        this.type = dataType;
        this.pfmt = null != pixFmt ? pixFmt : getPixelFormat(dataFormat, dataType);
        if (null == this.pfmt) {
            throw new GLException("Could not find PixelFormat for format and/or type: " + this);
        }
    }

    @Override
    public String toString() {
        return "PixelAttributes[fmt 0x" + Integer.toHexString(format) + ", type 0x" + Integer.toHexString(type) + ", " + pfmt + "]";
    }
}
