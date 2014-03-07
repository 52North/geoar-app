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
import org.n52.geoar.view.geoar.gl.mode.RenderFeature2;

import android.opengl.GLES20;
import android.util.FloatMath;

public class TriangleFeature extends RenderFeature2{
	
	final int numBorderPoints = 30;
	
	public TriangleFeature() {
		renderer = BilligerColorShader.getInstance();
		drawingMode = GLES20.GL_TRIANGLE_FAN;
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
		
		final int numOfVertices = (numBorderPoints+2) * 3;
		final int numOfColors = (numBorderPoints+2) * 4;
//		final int numOfTexCoords = (numBorderPoints+2) * 2;
		
		final float[] vertices = new float[numOfVertices];
		final float[] colors = new float[numOfColors];
		final float[] normals = new float[numOfVertices];
//		final float[] texCoords = new float[numOfTexCoords];
		
		for (int i = 3, point = 0; i < numOfVertices; i += 3, point++) {
			float radians = (float) Math.toRadians(360.f - point * 360.f / numBorderPoints);
			vertices[i] 	= FloatMath.cos(radians);
			vertices[i + 2] = FloatMath.sin(radians);

			normals[i + 1] = 1.0f;
		}
		
		/** set color coordinates - first point is in the middle */
		colors[0] = 1.0f;
		colors[3] = 1.0f;
		for (int i = 4; i < numOfColors; i += 4) {
			colors[i] = 1.0f;
			colors[i + 3] = 0.1f;
		}
		
		setRenderObjectives(vertices, colors, normals, null);
	}

}
