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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.gl.primitives.DataSourceRenderable;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OpenGLCallable;
import org.n52.android.view.geoar.gl.model.Spatial;

import android.opengl.GLES20;
import android.opengl.Matrix;

public abstract class RenderFeature extends Spatial implements
		DataSourceRenderable, OpenGLCallable {

	private enum TypeOfBuffer {
		FLOAT_BUFFER, INT_BUFFER, SHORT_BUFFER
	}

	private class FeatureGeometry {

		/** Size of the position data in elements. */
		static final int POSITION_DATA_SIZE = 3;
		/** Size of the normal data in elements. */
		static final int NORMAL_DATA_SIZE = 3;
		/** How many bytes per float. */
		static final int BYTES_PER_FLOAT = 4;

		private static final int BYTES_PER_SHORT = 2;

		static final int COLOR_DATA_SIZE = 4;

		public static final short FLOAT_BUFFER = 100;
		public static final short INT_BUFFER = 101;
		public static final short SHORT_BUFFER = 102;

		protected BufferDetails verticesDetails;
		protected BufferDetails colorsDetails;
		protected BufferDetails normalsDetails;
		protected BufferDetails indicesDetails;

		protected class BufferDetails {
			public Buffer buffer;
			public short bufferType;
			public int bufferHandle;
			public int byteSize;
			public int target;

			public BufferDetails(Buffer buffer, short bufferType, int target,
					int bufferHandle, int byteSize) {
				this.buffer = buffer;
				this.bufferType = bufferType;
				this.bufferHandle = bufferHandle;
				this.target = target;
				this.byteSize = byteSize;
			}
		}

		public void setRenderObjectives(float[] vertices, float[] colors,
				float[] normals, short[] indices) {

			final int bufferObjects[] = new int[4];
			GLES20.glGenBuffers(4, bufferObjects, 0);
			if (bufferObjects[0] < 1 || bufferObjects[1] < 1
					|| bufferObjects[2] < 1 || bufferObjects[3] < 1)
				try {
					throw new Exception("Buffer < 1");
				} catch (Exception e) {
					e.printStackTrace();
				}

			final FloatBuffer verticesBuffer = ByteBuffer
					.allocateDirect(vertices.length * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			verticesBuffer.put(vertices).compact().position(0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjects[0]);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
					verticesBuffer.capacity() * BYTES_PER_FLOAT,
					verticesBuffer, GLES20.GL_STATIC_DRAW);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			verticesDetails = new BufferDetails(verticesBuffer, FLOAT_BUFFER,
					GLES20.GL_ARRAY_BUFFER, bufferObjects[0], BYTES_PER_FLOAT);

			final FloatBuffer colorsBuffer = ByteBuffer
					.allocateDirect(colors.length * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			colorsBuffer.put(colors).position(0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjects[1]);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
					verticesBuffer.capacity() * BYTES_PER_FLOAT,
					verticesBuffer, GLES20.GL_STATIC_DRAW);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			colorsDetails = new BufferDetails(colorsBuffer, FLOAT_BUFFER,
					GLES20.GL_ARRAY_BUFFER, bufferObjects[1], BYTES_PER_FLOAT);

			final FloatBuffer normalsBuffer = ByteBuffer
					.allocateDirect(normals.length * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			normalsBuffer.put(normals).position(0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferObjects[2]);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
					normalsBuffer.capacity() * BYTES_PER_FLOAT, normalsBuffer,
					GLES20.GL_STATIC_DRAW);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			normalsDetails = new BufferDetails(normalsBuffer, FLOAT_BUFFER,
					GLES20.GL_ARRAY_BUFFER, bufferObjects[2], BYTES_PER_FLOAT);

			final ShortBuffer indicesBuffer = ByteBuffer
					.allocateDirect(indices.length * BYTES_PER_SHORT)
					.order(ByteOrder.nativeOrder()).asShortBuffer();
			indicesBuffer.put(indices).position(0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
					bufferObjects[3]);
			GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
					indicesBuffer.capacity() * BYTES_PER_SHORT, indicesBuffer,
					GLES20.GL_STATIC_DRAW);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
			indicesDetails = new BufferDetails(indicesBuffer, SHORT_BUFFER,
					GLES20.GL_ELEMENT_ARRAY_BUFFER, bufferObjects[3],
					BYTES_PER_SHORT);

		}
	}

	protected FeatureGeometry geometry;
	protected FeatureShader renderer;

	/** OpenGL handles to our program attributes */

	private final float[] modelMatrix = new float[16];
	private final float[] mvpMatrix = new float[16];
	private final float[] tmpMatrix = new float[16];

	protected int drawingMode = GLES20.GL_TRIANGLES;

	protected boolean enableBlending = true;
	protected boolean enableDepthTest = true;
	protected boolean enableDepthMask = true;
	protected boolean enableCullFace = false;

	protected Float alpha;
	protected int androidColor;
	
	protected SpatialEntity entity;

	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, short[] indices) {
		geometry = new FeatureGeometry();
		geometry.setRenderObjectives(vertices, colors, normals, indices);
	}

	@Override
	public void onRender(float[] projectionMatrix, float[] viewMatrix,
			float[] parentMatrix) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(mvpMatrix, 0);
		Matrix.setIdentityM(tmpMatrix, 0);

		Matrix.translateM(modelMatrix, 0, position[0], position[1] - 1.6f,
				position[2]);

		if (parentMatrix != null) {
			Matrix.multiplyMM(tmpMatrix, 0, parentMatrix, 0, modelMatrix, 0);
			System.arraycopy(tmpMatrix, 0, modelMatrix, 0, 16);
		}

		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

		renderer.useProgram();
		renderer.setVertices(geometry.verticesDetails.bufferHandle);
		renderer.setNormals(geometry.normalsDetails.bufferHandle);
		renderer.setColors(geometry.colorsDetails.bufferHandle);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		renderer.setModelViewProjectionMatrix(mvpMatrix);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				geometry.indicesDetails.bufferHandle);
		GLES20.glDrawElements(drawingMode,
				geometry.indicesDetails.buffer.capacity(),
				GLES20.GL_UNSIGNED_SHORT, 0);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	@Override
	public void enableCullface(boolean cullface) {
		this.enableCullFace = cullface;
	}

	@Override
	public void enableBlending(boolean blending, float alpha) {
		this.enableBlending = blending;
		this.alpha = alpha;
	}

	@Override
	public void enableDepthtest(boolean depthTest) {
		this.enableDepthTest = depthTest;
	}

	@Override
	public void setDrawingMode(int drawingMode) {
		this.drawingMode = drawingMode;
	}

	@Override
	public void setColor(int androidColor) {
		this.androidColor = androidColor;
	}

	@Override
	public void setColor(float[] colorArray) {
		throw new UnsupportedOperationException();
	}

	public SpatialEntity getEntity() {
		return entity;
	}

	public void setEntity(SpatialEntity entity) {
		this.entity = entity;
	}
}
