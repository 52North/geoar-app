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

public class FlatCircleFeature extends RenderFeature2 {

	private int numCirclePoints = 15;
	private int androidColor;

	public FlatCircleFeature() {
		this.drawingMode = GLES20.GL_TRIANGLES;
	}

	public FlatCircleFeature(final int numCirclePoints) {
		this.numCirclePoints = numCirclePoints;
		this.renderer = BilligerColorShader.getInstance();
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
		final int numOfVertices = (numCirclePoints+1)*3*3;
		final int numOfColorCoords = (numCirclePoints+1)*4*3;
		final int numOfTexCoords = (numCirclePoints+1)*2*3;
		
		final float[] vertices = new float[numOfVertices];
		final float[] colors = new float[numOfColorCoords];
		final float[] normals = new float[numOfVertices];
		final float[] texCoords = new float[numOfTexCoords];
		
		float radians = 0;
		for(int i = 0, point = 0; i < numOfVertices; i+=9, point++){
			vertices[i] = 0.0f;
			vertices[i+1] = 0.0f;
			vertices[i+2] = 0.0f;
			
			radians = (float) Math.toRadians(point*360/numCirclePoints);
			vertices[i+3] = FloatMath.cos(radians);
			vertices[i+5] = FloatMath.cos(radians);
			
			radians = (float) Math.toRadians((point+1)*360/numCirclePoints);
			vertices[i+6] = FloatMath.cos(radians);
			vertices[i+8] = FloatMath.cos(radians);
			
			normals[i+1] = 1.0f;
			normals[i+4] = 1.0f;
			normals[i+7] = 1.0f;
		}
		
		for (int i = 0; i < numOfColorCoords; i += 4) {
			colors[i] = 1.0f;
			colors[i + 1] = 0.0f;
			colors[i + 2] = 0.0f;
			colors[i + 3] = 0.1f;
		}
		
//		// circle center, we want no triangulation from the border
//		vertices[0] = 0.0f;
//		vertices[1] = 0.0f;
//		vertices[2] = 0.0f;
//		
//		normals[1] = 1.0f;
//		
//		for(int i = 3; i < numOfVertices; i+=3){
//			float radians = (float) Math.toRadians(i*360/numCirclePoints);
//			vertices[i] = FloatMath.cos(radians);
//			vertices[i+1] = 0.0f;
//			vertices[i+2] = FloatMath.sin(radians);
//			
//			normals[i] = 0.0f;
//			normals[i+1] = 1.0f;
//			normals[i+2] = 0.0f;
//		}
//		
//		for(int i = 0; i < numOfColorCoords; i+=4){
//			colors[i] = 1.0f;
//			colors[i+1] = 0.0f;
//			colors[i+2] = 0.0f;
//			colors[i+3] = 0.5f;
//		}
		
//		for(int i = 0; i < numOfTexCoords; i+=2){
//			texCoords[i] = 0.0f;
//			texCoords[i] = 0.0
//		}
		
		setRenderObjectives(vertices, colors, normals, null);
	}
}
