/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package com.github.opengrabeso.ogltext.util.glsl;

import java.io.PrintStream;
import java.nio.*;

import com.github.opengrabeso.jaagl.*;

import com.jogamp.common.nio.Buffers;

public class ShaderUtil {
    public static String getShaderInfoLog(final GL _gl, final int shaderObj) {
        final GL2GL3 gl = _gl.getGL2GL3();
        final int[] infoLogLength=new int[1];
        gl.glGetShaderiv(shaderObj, gl.GL_INFO_LOG_LENGTH(), infoLogLength, 0);

        if(infoLogLength[0]==0) {
            return "(no info log)";
        }
        final int[] charsWritten=new int[1];
        final byte[] infoLogBytes = new byte[infoLogLength[0]];
        gl.glGetShaderInfoLog(shaderObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }

    public static String getProgramInfoLog(final GL _gl, final int programObj) {
        final GL2GL3 gl = _gl.getGL2GL3();
        final int[] infoLogLength=new int[1];
        gl.glGetProgramiv(programObj, gl.GL_INFO_LOG_LENGTH(), infoLogLength, 0);

        if(infoLogLength[0]==0) {
            return "(no info log)";
        }
        final int[] charsWritten=new int[1];
        final byte[] infoLogBytes = new byte[infoLogLength[0]];
        gl.glGetProgramInfoLog(programObj, infoLogLength[0], charsWritten, 0, infoLogBytes, 0);

        return new String(infoLogBytes, 0, charsWritten[0]);
    }

    public static boolean isShaderStatusValid(final GL _gl, final int shaderObj, final int name, final PrintStream verboseOut) {
        final GL2GL3 gl = _gl.getGL2GL3();
        final int[] ires = new int[1];
        gl.glGetShaderiv(shaderObj, name, ires, 0);

        final boolean res = ires[0]==1;
        if(!res && null!=verboseOut) {
            verboseOut.println("Shader status invalid: "+ getShaderInfoLog(gl, shaderObj));
        }
        return res;
    }

    public static boolean isShaderStatusValid(final GL _gl, final IntBuffer shaders, final int name, final PrintStream verboseOut) {
        boolean res = true;
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            res = isShaderStatusValid(_gl, shaders.get(i), name, verboseOut) && res;
        }
        return res;
    }

    public static boolean isProgramStatusValid(final GL _gl, final int programObj, final int name) {
        final GL2GL3 gl = _gl.getGL2GL3();
        final int[] ires = new int[1];
        gl.glGetProgramiv(programObj, name, ires, 0);

        return ires[0]==1;
    }

    public static boolean isProgramLinkStatusValid(final GL _gl, final int programObj, final PrintStream verboseOut) {
        final GL2GL3 gl = _gl.getGL2GL3();
        if(!gl.glIsProgram(programObj)) {
            if(null!=verboseOut) {
                verboseOut.println("Program name invalid: "+programObj);
            }
            return false;
        }
        if(!isProgramStatusValid(gl, programObj, gl.GL_LINK_STATUS())) {
            if(null!=verboseOut) {
                verboseOut.println("Program link failed: "+programObj+"\n\t"+ getProgramInfoLog(gl, programObj));
            }
            return false;
        }
        return true;
    }

    /**
     * Performs {@link GL2GL3#glValidateProgram(int)}
     * <p>
     * One shall only call this method while debugging and only if all required
     * resources by the shader are set.
     * </p>
     * <p>
     * Note: It is possible that a working shader program will fail validation.
     * This has been experienced on NVidia APX2500 and Tegra2.
     * </p>
     * @see GL2GL3#glValidateProgram(int)
     **/
    public static boolean isProgramExecStatusValid(final GL _gl, final int programObj, final PrintStream verboseOut) {
        final GL2GL3 gl = _gl.getGL2GL3();
        gl.glValidateProgram(programObj);
        if(!isProgramStatusValid(gl, programObj, gl.GL_VALIDATE_STATUS())) {
            if(null!=verboseOut) {
                verboseOut.println("Program validation failed: "+programObj+"\n\t"+ getProgramInfoLog(gl, programObj));
            }
            return false;
        }
        return true;
    }

