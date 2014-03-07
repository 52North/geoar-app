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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Stack;
import java.util.concurrent.Callable;

import javax.microedition.khronos.opengles.GL10;

import org.n52.geoar.newdata.vis.DataSourceVisualization.DataSourceVisualizationGL;
import org.n52.geoar.ar.view.gl.GLESCamera;
import org.n52.geoar.ar.view.gl.ARSurfaceViewRenderer.OnInitializeInGLThread;
import org.n52.geoar.ar.view.gl.ARSurfaceViewRenderer.OpenGLCallable;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;

/**
 * 
 * @author Arne de Wall
 *
 */
public abstract class RenderFeature2 extends Spatial implements
		DataSourceVisualizationGL, OpenGLCallable, OnInitializeInGLThread {


	/** Static constants */
	protected static final int SIZE_OF_POSITION = 3;
	protected static final int SIZE_OF_NORMAL = 3;
	protected static final int SIZE_OF_COLOR = 4;
	protected static final int SIZE_OF_TEXCOORD = 2;
	
	protected static final int SIZE_OF_FLOAT = 4;
	protected static final int SIZE_OF_INT = 4;
	protected static final int SIZE_OF_SHORT = 2;

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

		abstract void render();
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
						.allocateDirect(data.length * SIZE_OF_FLOAT)
						.order(ByteOrder.nativeOrder()).asFloatBuffer();
				floatBuffer.put(data).compact().position(0);
				this.buffer = floatBuffer;
				this.bufferType = FLOAT_BUFFER;
				this.byteSize = SIZE_OF_FLOAT;
				this.target = target;
				this.bufferHandle = bufferHandle;
			}

			@SuppressWarnings("unused")
			public BufferDetails(final int[] data, final int bufferHandle,
					final int target) {
				final IntBuffer integerBuffer = ByteBuffer
						.allocateDirect(data.length * SIZE_OF_INT)
						.order(ByteOrder.nativeOrder()).asIntBuffer();
				integerBuffer.put(data).compact().position(0);
				this.buffer = integerBuffer;
				this.bufferType = INT_BUFFER;
				this.byteSize = SIZE_OF_INT;
				this.target = target;
				this.bufferHandle = bufferHandle;
			}

			public BufferDetails(final short[] data, final int bufferHandle,
					final int target) {
				final ShortBuffer shortBuffer = ByteBuffer
						.allocateDirect(data.length * SIZE_OF_SHORT)
						.order(ByteOrder.nativeOrder()).asShortBuffer();
				shortBuffer.put(data).compact().position(0);
				this.buffer = shortBuffer;
				this.bufferType = SHORT_BUFFER;
				this.byteSize = SIZE_OF_SHORT;
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

			this.verticesCount = vertices.length / SIZE_OF_POSITION;
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
		void render() {
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

			this.verticesCount = vertices.length / SIZE_OF_POSITION;
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
					.allocateDirect(bufferLength * SIZE_OF_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();

			for (int i = 0; i < verticesCount; i++) {
				interleavedBuffer.put(vertices, verticesOffset,
						SIZE_OF_POSITION);
				verticesOffset += SIZE_OF_POSITION;
				if (hasColors) {
					interleavedBuffer
							.put(colors, colorsOffset, SIZE_OF_COLOR);
					colorsOffset += SIZE_OF_COLOR;
				}
				if (hasNormals) {
					interleavedBuffer.put(normals, normalsOffset,
							SIZE_OF_NORMAL);
					normalsOffset += SIZE_OF_NORMAL;
				}
				if (hasTextureCoords) {
					interleavedBuffer.put(textureCoords, texturesOffset,
							SIZE_OF_TEXCOORD);
					texturesOffset += SIZE_OF_TEXCOORD;
				}
			}
			interleavedBuffer.position(0);
			return interleavedBuffer;
		}

		@Override
		void render() {
			// @formatter:off
			final int stride = (SIZE_OF_POSITION
					+ (hasColors ? SIZE_OF_COLOR : 0)
					+ (hasNormals ? SIZE_OF_NORMAL : 0) 
					+ (hasTextureCoords ? SIZE_OF_TEXCOORD : 0))
					* SIZE_OF_FLOAT;
			// @formatter:on

			int bufferPosition = 0;
			/** defines the array of generic vertex attribute data */
			strideBuffer.position(bufferPosition);
			final int positionhandle = renderer.getPositionHandle();
			GLES20.glEnableVertexAttribArray(positionhandle);
			GLES20.glVertexAttribPointer(positionhandle, SIZE_OF_POSITION,
					GLES20.GL_FLOAT, false, stride, strideBuffer);
			bufferPosition += SIZE_OF_POSITION;

			if (hasColors) {
				/** defines the array of color attribute data */
				strideBuffer.position(bufferPosition);
				final int colorhandle = renderer.getColorHandle();
				GLES20.glEnableVertexAttribArray(colorhandle);
				GLES20.glVertexAttribPointer(colorhandle, SIZE_OF_COLOR,
						GLES20.GL_FLOAT, false, stride, strideBuffer);
				bufferPosition += SIZE_OF_COLOR;
			}

			if (hasNormals) {
				/** defines the array of vertices normals */
				strideBuffer.position(bufferPosition);
				final int normalhandle = renderer.getNormalHandle();
				GLES20.glEnableVertexAttribArray(normalhandle);
				GLES20.glVertexAttribPointer(normalhandle, SIZE_OF_NORMAL,
						GLES20.GL_FLOAT, false, stride, strideBuffer);
				bufferPosition += SIZE_OF_NORMAL;
			}

			if (hasTextureCoords) {
				// FIXME texture handling here
				strideBuffer.position(bufferPosition);
				final int textureCoordinateHandle = renderer
						.getTextureCoordinateHandle();
				GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
				GLES20.glVertexAttribPointer(textureCoordinateHandle,
						SIZE_OF_TEXCOORD, GLES20.GL_FLOAT, false, stride,
						strideBuffer);
				bufferPosition += SIZE_OF_TEXCOORD;
			}

			/** render primitives from array data */
			GLES20.glDrawArrays(drawingMode, 0, verticesCount);
		}
	}

	/** Feature geometries and shader settings */
	protected FeatureGeometry geometry;
	protected FeatureShader renderer;
	protected BoundingBox boundingBox; // unused

	protected final Stack<OpenGLCallable> childrenFeatures = new Stack<OpenGLCallable>();

	/** Model Matrix of this feature */
	private final float[] modelMatrix = new float[16];
	/** Model-View-Projection Matrix of our feature */
	private final float[] mvpMatrix = new float[16];
	/** temporary Matrix for caching */
	private final float[] tmpMatrix = new float[16];

	/** GL drawing mode - default triangles */
	protected int drawingMode = GLES20.GL_TRIANGLES;
	/** GL for features rendering */
	protected boolean enableBlending = true;
	protected boolean enableDepthTest = true;
	protected boolean enableDepthMask = true;
	protected boolean enableCullFace = false;
	
	protected float heightOffset = 0.0f;
	/** alpha value for Blending */
	protected Float alpha; 
	/** color of the object */
	protected int androidColor; 

	protected boolean isInitialized = false;
	private Texture texture;

	@Deprecated
	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, float[] textureCoords, short[] indices) {
		if (indices == null || indices.length == 0) {
			setRenderObjectives(vertices, colors, normals, textureCoords);
		} else {

			if (renderer == null) {
				renderer = BilligerColorShader.getInstance();
			}
			// renderer.onCreateInGLESThread();
			geometry = new FeatureGeometryVBOandIBO(vertices, colors, normals,
					textureCoords, indices);
			boundingBox = new BoundingBox(vertices);
		}
	}

	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, float[] textureCoords) {
		// TODO XXX FIXME not elegant here, maybe this is
		// Jep
		if (renderer == null) {
			if (textureCoords != null && texture != null) {
				renderer = TextureFeatureShader.getInstance();
			} else {
				renderer = BilligerLightShader.getInstance();
			}
		}

		// renderer.onCreateInGLESThread();
		geometry = new FeatureGeometryStride(vertices, colors, normals,
				textureCoords);
		boundingBox = new BoundingBox(vertices);

		isInitialized = true;
	}

	public float[] onScreenCoordsUpdate() {
		if (modelMatrix == null || GLESCamera.projectionMatrix == null
				|| GLESCamera.viewportMatrix == null) {
			return null;
		}
		float[] output = new float[3];
		int res = GLU.gluProject(position[0], position[1], position[2],
				modelMatrix, 0, GLESCamera.projectionMatrix, 0,
				GLESCamera.viewportMatrix, 0, output, 0);

		if (res == GL10.GL_FALSE)
			return null;
		return output;
	}

	public void transform() {
		// TODO
		// gl.glTranslatef(tx, ty, tz);
		// gl.glRotatef(rz, 0, 0, 1);
		// gl.glRotatef(ry, 0, 1, 0);
		// gl.glRotatef(rx, 1, 0, 0);
		// gl.glScalef(sx, sy, sz);
	}

	@Override
	public void render(final float[] projectionMatrix,
			final float[] viewMatrix, final float[] parentMatrix,
			final float[] lightPosition) {
		if (!isInitialized)
			return;
		
		/** set the matrices to identity matrix */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(mvpMatrix, 0);
		Matrix.setIdentityM(tmpMatrix, 0);
		/** translate feature to the position relative to the device */
		// TODO i think position[0] must be translated negatively -> Check
		Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2]);

		if (parentMatrix != null) {
			Matrix.multiplyMM(tmpMatrix, 0, parentMatrix, 0, modelMatrix, 0);
			System.arraycopy(tmpMatrix, 0, modelMatrix, 0, 16);
			Matrix.setIdentityM(tmpMatrix, 0);
		}

		Matrix.multiplyMM(modelMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

		render(mvpMatrix, modelMatrix, lightPosition);
		
		for(OpenGLCallable childFeature : childrenFeatures){
			childFeature.render(projectionMatrix, viewMatrix, modelMatrix, lightPosition);
		}
	}

	public void render(float[] mvpMatrix) {
		/** sets the program object as part of current rendering state */
		if (!isInitialized)
			return;
		renderer.useProgram();
		
		if(enableBlending){
			GLES20.glEnable(GLES20.GL_BLEND);
		}
		if(enableCullFace){
			GLES20.glEnable(GLES20.GL_CULL_FACE);
		}

		if (texture != null) {
			// Set the active texture unit to texture unit 0.
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glUniform1i(renderer.getTextureUniform(), 0);

			// // Bind the texture to this unit.
			// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
			// textureDetails.textureId);

			texture.bindTexture();
		}
		renderer.setModelViewProjectionMatrix(mvpMatrix);
		
		/** render the geometry of this feature */
		if (geometry != null)
			geometry.render();
	}
	

	public void render(float[] mvpMatrix, float[] mvMatrix,
			float[] lightPosition) {
		/** sets the program object as part of current rendering state */
		renderer.useProgram();
		
		if(enableBlending){
			GLES20.glEnable(GLES20.GL_BLEND);
		}
		if(enableCullFace){
			GLES20.glEnable(GLES20.GL_CULL_FACE);
		}

		renderer.setLightPositionVec(lightPosition);
		
		if (texture != null) {
			// Set the active texture unit to texture unit 0.
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glUniform1i(renderer.getTextureUniform(), 0);

			// // Bind the texture to this unit.
			// GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
			// textureDetails.textureId);

			texture.bindTexture();
		}

		renderer.setModelViewMatrix(mvMatrix);
		renderer.setModelViewProjectionMatrix(mvpMatrix);

		/** render the geometry of this feature */
		if (geometry != null)
			geometry.render();
		
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
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

	@Override
	public void setTextureCallback(Callable<Bitmap> callback) {
		this.texture = Texture.createInstance(callback);
	}

	@Override
	public void setSubVisualization(DataSourceVisualizationGL subVisualizationGL) {
		if (this.childrenFeatures.contains(subVisualizationGL))
			return;
		this.childrenFeatures.add((OpenGLCallable) subVisualizationGL);
	}

	public void setLightPosition(float[] lightPosInEyeSpace) {
		GLES20.glUniform3f(renderer.getLightPosHandle(), lightPosInEyeSpace[0],
				lightPosInEyeSpace[1], lightPosInEyeSpace[2]);
	}
}
