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

import org.n52.geoar.view.geoar.gl.mode.BilligerColorShader;
import org.n52.geoar.view.geoar.gl.mode.FeatureShader;
import org.n52.geoar.view.geoar.gl.mode.RenderFeature2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class HeightMapFeature extends RenderFeature2 {

	
	static final int SIZE_PER_SIDE = 64;
	static final float MIN_POSITION = -10f;
	static final float POSITION_RANGE = 20f;

	private static final int POSITION_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int NORMAL_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int COLOR_DATA_SIZE_IN_ELEMENTS = 4;

	private static final Logger LOG = LoggerFactory
			.getLogger(HeightMapFeature.class);

	public HeightMapFeature() {
		this(BilligerColorShader.getInstance());
	}

	public HeightMapFeature(FeatureShader renderer) {
		this.renderer = renderer;
		this.drawingMode = GLES20.GL_TRIANGLE_STRIP;
	}

	@Override
	public void setOpenGLPreRenderingSettings() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreRender() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCreateInGLESThread() {
		try {
			final int floatsPerVertex = POSITION_DATA_SIZE_IN_ELEMENTS; // +
			// NORMAL_DATA_SIZE_IN_ELEMENTS
			// + COLOR_DATA_SIZE_IN_ELEMENTS;
			final int floatsPerColor = COLOR_DATA_SIZE_IN_ELEMENTS;
			final int floatsPerNormal = NORMAL_DATA_SIZE_IN_ELEMENTS;
			final int xLength = SIZE_PER_SIDE;
			final int yLength = SIZE_PER_SIDE;

			final float[] vertices = new float[xLength * yLength
					* floatsPerVertex];
			final float[] colors = new float[xLength * yLength * floatsPerColor];
			final float[] normals = new float[xLength * yLength
					* floatsPerNormal];

			int indexCount;
			int offset = 0;
			int colorOffset = 0;
			int normalOffSet = 0;

			// First, build the data for the vertex buffer
			for (int y = 0; y < yLength; y++) {
				for (int x = 0; x < xLength; x++) {
					final float xRatio = x / (float) (xLength - 1);

					// Build our heightmap from the top down, so that our
					// triangles are counter-clockwise.
					final float yRatio = 1f - (y / (float) (yLength - 1));

					final float xPosition = MIN_POSITION
							+ (xRatio * POSITION_RANGE);
					final float yPosition = MIN_POSITION
							+ (yRatio * POSITION_RANGE);

					vertices[offset++] = xPosition;

					vertices[offset++] = ((xPosition * xPosition) + (yPosition * yPosition)) / 20f;
					vertices[offset++] = yPosition;

					final float xSlope = (2 * xPosition) / 10f;
					final float ySlope = (2 * yPosition) / 10f;

					// Calculate the normal using the cross product of the
					// slopes.
					final float[] planeVectorX = { 1f, 0f, xSlope };
					final float[] planeVectorY = { 0f, 1f, ySlope };
					final float[] normalVector = {
							(planeVectorX[1] * planeVectorY[2])
									- (planeVectorX[2] * planeVectorY[1]),
							(planeVectorX[2] * planeVectorY[0])
									- (planeVectorX[0] * planeVectorY[2]),
							(planeVectorX[0] * planeVectorY[1])
									- (planeVectorX[1] * planeVectorY[0]) };

					// Normalize the normal
					final float length = Matrix.length(normalVector[0],
							normalVector[1], normalVector[2]);

					normals[normalOffSet++] = normalVector[0] / length;
					normals[normalOffSet++] = normalVector[1] / length;
					normals[normalOffSet++] = normalVector[2] / length;

					colors[colorOffset++] = xRatio;
					colors[colorOffset++] = yRatio;
					colors[colorOffset++] = 0.5f;
					colors[colorOffset++] = 0.5f;
				}
			}
			// Now build the index data
			final int numStripsRequired = yLength - 1;
			final int numDegensRequired = 2 * (numStripsRequired - 1);
			final int verticesPerStrip = 2 * xLength;

			final short[] heightMapIndexData = new short[(verticesPerStrip * numStripsRequired)
					+ numDegensRequired];

			offset = 0;

			for (int y = 0; y < yLength - 1; y++) {
				if (y > 0) {
					// Degenerate begin: repeat first vertex
					heightMapIndexData[offset++] = (short) (y * yLength);
				}

				for (int x = 0; x < xLength; x++) {
					// One part of the strip
					heightMapIndexData[offset++] = (short) ((y * yLength) + x);
					heightMapIndexData[offset++] = (short) (((y + 1) * yLength) + x);
				}

				if (y < yLength - 2) {
					// Degenerate end: repeat last vertex
					heightMapIndexData[offset++] = (short) (((y + 1) * yLength) + (xLength - 1));
				}
			}

			indexCount = heightMapIndexData.length;

			setRenderObjectives(vertices, colors, normals, null,
					heightMapIndexData);
		} catch (Throwable t) {
			LOG.debug("Unknown error", t);
		}
	}
}