    public static void createShader(final GL _gl, final int type, final IntBuffer shaders) {
        final GL2GL3 gl = _gl.getGL2GL3();
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            shaders.put(i, gl.glCreateShader(type));
        }
    }

    public static void shaderSource(final GL _gl, final int shader, final CharSequence[] source)
    {
        final GL2GL3 gl = _gl.getGL2GL3();

        final int count = (null!=source)?source.length:0;
        if(count==0) {
            throw gl.newGLException("No sources specified");
        }

        final IntBuffer lengths = Buffers.newDirectIntBuffer(count);
        for(int i=0; i<count; i++) {
            lengths.put(i, source[i].length());
        }
        if(source instanceof String[]) {
            // rare case ..
            gl.glShaderSource(shader, count, (String[])source, lengths);
        } else {
            final String[] tmp = new String[source.length];
            for(int i = source.length - 1; i>=0; i--) {
                final CharSequence csq = source[i];
                if(csq instanceof String) {
                    // if ShaderCode.create(.. mutableStringBuilder == false )
                    tmp[i] = (String) csq;
                } else {
                    // if ShaderCode.create(.. mutableStringBuilder == true )
                    tmp[i] = source[i].toString();
                }
            }
            gl.glShaderSource(shader, count, tmp, lengths);
        }
    }

    public static void shaderSource(final GL _gl, final IntBuffer shaders, final CharSequence[][] sources)
    {
        final int sourceNum = (null!=sources)?sources.length:0;
        final int shaderNum = (null!=shaders)?shaders.remaining():0;
        if(shaderNum<=0 || sourceNum<=0 || shaderNum!=sourceNum) {
            throw _gl.newGLException("Invalid number of shaders and/or sources: shaders="+
                                  shaderNum+", sources="+sourceNum);
        }
        for(int i=0; i<sourceNum; i++) {
            shaderSource(_gl, shaders.get(shaders.position() + i), sources[i]);
        }
    }

    public static void shaderBinary(final GL _gl, final IntBuffer shaders, final int binFormat, final java.nio.Buffer bin)
    {
    }

    public static void compileShader(final GL _gl, final IntBuffer shaders)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            gl.glCompileShader(shaders.get(i));
        }
    }

    public static void attachShader(final GL _gl, final int program, final IntBuffer shaders)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            gl.glAttachShader(program, shaders.get(i));
        }
    }

    public static void detachShader(final GL _gl, final int program, final IntBuffer shaders)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            gl.glDetachShader(program, shaders.get(i));
        }
    }

    public static void deleteShader(final GL _gl, final IntBuffer shaders)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        for (int i = shaders.position(); i < shaders.limit(); i++) {
            gl.glDeleteShader(shaders.get(i));

        }
    }

    public static boolean createAndLoadShader(final GL _gl, final IntBuffer shader, final int shaderType,
                                              final int binFormat, final java.nio.Buffer bin,
                                              final PrintStream verboseOut)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        int err = gl.glGetError(); // flush previous errors ..
        if(err!=gl.GL_NO_ERROR() && null!=verboseOut) {
            verboseOut.println("createAndLoadShader: Pre GL Error: 0x"+Integer.toHexString(err));
        }

        createShader(gl, shaderType, shader);
        err = gl.glGetError();
        if(err!=gl.GL_NO_ERROR()) {
            throw gl.newGLException("createAndLoadShader: CreateShader failed, GL Error: 0x"+Integer.toHexString(err));
        }

        shaderBinary(gl, shader, binFormat, bin);

        err = gl.glGetError();
        if(err!=gl.GL_NO_ERROR() && null!=verboseOut) {
            verboseOut.println("createAndLoadShader: ShaderBinary failed, GL Error: 0x"+Integer.toHexString(err));
        }
        return err == gl.GL_NO_ERROR();
    }

    public static boolean createAndCompileShader(final GL _gl, final IntBuffer shader, final int shaderType,
                                                 final CharSequence[][] sources,
                                                 final PrintStream verboseOut)
    {
        final GL2GL3 gl = _gl.getGL2GL3();
        int err = gl.glGetError(); // flush previous errors ..
        if(err!=gl.GL_NO_ERROR() && null!=verboseOut) {
            verboseOut.println("createAndCompileShader: Pre GL Error: 0x"+Integer.toHexString(err));
        }

        createShader(gl, shaderType, shader);
        err = gl.glGetError();
        if(err!=gl.GL_NO_ERROR()) {
            throw gl.newGLException("createAndCompileShader: CreateShader failed, GL Error: 0x"+Integer.toHexString(err));
        }

        shaderSource(gl, shader, sources);
        err = gl.glGetError();
        if(err!=gl.GL_NO_ERROR()) {
            throw gl.newGLException("createAndCompileShader: ShaderSource failed, GL Error: 0x"+Integer.toHexString(err));
        }

        compileShader(gl, shader);
        err = gl.glGetError();
        if(err!=gl.GL_NO_ERROR() && null!=verboseOut) {
            verboseOut.println("createAndCompileShader: CompileShader failed, GL Error: 0x"+Integer.toHexString(err));
        }

        return isShaderStatusValid(gl, shader, gl.GL_COMPILE_STATUS(), verboseOut) && err == gl.GL_NO_ERROR();
    }

}
