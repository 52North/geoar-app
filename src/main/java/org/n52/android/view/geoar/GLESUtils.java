/**
 * Copyright 2012 52°North Initiative for Geospatial Open Source Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.android.view.geoar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.opengl.GLES20;

public abstract class GLESUtils {

	private static final Logger LOG = LoggerFactory.getLogger(GLESUtils.class);

	public static int createProgram(String vertexSource, String fragmentSource) {
		int vertexShaderHandle = loadVertexShader(vertexSource);
		if (vertexShaderHandle == 0)
			return 0;

		int fragmentShaderHandle = loadFragmentShader(fragmentSource);
		if (fragmentShaderHandle == 0)
			return 0;

		int program = createProgram(vertexShaderHandle, fragmentShaderHandle);
		return program;
	}

	private static int createProgram(int vertexShaderHandle,
			int fragmentShaderHandle) {
		int programHandle = GLES20.glCreateProgram();
		if (programHandle != 0) {
			// bind vertex shader to the program
			GLES20.glAttachShader(programHandle, vertexShaderHandle);
			// bind fragment shader to the program
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
			GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
			GLES20.glBindAttribLocation(programHandle, 2, "a_Normal");
			// GLES20.glBindAttribLocation(programHandle, i, attributes[i]);
			// link shaders into the program
			GLES20.glLinkProgram(programHandle);
			// get the link status
			final int[] linkStatus = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS,
					linkStatus, 0);
			// check link status
			if (linkStatus[0] == 0) {
				GLES20.glDeleteProgram(programHandle);
				programHandle = 0;
				LOG.error("createProgram failed: "
						+ GLES20.glGetProgramInfoLog(programHandle));
				throw new RuntimeException("Error creating program");
			}
		}
		return programHandle;
	}

	private static int loadVertexShader(String source) {
		int shaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		if (shaderHandle != 0) {
			// Pass in the shader source
			GLES20.glShaderSource(shaderHandle, source);
			// Compile the shader
			GLES20.glCompileShader(shaderHandle);
			// get the compilation status
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS,
					compileStatus, 0);
			// check the compilation status
			if (compileStatus[0] == 0) {
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
				LOG.error("Vertexshader compilation failed: "
						+ GLES20.glGetShaderInfoLog(shaderHandle));
				throw new RuntimeException("Error creating vertex shader.");
			}
		}

		return shaderHandle;
	}

	private static int loadFragmentShader(String source) {
		int shaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		if (shaderHandle != 0) {
			// Pass in the shader source
			GLES20.glShaderSource(shaderHandle, source);
			// Compile the shader
			GLES20.glCompileShader(shaderHandle);
			// get the compilation status
			final int[] compileStatus = new int[1];
			GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS,
					compileStatus, 0);
			// check the compilation status
			if (compileStatus[0] == 0) {
				GLES20.glDeleteShader(shaderHandle);
				shaderHandle = 0;
				LOG.error("Fragmentshader compilation failed: "
						+ GLES20.glGetShaderInfoLog(shaderHandle));
				throw new RuntimeException("Error creating fragment shader.");
			}
		}

		return shaderHandle;
	}

}
