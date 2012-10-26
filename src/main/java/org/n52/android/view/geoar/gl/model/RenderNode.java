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
package org.n52.android.view.geoar.gl.model;

import java.util.ArrayList;

import org.n52.android.view.geoar.gl.model.shader.Renderer;
import org.n52.android.view.geoar.gl.model.shader.SimpleColorRenderer;

import android.opengl.GLES20;
import android.opengl.Matrix;

public abstract class RenderNode extends Spatial {

	protected String name;

	protected float[] mvpMatrix = new float[16];
	protected float[] modelMatrix = new float[16];
	protected float[] projMatrix = new float[16];

	protected float[] scaleMatrix = new float[16];
	protected float[] translateMatrix = new float[16];
	protected float[] rotateMatrix = new float[16];

	protected float[] tmpMatrix = new float[16];

	protected ArrayList<RenderNode> children;
	protected Geometry geometry;
	protected Renderer renderer;

	protected int drawingMode = GLES20.GL_TRIANGLES;

	protected boolean enableBlending = false;
	protected boolean enableDepthTest = true;
	protected boolean enableDepthMask = true;
	protected boolean enableCullFace = false;

	protected boolean isVisible = true;
	protected boolean isComposition = false;

	public RenderNode() {
		children = new ArrayList<RenderNode>();
		geometry = new Geometry();
	}

	protected abstract void onPreRender();

	public void onRender(float[] projectionMatrix, float[] viewMatrix) {
		onRender(projectionMatrix, viewMatrix, null);
	}

	public void onRender(float[] projectionMatrix, float[] viewMatrix,
			final float[] parentMatrix) {
		if (!isVisible)
			return;

		// set identity
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.setIdentityM(scaleMatrix, 0);
		Matrix.setIdentityM(rotateMatrix, 0);
		Matrix.setIdentityM(tmpMatrix, 0);
		// Matrix.scaleM(scaleMatrix, 0, x, y, z)
		// Matrix.translateM(modelMatrix, 0, 0f, 0f, 5f);
		Matrix.translateM(modelMatrix, 0, position[0], position[1], position[2]);
		Matrix.translateM(modelMatrix, 0, 0.f, -1.6f, 0.f);
		// do we really need this step FIXME
		Matrix.multiplyMM(tmpMatrix, 0, modelMatrix, 0, scaleMatrix, 0);
		Matrix.multiplyMM(modelMatrix, 0, tmpMatrix, 0, rotateMatrix, 0);

		if (parentMatrix != null) {
			Matrix.multiplyMM(tmpMatrix, 0, parentMatrix, 0, modelMatrix, 0);
			System.arraycopy(tmpMatrix, 0, modelMatrix, 0, 16);
		}

		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

		// TODO check if bounds are inside view
		if (!isComposition) {
			onPreRender();
			this.projMatrix = projectionMatrix;
			// set blending settings
			if (enableBlending) {
				GLES20.glEnable(GLES20.GL_BLEND);
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA,
						GLES20.GL_ONE_MINUS_SRC_ALPHA);
			} else {
				GLES20.glDisable(GLES20.GL_BLEND);
			}

			// double sided rendering
			if (enableCullFace)
				GLES20.glEnable(GLES20.GL_CULL_FACE);
			else
				GLES20.glDisable(GLES20.GL_CULL_FACE);

			// depth Test
			if (enableDepthTest)
				GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			else
				GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			GLES20.glDepthMask(enableDepthMask);

			if (renderer == null) {
				renderer = SimpleColorRenderer.getInstance();
			}
			renderer.useProgram();
			renderer.setNormals(geometry.getNormalDetails().bufferHandle);
			renderer.setVertices(geometry.getVertexDetails().bufferHandle);
			renderer.setColors(geometry.getColorDetails().bufferHandle);

			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			renderer.setRenderMatrices(mvpMatrix, modelMatrix, viewMatrix);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER,
					geometry.getIndicesDetails().bufferHandle);
			// WTF! FU OPENGLES 2.0
			// GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0,
			// geometry.getVerticesCount());
			// GLES20.glDrawElements(drawingMode, geometry.getIndicesCount(),
			// GLES20.GL_UNSIGNED_INT, 0);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

			GLES20.glDisable(GLES20.GL_CULL_FACE);
			GLES20.glDisable(GLES20.GL_BLEND);
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		}

		for (RenderNode child : children) {
			child.onRender(projectionMatrix, viewMatrix, modelMatrix);
		}
	}

	/**
	 * @return the isComposition
	 */
	public boolean isComposition() {
		return isComposition;
	}

	/**
	 * @param isComposition
	 *            the isComposition to set
	 */
	public void setComposition(boolean isComposition) {
		this.isComposition = isComposition;
	}

	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, int[] indices) {
		geometry.setRenderObjectives(vertices, colors, normals, indices);
	}

	protected void setRenderObjectives(float[] vertices, float[] colors,
			float[] normals, short[] indices) {
		geometry.setRenderObjectives(vertices, colors, normals, indices);
	}

	public void setTransparent(boolean value) {
		this.enableBlending = value;
		this.enableDepthMask = !value;
	}

	// Name Getter / Setter

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// Children Getter / Setter

	public int getChildrenSize() {
		return children.size();
	}

	public void addChild(RenderNode child) {
		children.add(child);
	}

	public void removeChild(RenderNode child) {
		children.remove(child);
	}

	public RenderNode getChildAt(int index) {
		return children.get(index);
	}

	// Geometry Getter / Setter

	public Geometry getGeometry() {
		return geometry;
	}

	// Renderer Getter / Setter

	public void setRenderer(Renderer renderer) {
		// if (this.renderer != null) {
		// this.renderer = null;
		// System.gc();
		// }
		this.renderer = renderer;
	}

	public Renderer getRenderer() {
		return renderer;
	}

	// Visibilty Getter / Setter

	public void setVisibility(boolean visible) {
		this.isVisible = visible;
	}

	public boolean getVisibility() {
		return isVisible;
	}

	public void onDestroy() {
		if (geometry != null) {
			geometry = null;
		}
		if (renderer != null) {
			renderer.onDestroy();
			renderer = null;
		}
		for (int i = 0; i < children.size(); i++) {
			children.get(i).onDestroy();
		}
		children.clear();
		children = null;
	}

	/**
	 * @param geometry
	 *            the geometry to set
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
