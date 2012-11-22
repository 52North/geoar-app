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
package org.n52.android.view.geoar.gl.model.shader;

import java.nio.Buffer;

import org.n52.android.view.geoar.GLESUtils;

import android.annotation.SuppressLint;
import android.opengl.GLES20;
import android.util.Log;

/**
 * 
 * @author Arne de Wall
 * 
 */
public abstract class Renderer {

	/************************
	 * Variables
	 ************************/
	int vertexHandle;
	int fragmentHandle;
	int programHandle;

	int positionHandle;
	int colorHandle;
	int normalHandle;

	int mvpMatrixHandle;
	int modelMatrixHandle;
	int viewMatrixHandle;

	float[] modelViewlMatrix;
	float[] viewMatrix;

	String vertexShader;
	String fragmentShader;


	public Renderer(String vertexShader, String fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;

	}  

	public void initShaders() {
		if (vertexShader == null || fragmentShader == null)
			return;

		programHandle = GLESUtils.createProgram(vertexShader, fragmentShader);
		if (programHandle == 0)
			throw new RuntimeException("Could not compile the program");


		positionHandle = GLES20
				.glGetAttribLocation(programHandle, "a_Position");
		if (positionHandle == -1)
			throw new RuntimeException(
					"get attrib location aVertexPosition failed");

		colorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");
		if (colorHandle == -1)
			throw new RuntimeException(
					"get attrib location fVertexColor failed"); 
		
		normalHandle = GLES20.glGetAttribLocation(programHandle, "a_Normal");

		mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle,
				"u_MVPMatrix");
		if (mvpMatrixHandle == -1)
			throw new RuntimeException("get attrib location uMVPMatrix failed");

		modelMatrixHandle = GLES20.glGetUniformLocation(programHandle,
				"u_MMatrix");
		viewMatrixHandle = GLES20.glGetUniformLocation(programHandle,
				"u_VMatrix");

		int x = 2;
	}

	public void onDestroy() {
		modelViewlMatrix = null;
		viewMatrix = null;
		GLES20.glDeleteShader(fragmentHandle);
		GLES20.glDeleteShader(vertexHandle);
		GLES20.glDeleteProgram(programHandle);
	}

	public void setVertices(final int vertexBufferHandle) {
		if (vertexBufferHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferHandle);
			GLES20.glEnableVertexAttribArray(positionHandle);
			GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
		} else {
			Log.d(this.getClass().getSimpleName(),
					"vertexbufferhandle is not a valid handle");
		}
	}

	public void setNormals(final int normalBufferHandle) {
		if (normalBufferHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, normalBufferHandle);
			GLES20.glEnableVertexAttribArray(normalHandle);
			GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
					false, 0, 0);
		} else {
			Log.d(this.getClass().getSimpleName(),
					"normalbufferhandle is not a valid handle");
		}
	}

	public void setColors(final int colorBufferHandle, Buffer buf) {
		if (colorBufferHandle >= 0) {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, colorBufferHandle);
			GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT,
					false, 0, buf);
			GLES20.glEnableVertexAttribArray(colorHandle);
		} else {
			Log.d(this.getClass().getSimpleName(),
					"colorbufferhandle is not a valid handle");
		}
	}

	public void setRenderMatrices(float[] mvpMatrix, float[] modelMatrix,
			float[] viewMatrix) {
		setMVPMatrix(mvpMatrix);
		setModelMatrix(modelMatrix);
		setViewMatrix(viewMatrix); 
	}

	public void setMVPMatrix(float[] mvpMatrix) {

		GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
	}

	public void setModelMatrix(float[] modelMatrix) {
		this.modelViewlMatrix = modelMatrix;
		GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
	}

	public void setViewMatrix(float[] viewMatrix) {
		this.viewMatrix = viewMatrix;
		GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0);
	}

	public void useProgram() {
		GLES20.glUseProgram(programHandle);
	}
}
