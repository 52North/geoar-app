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
package org.n52.android.view.geoar.gl.mode;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.opengl.GLES20;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class FeatureShader {

	/** Size of the position data in elements. */
	static final int POSITION_DATA_SIZE = 3;
	/** Size of the normal data in elements. */
	static final int NORMAL_DATA_SIZE = 3;
	/** How many bytes per float. */
	static final int BYTES_PER_FLOAT = 4;

	private static final int BYTES_PER_SHORT = 2;

	static final int COLOR_DATA_SIZE = 4;

	/** OpenGL handles to our program uniforms */
	private static final String MVP_MATRIX_UNIFORM = "u_MVPMatrix";
	private static final String MV_MATRIX_UNIFORM = "u_MVMatrix";
	private static final String V_MATRIX_UNIFORM = "u_VMatrix";

	private static final String POSITION_ATTRIBUTE = "a_Position";
	private static final String NORMAL_ATTRIBUTE = "a_Normal";
	private static final String COLOR_ATTRIBUTE = "a_Color";
	private static final String TEXTURE_ATTRIBUTE = "a_TexCoordinate";

	private static final Logger LOG = LoggerFactory
			.getLogger(FeatureShader.class);

	private static final Set<WeakReference<FeatureShader>> INSTANCES = new HashSet<WeakReference<FeatureShader>>();

	public static void resetShaders() {
		Iterator<WeakReference<FeatureShader>> iterator = INSTANCES.iterator();
		while (iterator.hasNext()) {
			FeatureShader next = iterator.next().get();
			if (next == null) {
				iterator.remove();
			} else {
				next.reset();
			}
		}
	}

	private static void compileShader(final int shaderHandle,
			final String shaderProgram) {
		if (shaderHandle <= 0) {
			throw new IllegalArgumentException("Invalid handle");
		}

		GLES20.glShaderSource(shaderHandle, shaderProgram);
		GLES20.glCompileShader(shaderHandle);
		final int[] status = new int[1];
		GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, status, 0);

		if (status[0] == 0) {
			LOG.error(
					"FeatureShader",
					"Error compiling shader: "
							+ GLES20.glGetShaderInfoLog(shaderHandle));
			GLES20.glDeleteShader(shaderHandle);
		}

	}

	private int programHandle = -1;

	private int positionHandle = -1;
	private int colorHandle = -1;
	private int normalHandle = -1;

	private int mvpMatrixUniform = -1;
	private int mvMatrixUniform = -1;
	private int vMatrixUniform = -1;

	private final String vertexShader, fragmentShader;

	final boolean supportsNormals;
	final boolean supportsColors;
	final boolean supportsTextures;
	private int vertexShaderHandle = -1;
	private int fragmentShaderHandle = -1;

	public FeatureShader(String vertexShader, String fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;

		/** searches with the aid of regex for the normal attribute */
		Pattern pattern = Pattern.compile(".*" + NORMAL_ATTRIBUTE + ".*");
		Matcher matcher = pattern.matcher(vertexShader);
		supportsNormals = matcher.find();

		/** Searches with the aid of regex for color attribute */
		pattern = Pattern.compile(".*" + COLOR_ATTRIBUTE + ".*");
		matcher = pattern.matcher(vertexShader);
		supportsColors = matcher.find();

		/** searches with the aid of regex for texture attribute */
		pattern = Pattern.compile(".*" + TEXTURE_ATTRIBUTE + ".*");
		matcher = pattern.matcher(vertexShader);
		supportsTextures = matcher.find();

		INSTANCES.add(new WeakReference<FeatureShader>(this));
	}

	public int getColorHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		colorHandle = GLES20
				.glGetAttribLocation(programHandle, COLOR_ATTRIBUTE);
		return colorHandle;
	}

	public int getNormalHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		normalHandle = GLES20.glGetAttribLocation(programHandle,
				NORMAL_ATTRIBUTE);
		return normalHandle;
	}

	public int getPositionHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		positionHandle = GLES20.glGetAttribLocation(programHandle,
				POSITION_ATTRIBUTE);
		return positionHandle;
	}

	public void setColors(final int colorBufferHandle) {
		if (colorBufferHandle < 0) {
			LOG.debug("colorbufferhandle is not a valid handle");
			return;
		}
		if (programHandle == -1) {
			initProgram();
		}

		colorHandle = GLES20
				.glGetAttribLocation(programHandle, COLOR_ATTRIBUTE);
		if (colorHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorBufferHandle);
			GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(colorHandle);
		}
	}

	public void setModelViewProjectionMatrix(float[] mvpMatrix) {
		if (programHandle == -1) {
			initProgram();
		}
		// combined Matrix
		mvpMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				MVP_MATRIX_UNIFORM);
		if (mvpMatrixUniform >= 0)
			GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
	}

	public void setNormals(final int normalBufferHandle) {
		if (normalBufferHandle < 0) {
			LOG.debug("normalbufferhandle is not a valid handle");
			return;
		}
		if (programHandle == -1) {
			initProgram();
		}

		normalHandle = GLES20.glGetAttribLocation(programHandle,
				NORMAL_ATTRIBUTE);
		if (normalHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalBufferHandle);
			GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(normalHandle);
		}
	}

	// FIXME view gibts nicht... genauso wie modematrix
	public void setRenderMatrices(float[] mvpMatrix, float[] modelMatrix,
			float[] viewMatrix) {
		if (programHandle == -1) {
			initProgram();
		}
		// combined Matrix
		mvpMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				MVP_MATRIX_UNIFORM);
		if (mvpMatrixUniform >= 0)
			GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		// Model view matrix
		// mvMatrixUniform = GLES20.glGetUniformLocation(programHandle,
		// "u_MVMatrix");
		// if(mvMatrixUniform >= 0)
		// GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, modelMatrix, 0);
		// // View Matrix
		// vMatrixUniform = GLES20.glGetUniformLocation(programHandle,
		// "u_VMatrix");
		// if(vMatrixUniform >= 0)
		// GLES20.glUniformMatrix4fv(vMatrixUniform, 1, false, viewMatrix, 0);
	}

	public void setVertices(final int vertexBufferHandle) {
		if (programHandle == -1) {
			initProgram();
		}
		positionHandle = GLES20.glGetAttribLocation(programHandle,
				POSITION_ATTRIBUTE);
		if (vertexBufferHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferHandle);
			GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(positionHandle);
		} else {
			LOG.debug("vertexbufferhandle is not a valid handle");
		}
	}

	public boolean supportsColors() {
		return supportsColors;
	}

	public boolean supportsNormals() {
		return supportsNormals;
	}

	public boolean supportsTextures() {
		return supportsTextures;
	}

	public void useProgram() {
		if (programHandle == -1) {
			initProgram();
		}
		/** installs the program object as part of the current rendering */
		GLES20.glUseProgram(programHandle);
	}

	private void initProgram() {
		if (vertexShader == null || fragmentShader == null)
			throw new IllegalStateException("Shaders not set");

		if (vertexShaderHandle == -1) {
			vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
		}
		compileShader(vertexShaderHandle, vertexShader);

		if (fragmentShaderHandle == -1) {
			fragmentShaderHandle = GLES20
					.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
		}
		compileShader(fragmentShaderHandle, fragmentShader);

		if (programHandle == -1) {
			programHandle = GLES20.glCreateProgram();
		}
		linkProgram(new String[] { "a_Position", "a_Normal", "a_Color" });

		mvpMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				MVP_MATRIX_UNIFORM);
		positionHandle = GLES20.glGetAttribLocation(programHandle,
				POSITION_ATTRIBUTE);
		if (supportsNormals)
			normalHandle = GLES20.glGetAttribLocation(programHandle,
					NORMAL_ATTRIBUTE);
		if (supportsColors)
			colorHandle = GLES20.glGetAttribLocation(programHandle,
					COLOR_ATTRIBUTE);
	}

	private void linkProgram(final String[] attributes) {
		if (programHandle <= 0) {
			throw new IllegalStateException("Invalid program handle");
		}
		if (vertexShaderHandle <= 0) {
			throw new IllegalStateException("Invalid vertex shader handle");
		}
		if (fragmentShaderHandle <= 0) {
			throw new IllegalStateException("Invalid fragment shader handle");
		}
		if (programHandle != 0) {
			GLES20.glAttachShader(programHandle, vertexShaderHandle);
			GLES20.glAttachShader(programHandle, fragmentShaderHandle);

			GLES20.glBindAttribLocation(programHandle, 0, POSITION_ATTRIBUTE);
			GLES20.glBindAttribLocation(programHandle, 1, NORMAL_ATTRIBUTE);
			// if (attributes != null) {
			// for (int i = 0, size = attributes.length; i < size; i++)
			// GLES20.glBindAttribLocation(programHandle, i+1, attributes[i]);
			// }

			GLES20.glLinkProgram(programHandle);

			final int[] status = new int[1];
			GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, status,
					0);

			if (status[0] == 0) {
				LOG.error("Error compiling program: "
						+ GLES20.glGetProgramInfoLog(programHandle));
				// GLES20.glDeleteProgram(programHandle);
				// programHandle = 0;
			}
		} else {
			throw new RuntimeException("Error creating Program.");
		}
	}

	private void reset() {
		programHandle = positionHandle = colorHandle = normalHandle = mvpMatrixUniform = vertexShaderHandle = fragmentShaderHandle = -1;
	}

}
