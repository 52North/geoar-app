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
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.n52.android.newdata.gl.primitives.DataSourceRenderable;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OnInitializeInGLThread;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OpenGLCallable;
import org.n52.android.view.geoar.gl.model.Spatial;

import android.opengl.GLES20;
import android.opengl.Matrix;

public abstract class RenderFeature2 extends Spatial implements
		DataSourceRenderable, OpenGLCallable, OnInitializeInGLThread {

	/** Size of the position data in elements. */
	static final int POSITION_DATA_SIZE = 3;
	/** Size of the normal data in elements. */
	static final int NORMAL_DATA_SIZE = 3;

	static final int COLOR_DATA_SIZE = 4;

	static final int TEXTURE_DATA_SIZE = 2;
	/** How many bytes per float. */
	static final int BYTES_PER_FLOAT = 4;

	static final int BYTES_PER_INT = 4;

	static final int BYTES_PER_SHORT = 2;

	/**
	 * 
	 * @author Arne de Wall
	 * 
	 */
	private abstract class FeatureGeometry {

		protected int verticesCount;
		protected boolean hasNormals;
		protected boolean hasColors;
		protected boolean hasTextureCoords;

		abstract void onRenderGeometrie();
	}

	/**
	 * 
	 * @author Arne de Wall
	 * 
	 */
	@Deprecated
	private final class FeatureGeometryVBOandIBO extends FeatureGeometry {

		protected class BufferDetails {
			public final Buffer buffer;
			public final short bufferType;
			public final int bufferHandle;
			public final int byteSize;
			public final int target;

			public BufferDetails(final float[] data, final int bufferHandle,
					final int target) {
				final FloatBuffer floatBuffer = ByteBuffer
						.allocateDirect(data.length * BYTES_PER_FLOAT)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				floatBuffer.put(data).compact().position(0);
				this.buffer = floatBuffer;
				this.bufferType = FLOAT_BUFFER;
				this.byteSize = BYTES_PER_FLOAT;
				this.target = target;
				this.bufferHandle = bufferHandle;
			}

			@SuppressWarnings("unused")
			public BufferDetails(final int[] data, final int bufferHandle,
					final int target) {
				final IntBuffer integerBuffer = ByteBuffer
						.allocateDirect(data.length * BYTES_PER_INT)
						.order(ByteOrder.nativeOrder()).asIntBuffer();
				integerBuffer.put(data).compact().position(0);
				this.buffer = integerBuffer;
				this.bufferType = INT_BUFFER;
				this.byteSize = BYTES_PER_INT;
				this.target = target;
				this.bufferHandle = bufferHandle;
			}

			public BufferDetails(final short[] data, final int bufferHandle,
					final int target) {
				final ShortBuffer shortBuffer = ByteBuffer
						.allocateDirect(data.length * BYTES_PER_SHORT)
						.order(ByteOrder.nativeOrder()).asShortBuffer();
				shortBuffer.put(data).compact().position(0);
				this.buffer = shortBuffer;
				this.bufferType = SHORT_BUFFER;
				this.byteSize = BYTES_PER_SHORT;
				this.target = target;
				this.bufferHandle = bufferHandle;
			}

			void bindBuffer() {
				GLES20.glBindBuffer(target, bufferHandle);
				GLES20.glBufferData(target, buffer.capacity() * byteSize,
						buffer, GLES20.GL_STATIC_DRAW);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			}

		}

		static final short FLOAT_BUFFER = 100;
		static final short INT_BUFFER = 101;
		static final short SHORT_BUFFER = 102;

		protected BufferDetails verticesDetails;
		protected BufferDetails colorsDetails;
		protected BufferDetails normalsDetails;
		protected BufferDetails textureDetails;
		protected BufferDetails indicesDetails;

		protected FeatureGeometryVBOandIBO(final float[] vertices,
				final float[] colors, final float[] normals,
				final float[] textureCoords, final short[] indices) {
			if (vertices.length % 3 != 0)
				throw new IllegalArgumentException(
						"[RENDERFEATURE] vertices-array size must be a multiple of three");

			this.hasColors = (colors != null && renderer.supportsColors);
			this.hasNormals = (normals != null && renderer.supportsNormals);
			this.hasTextureCoords = (textureCoords != null && renderer.supportsTextures);

			this.verticesCount = vertices.length / POSITION_DATA_SIZE;
			initBuffers(vertices, colors, normals, textureCoords, indices);
		}

		private void initBuffers(final float[] vertices, final float[] colors,
				final float[] normals, final float[] textureCoords,
				final short[] indices) {

			final int bufferCount = 1 + (hasColors ? 1 : 0)
					+ (hasNormals ? 1 : 0) + (hasTextureCoords ? 1 : 0) + 1;

			/** generate buffers on OpenGL */
			final int bufferObjects[] = new int[bufferCount];
			GLES20.glGenBuffers(bufferCount, bufferObjects, 0);
			for (int i = 0; i < bufferCount; i++) {
				if (bufferObjects[i] < 1)
					try {
						throw new Exception(
								"[RENDERFEATURE] initBuffers() -> (Buffer < 1) No Buffer created! ");
					} catch (Exception e) {
						e.printStackTrace();
					}
			}

			int nextBuffer = 0;

			/** generate and bind vertices FloatBuffer */
			verticesDetails = new BufferDetails(vertices,
					bufferObjects[nextBuffer++], GLES20.GL_ARRAY_BUFFER);
			verticesDetails.bindBuffer();

			if (hasColors) {
				/** generate and bind colors FloatBuffer */
				colorsDetails = new BufferDetails(colors,
						bufferObjects[nextBuffer++], GLES20.GL_ARRAY_BUFFER);
				colorsDetails.bindBuffer();
			}

			if (hasNormals) {
				/** generate and bind normals FloatBuffer */
				normalsDetails = new BufferDetails(normals,
						bufferObjects[nextBuffer++], GLES20.GL_ARRAY_BUFFER);
				normalsDetails.bindBuffer();
			}

			if (hasTextureCoords) {
				/** generate and bind texture coordinates FloatBuffer */
				textureDetails = new BufferDetails(textureCoords,
						bufferObjects[nextBuffer++], GLES20.GL_ARRAY_BUFFER);
				textureDetails.bindBuffer();
			}

			indicesDetails = new BufferDetails(indices,
					bufferObjects[nextBuffer++], GLES20.GL_ARRAY_BUFFER);
			indicesDetails.bindBuffer();
		}

		@Override
		void onRenderGeometrie() {
			/** bind the named vertices buffer object */
			renderer.setVertices(verticesDetails.bufferHandle);
			if (hasColors) {
				/** bind the named color buffer object */
				renderer.setColors(colorsDetails.bufferHandle);
			}
			if (hasNormals) {
				/** bind the named normal buffer object */
				renderer.setNormals(normalsDetails.bufferHandle);
			}
			if (hasTextureCoords) {
				// FIXME NOT IMPLEMENTED YET !
				// renderer.setTextureCoords(textureDetails.bufferHandle);
			}

			/**
			 * bind the named indices buffer object and draw the elements in
			 * respect to the GLES20 drawingMode
			 */
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
					indicesDetails.bufferHandle);
			GLES20.glDrawElements(drawingMode,
					indicesDetails.buffer.capacity(), GLES20.GL_UNSIGNED_SHORT,
					0);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}

	}

	private final class FeatureGeometryStride extends FeatureGeometry {

		private final FloatBuffer strideBuffer;

		protected FeatureGeometryStride(final float[] vertices,
				final float[] colors, final float[] normals,
				final float[] textureCoords) {
			if (vertices.length % 3 != 0)
				throw new IllegalArgumentException(
						"[RENDERFEATURE] vertices-array size must be a multiple of three");

			this.hasColors = (colors != null && renderer.supportsColors);
			this.hasNormals = (normals != null && renderer.supportsNormals);
			this.hasTextureCoords = (textureCoords != null && renderer.supportsTextures);

			this.verticesCount = vertices.length / POSITION_DATA_SIZE;
			this.strideBuffer = initInterleavedBuffer(vertices, colors,
					normals, textureCoords);
		}

		private FloatBuffer initInterleavedBuffer(final float[] vertices,
				final float[] colors, final float[] normals,
				final float[] textureCoords) {

			final int bufferLength = vertices.length
					+ (hasColors ? colors.length : 0)
					+ (hasNormals ? normals.length : 0)
					+ (hasTextureCoords ? textureCoords.length : 0);

			int verticesOffset = 0;
			int normalsOffset = 0;
			int colorsOffset = 0;
			int texturesOffset = 0;

			final FloatBuffer interleavedBuffer = ByteBuffer
					.allocateDirect(bufferLength * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();

			for (int i = 0; i < verticesCount; i++) {
				interleavedBuffer.put(vertices, verticesOffset,
						POSITION_DATA_SIZE);
				verticesOffset += POSITION_DATA_SIZE;
				if (hasColors) {
					interleavedBuffer
							.put(colors, colorsOffset, COLOR_DATA_SIZE);
					colorsOffset += COLOR_DATA_SIZE;
				}
				if (hasNormals) {
					interleavedBuffer.put(normals, normalsOffset,
							NORMAL_DATA_SIZE);
					normalsOffset += NORMAL_DATA_SIZE;
				}
				if (hasTextureCoords) {
					interleavedBuffer.put(textureCoords, texturesOffset,
							TEXTURE_DATA_SIZE);
					texturesOffset += TEXTURE_DATA_SIZE;
				}
			}
			interleavedBuffer.position(0);
			return interleavedBuffer;
		}

		@Override
		void onRenderGeometrie() {
			final int stride = (POSITION_DATA_SIZE + COLOR_DATA_SIZE)
					* BYTES_PER_FLOAT;

			int bufferPosition = 0;
			/** defines the array of generic vertex attribute data */
			strideBuffer.position(bufferPosition);
			// FIXME caching of handles?!
			final int positionhandle = renderer.getPositionHandle();
			GLES20.glEnableVertexAttribArray(positionhandle);
			GLES20.glVertexAttribPointer(positionhandle, POSITION_DATA_SIZE,
					GLES20.GL_FLOAT, false, stride, strideBuffer);
			bufferPosition += POSITION_DATA_SIZE;

			if (hasColors) {
				/** defines the array of color attribute data */
				strideBuffer.position(bufferPosition);
				final int colorhandle = renderer.getColorHandle();
				GLES20.glEnableVertexAttribArray(colorhandle);
				GLES20.glVertexAttribPointer(colorhandle, COLOR_DATA_SIZE,
						GLES20.GL_FLOAT, false, stride, strideBuffer);
				bufferPosition += COLOR_DATA_SIZE;
			}

			if (hasNormals) {
				/** defines the array of vertices normals attribute data */
				strideBuffer.position(bufferPosition);
				final int normalhandle = renderer.getNormalHandle();
				GLES20.glEnableVertexAttribArray(normalhandle);
				GLES20.glVertexAttribPointer(normalhandle, NORMAL_DATA_SIZE,
						GLES20.GL_FLOAT, false, stride, strideBuffer);
				bufferPosition += NORMAL_DATA_SIZE;
			}

			if (hasTextureCoords) {
				// FIXME texture handling here
				// strideBuffer.position(bufferPosition);
				// final int textureHandle = renderer.getTextureHandle();
				// GLES20.glEnableVertexAttribArray(textureHandle);
				// GLES20.glVertexAttribPointer(textureHandle,
				// TEXTURE_DATA_SIZE,
				// GLES20.GL_FLOAT, false, stride, strideBuffer);
				// bufferPosition += TEXTURE_DATA_SIZE;
			}

			/** render primitives from array data */
			GLES20.glDrawArrays(drawingMode, 0, verticesCount);
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
	


	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, float[] textureCoords, short[] indices) {
		if (indices == null || indices.length == 0) {
			setRenderObjectives(vertices, colors, normals, textureCoords);
		} else {
			if(renderer == null){
				renderer = new ColoredFeatureShader();
			}
			renderer.onCreateInGLESThread();
			geometry = new FeatureGeometryVBOandIBO(vertices, colors, normals,
					textureCoords, indices);
		}
	}

	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, float[] textureCoords) {
		if(renderer == null){
			renderer = new ColoredFeatureShader();
		}
		renderer.onCreateInGLESThread();
		geometry = new FeatureGeometryStride(vertices, colors, normals,
				textureCoords);
	}

	@Override
	public void onRender(float[] projectionMatrix, float[] viewMatrix,
			float[] parentMatrix) {

		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(mvpMatrix, 0);
		Matrix.setIdentityM(tmpMatrix, 0);
		Matrix.translateM(modelMatrix, 0, position[0], position[1] - 1.6f,
				position[2]);

		if (parentMatrix != null) {
			Matrix.multiplyMM(tmpMatrix, 0, parentMatrix, 0, modelMatrix, 0);
			System.arraycopy(tmpMatrix, 0, modelMatrix, 0, 16);
			Matrix.setIdentityM(tmpMatrix, 0);
		}

		Matrix.multiplyMM(tmpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, tmpMatrix, 0);

		/** sets the program object as part of current rendering state */
		renderer.useProgram();
		renderer.setModelViewProjectionMatrix(mvpMatrix);

		geometry.onRenderGeometrie();
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
}
