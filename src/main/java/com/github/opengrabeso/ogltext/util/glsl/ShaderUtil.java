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

    public static boolean isProgramStatusValid(final GL _gl, final int programObj, final int name) {
        final GL2GL3 gl = _gl.getGL2GL3();
        final int[] ires = new int[1];
        gl.glGetProgramiv(programObj, name, ires, 0);

        return ires[0]==1;
    }


}
