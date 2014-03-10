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
package org.n52.geoar.view.geoar.gl.mode.features;

import org.apache.http.MethodNotSupportedException;
import org.n52.geoar.view.geoar.gl.mode.BilligerColorShader;
import org.n52.geoar.view.geoar.gl.mode.FeatureShader;
import org.n52.geoar.view.geoar.gl.mode.RenderFeature2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.opengl.GLES20;

/**
 * 
 * @author Arne de Wall
 *
 */
public class NewGridFeature extends RenderFeature2 {
	private final int totalSize;
	private final int gridSize;
	private int lineColor;
	private float thickness = 2.5f;

	private int androidColor = -1;

	private static final Logger LOG = LoggerFactory
			.getLogger(NewGridFeature.class);

	public NewGridFeature() {
		this(BilligerColorShader.getInstance());
	}

	public NewGridFeature(FeatureShader renderer) {
		this.totalSize = 200;
		this.gridSize = 2;
		this.drawingMode = GLES20.GL_LINES;
		this.renderer = renderer;
	}

	@Override
	public void setColor(float[] colorArray) {
		try {
			throw new MethodNotSupportedException(
					"Setting Color array is not supported atm");
		} catch (MethodNotSupportedException e) { // Lol...
			LOG.debug("setColor array is not supported atm");
			e.printStackTrace();
		}
	}

	@Override
	public void onCreateInGLESThread() {
		final int centerToOrigin = totalSize / 2;

		final int numGridPerStride = totalSize / gridSize; // grids along a line
		final int numGridLinesPerStride = numGridPerStride + 1;
		/** count for lines in x and y direction */
		final int numTotalGridLines = numGridLinesPerStride * 2; 
		final int numGridEndPoints = numTotalGridLines * 2; // num of vertices

		final int numVertices = numGridEndPoints * SIZE_OF_POSITION;
		final int numColorCoords = numGridEndPoints * SIZE_OF_COLOR;

		final float[] vertices = new float[numVertices];
		final float[] colors = new float[numColorCoords];
		final float[] normals = new float[numVertices];
		
		for (int gridlineindex = 0, arrayindex = 0, nextgrid = 0; gridlineindex < numGridLinesPerStride; arrayindex +=12, gridlineindex++, nextgrid += gridSize ){
			vertices[arrayindex] = -centerToOrigin;
			vertices[arrayindex + 2] = -centerToOrigin + nextgrid;

			vertices[arrayindex + 3] = centerToOrigin;
			vertices[arrayindex + 5] = -centerToOrigin + nextgrid;
			
			vertices[arrayindex + 6] = -centerToOrigin + nextgrid;
			vertices[arrayindex + 8] = -centerToOrigin;

			vertices[arrayindex + 9] = -centerToOrigin + nextgrid;
			vertices[arrayindex + 11] = centerToOrigin;
		}

		for (int normalIndex = 1, length = normals.length; normalIndex < length; normalIndex += 3)
			normals[normalIndex] = 1.0f;

		// if (androidColor != -1) {
		// r = Color.red(androidColor);
		// g = Color.green(androidColor);
		// b = Color.blue(androidColor);
		// a = 0.5f;// (alpha == -1 ? Color.alpha(androidColor) : alpha);
		// } else {
		// r = 1.0f;
		// g = 1.0f;
		// b = 1.0f;
		// a = 0.5f;
		// }

		for (int i = 0; i < numColorCoords; i += 4) {
			colors[i] = 1.0f;
			colors[i + 1] = 1.0f;
			colors[i + 2] = 1.0f;
			colors[i + 3] = 0.75f;
		}

		 setRenderObjectives(vertices, colors, normals, null);
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
