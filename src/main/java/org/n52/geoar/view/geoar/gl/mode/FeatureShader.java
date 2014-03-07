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
package org.n52.geoar.view.geoar.gl.mode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class FeatureShader {

	private boolean hasLight;
	private boolean hasColor;
	private boolean hasTexture;

	// TODO unused!
	static final int SIZE_OF_VERTEX_ELEMENT = 3;
	static final int SIZE_OF_NORMAL_ELEMENT = 3;
	static final int SIZE_OF_COLOR_ELEMENT = 4;
	static final int SIZE_OF_TEXTURECOORD_ELEMENT = 2;

	static final int SIZE_OF_FLOAT = 4;
	static final int SIZE_OF_SHORT = 2;

	/** OpenGL handles and uniforms to our program */
	protected static final String ATTRIBUTE_POSITION = "attr_Position";
	protected static final String ATTRIBUTE_NORMAL = "attr_Normal";
	protected static final String ATTRIBUTE_COLOR = "attr_Color";
	protected static final String ATTRIBUTE_TEXTURE = "attr_TexCoordinate";

	protected static final String UNIFORM_MATRIX_MVP = "unif_MVPMatrix";
	protected static final String UNIFORM_MATRIX_MV = "unif_MVMatrix";
	protected static final String UNIFORM_MATRIX_V = "unif_VMatrix";

	protected static final String UNIFORM_VEC3_LIGHTPOS = "unif_LightPos";
	protected static final String UNIFORM_SAMPLER_TEXTURE = "unif_Texture";

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
					"FeatureShader " + GLES20.glGetShaderInfoLog(shaderHandle),
					"Error compiling shader: "
							+ GLES20.glGetShaderInfoLog(shaderHandle));
			GLES20.glDeleteShader(shaderHandle);
		}
	}

	private static float calculateIntegralImageKey(Bitmap bitmap) {
		final int width = bitmap.getWidth();
		final int height = bitmap.getHeight();

		float sum = 0;

		// for (int i = 0, j = 0; i < height && j < width; i++, j++) {
		// float[] hueSatBright = new float[3];
		// int what = bitmap.getPixel(j, i);
		// Color.RGBToHSV((what >> 16) & 0xff, (what >> 8) & 0xff,
		// what & 0xff, hueSatBright);
		// /** we calculate the integral over the brightness */
		// sum += hueSatBright[2] * 255;
		// }

		return 12;

		// final float[][] integral = new float[height][width];

		// for(int i = 0; i < height; i++){
		// float sumOfTheRow = 0;
		// for(int j = 0; j < width; j++){
		// float[] hueSatBright = new float[3];
		// int what = bitmap.getPixel(j, i);
		// Color.RGBToHSV((what >> 16) & 0xff, (what >> 8) & 0xff, what & 0xff,
		// hueSatBright);
		// /** we calculate the integral over the brightness */
		// sumOfTheRow += hueSatBright[2]*255;
		// if(i > 0){
		// integral[i][j] = integral[i-1][j] + sumOfTheRow;
		// } else {
		// integral[i][j] = integral[i][j] + sumOfTheRow;
		// }
		// }
		// }

	}

	private int programHandle = -1;

	private int positionHandle = -1;
	private int colorHandle = -1;
	private int normalHandle = -1;
	private int lightPosHandle = -1;

	private int vertexShaderHandle = -1;
	private int fragmentShaderHandle = -1;

	private int textureCoordinateHandle = -1;
	// private int textureDataHandle = -1;

	private int textureUniformHandle = -1;

	private int mvpMatrixUniform = -1;
	private int mvMatrixUniform = -1;
	private int vMatrixUniform = -1;

	private final float[] lightPosInModelSpace = new float[] { 0.0f, 0.0f,
			0.0f, 1.0f };
	private final float[] lightPosInWorldSpace = new float[4];
	private final float[] lightPosInEyeSpace = new float[4];

	private final String vertexShader, fragmentShader;

	final boolean supportsNormals;
	final boolean supportsColors;
	final boolean supportsTextures;
	final boolean supportsLight;

	public FeatureShader(String vertexShader, String fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;

		/** searches with the aid of regex for the normal attribute */
		Pattern pattern = Pattern.compile(".*" + ATTRIBUTE_NORMAL + ".*");
		Matcher matcher = pattern.matcher(vertexShader);
		supportsNormals = matcher.find();

		/** Searches with the aid of regex for color attribute */
		pattern = Pattern.compile(".*" + ATTRIBUTE_COLOR + ".*");
		matcher = pattern.matcher(vertexShader);
		supportsColors = matcher.find();

		/** searches with the aid of regex for texture attribute */
		pattern = Pattern.compile(".*" + ATTRIBUTE_TEXTURE + ".*");
		matcher = pattern.matcher(vertexShader);
		supportsTextures = matcher.find();

		pattern = Pattern.compile(".*" + UNIFORM_VEC3_LIGHTPOS + ".*");
		matcher = pattern.matcher(vertexShader);
		supportsLight = matcher.find();

		INSTANCES.add(new WeakReference<FeatureShader>(this));
	}

	public int getColorHandle() {
		if (programHandle == -1) {
			initProgram();
		}

		return colorHandle;
	}

	public int getNormalHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		return normalHandle;
	}

	public int getPositionHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		return positionHandle;
	}

	public int getLightPosHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		return lightPosHandle;
	}

	public int getTextureCoordinateHandle() {
		if (programHandle == -1) {
			initProgram();
		}
		return textureCoordinateHandle;
	}

	public int getTextureUniform() {
		if (programHandle == -1) {
			initProgram();
		}

		return textureUniformHandle;
	}

	/******************************************************************
	 * Methods for setting required object Data
	 ******************************************************************/
	public void setVertices(final int vertexBufferHandle) {
		if (programHandle == -1) {
			initProgram();
		}

		if (vertexBufferHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferHandle);
			GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(positionHandle);
		} else {
			LOG.debug("vertexbufferhandle is not a valid handle");
		}
	}

	public void setTextureCoordinates(final int textureBufferHandle) {
		if (programHandle == -1) {
			initProgram();
		}

		if (textureCoordinateHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBufferHandle);
			GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
			GLES20.glVertexAttribPointer(textureCoordinateHandle, 2,
					GLES20.GL_FLOAT, false, 0, 0);
		}
	}

	public void setColors(final int colorBufferHandle) {
		if (colorBufferHandle < 0) {
			LOG.debug("colorbufferhandle is not a valid handle");
			return;
		}
		if (programHandle == -1) {
			initProgram();
		}

		if (colorHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorBufferHandle);
			GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(colorHandle);
		}
	}

	public void setNormals(final int normalBufferHandle) {
		if (normalBufferHandle < 0) {
			LOG.debug("normalbufferhandle is not a valid handle");
			return;
		}
		if (programHandle == -1) {
			initProgram();
		}

		if (normalHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalBufferHandle);
			GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
			GLES20.glEnableVertexAttribArray(normalHandle);
		}
	}

	public void setModelViewProjectionMatrix(float[] mvpMatrix) {
		if (programHandle == -1) {
			initProgram();
		}
		// combined Matrix
		mvpMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				UNIFORM_MATRIX_MVP);
		if (mvpMatrixUniform >= 0)
			GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
	}

	public void setModelViewMatrix(float[] mvMatrix) {
		if (programHandle == -1) {
			initProgram();
		}

		mvMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				UNIFORM_MATRIX_MV);
		if (mvMatrixUniform >= 0) {
			GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvMatrix, 0);
		}
	}

	public void setLightPositionVec(float[] lightPosition) {
		if (programHandle == -1) {
			initProgram();
		}

		lightPosHandle = GLES20.glGetUniformLocation(programHandle,
				UNIFORM_VEC3_LIGHTPOS);
		if (lightPosHandle >= 0) {
			GLES20.glUniform3f(lightPosHandle, lightPosition[0],
					lightPosition[1], lightPosition[2]);
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
				UNIFORM_MATRIX_MVP);
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
		linkProgram(new String[] { "aPosition", "aNormal", "aColor" });

		mvpMatrixUniform = GLES20.glGetUniformLocation(programHandle,
				UNIFORM_MATRIX_MVP);
		positionHandle = GLES20.glGetAttribLocation(programHandle,
				ATTRIBUTE_POSITION);
		if (supportsNormals)
			normalHandle = GLES20.glGetAttribLocation(programHandle,
					ATTRIBUTE_NORMAL);
		if (supportsColors)
			colorHandle = GLES20.glGetAttribLocation(programHandle,
					ATTRIBUTE_COLOR);
		if (supportsLight)
			lightPosHandle = GLES20.glGetUniformLocation(programHandle,
					UNIFORM_VEC3_LIGHTPOS);
		if (supportsTextures) {
			textureCoordinateHandle = GLES20.glGetAttribLocation(programHandle,
					ATTRIBUTE_TEXTURE);
			textureUniformHandle = GLES20.glGetUniformLocation(programHandle,
					UNIFORM_SAMPLER_TEXTURE);
		}
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

			GLES20.glBindAttribLocation(programHandle, 0, ATTRIBUTE_POSITION);
			// GLES20.glBindAttribLocation(programHandle, 1, ATTRIBUTE_COLOR);
			GLES20.glBindAttribLocation(programHandle, 1, ATTRIBUTE_NORMAL);
			// GLES20.glBindAttribLocation(programHandle, 3, ATTRIBUTE_TEXTURE);
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
