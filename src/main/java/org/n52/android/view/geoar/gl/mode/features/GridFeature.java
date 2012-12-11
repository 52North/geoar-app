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
package org.n52.android.view.geoar.gl.mode.features;

import org.apache.http.MethodNotSupportedException;
import org.n52.android.view.geoar.gl.mode.ColoredFeatureShader;
import org.n52.android.view.geoar.gl.mode.FeatureShader;
import org.n52.android.view.geoar.gl.mode.RenderFeature;

import android.graphics.Color;
import android.opengl.GLES20;
import android.util.Log;

public class GridFeature extends RenderFeature {
	private int gridSize;
	private int lineColor;
	private float thickness = 1.f;

	private int androidColor = -1;

	public GridFeature() {
		this(new ColoredFeatureShader());
	}

	public GridFeature(FeatureShader renderer) {
		gridSize = 80;
		this.drawingMode = GLES20.GL_LINES;
		this.renderer = renderer;
		onCreateInGLESThread();
	}

	@Override
	public void setColor(float[] colorArray) {
		try {
			throw new MethodNotSupportedException(
					"Setting Color array is not supported atm");
		} catch (MethodNotSupportedException e) { // Lol...
			Log.d(this.getClass().getSimpleName(),
					"setColor array is not supported atm");
			e.printStackTrace();
		}
	}

	@Override
	public void onCreateInGLESThread() {
		final float[] vertices = new float[gridSize * 6 * 2];
		final float[] colors = new float[gridSize * 16];
		final float[] normals = new float[gridSize * 16];
		final short[] indices = new short[gridSize * 4];

		final int halfSize = gridSize / 2;

		for (int i = 0; i < gridSize; i++) {
			vertices[i * 6] = -halfSize;
			vertices[i * 6 + 1] = 0.0f;
			vertices[i * 6 + 2] = -halfSize + i + 0.0f;

			vertices[i * 6 + 3] = gridSize - 1.f;
			vertices[i * 6 + 4] = 0.0f;
			vertices[i * 6 + 5] = -halfSize + i + 0.0f;
		}

		for (int i = gridSize, var = gridSize * 2; i < var; i++) {
			vertices[i * 6] = -halfSize + i + 0.0f - gridSize;
			vertices[i * 6 + 1] = 0.0f;
			vertices[i * 6 + 2] = -halfSize;

			vertices[i * 6 + 3] = -halfSize + i + 0.0f - gridSize;
			vertices[i * 6 + 4] = 0.0f;
			vertices[i * 6 + 5] = gridSize - 1.f;
		}

		/** init indices array */
		for (short index = 0, length = (short) indices.length; index < length; index++)
			indices[index] = index;

		/** init normals array **/
		for (int normalIndex = 2, length = normals.length; normalIndex < length; normalIndex += 3)
			normals[normalIndex] = 1;

		final float r, g, b, a;

		if (androidColor != -1) {
			r = Color.red(androidColor);
			g = Color.green(androidColor);
			b = Color.blue(androidColor);
			a = (alpha == -1 ? Color.alpha(androidColor) : alpha);
		} else {
			r = 1.0f;
			g = 1.0f;
			b = 1.0f;
			a = 1.0f;
		}

		for (int i = 0, length = gridSize * 4; i < length; i += 4) {
			colors[i] = r;
			colors[i + 1] = g;
			colors[i + 2] = b;
			colors[i + 3] = a;
		}

		setRenderObjectives(vertices, colors, normals, indices);
	}
	
	@Override
	public void onPreRender() {
		GLES20.glLineWidth(thickness);
	}

	@Override
	public void setOpenGLPreRenderingSettings() {
		// TODO Auto-generated method stub

	}
}
