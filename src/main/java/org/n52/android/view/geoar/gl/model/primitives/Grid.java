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
package org.n52.android.view.geoar.gl.model.primitives;

import org.n52.android.view.geoar.gl.model.RenderNode;

import android.graphics.Color;
import android.opengl.GLES20;

public class Grid extends RenderNode {
	
	private int gridSize;
	private int lineColor;
	private float thickness = 5.f;
	private float alpha = -1;
	
	public Grid(){
		gridSize = 80;
		init();
	}
	
	public Grid(int gridSize){
		this.gridSize = gridSize;
		init();
	}
	
	public Grid(int gridSize, int thickness, int color, float alpha){
		this(gridSize);
		this.thickness = thickness;
		this.lineColor = color;
		this.alpha = alpha;
	}
	
	private void init(){
		
		enableCullFace = false;
		drawingMode = GLES20.GL_LINES;
		
		final float[] vertices 	= new float[gridSize*6*2];
		final float[] colors 	= new float[gridSize*16];
		final float[] normals 	= new float[gridSize*16];
		final int[]   indices 	= new int[gridSize*2];
		
		final int halfSize = gridSize/2;
		
		final float r = Color.red(lineColor);
		final float g = Color.green(lineColor);
		final float b = Color.blue(lineColor);
		final float a = (alpha == -1 ? Color.alpha(lineColor) : alpha);

		for(int i = 0; i < gridSize; i++){
			vertices[i*6] = -halfSize;
			vertices[i*6+1] = 0.0f;
			vertices[i*6+2] = -halfSize + i + 0.0f;
			
			vertices[i*6+3] = gridSize-1.f;
			vertices[i*6+4] = 0.0f;
			vertices[i*6+5] = -halfSize + i + 0.0f;
			indices[i] = i;
		}
		
		for(int i = gridSize, var = gridSize*2; i < var; i++){
			vertices[i*6] 	= -halfSize + i + 0.0f-gridSize;
			vertices[i*6+1] = 0.0f;
			vertices[i*6+2] = -halfSize;
			
			vertices[i*6+3] =-halfSize + i + 0.0f-gridSize;
			vertices[i*6+4] = 0.0f;
			vertices[i*6+5] = gridSize-1;
			
			indices[i] = i;
		}

		for(int i = 0, var = gridSize*4; i < var; i++){
			colors[i*4] = 1.0f;
			colors[i*4 + 1] = 1.0f;
			colors[i*4 + 2] = 1.0f;
			colors[i*4 + 3] = 1.0f;
			
			normals[i*3] = 0;
			normals[i*3+1] = 0;
			normals[i*3+2] = 1;
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

	@Override
	public void setColor(int androidColor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColor(float[] colorArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateInGLESThread() {
		// TODO Auto-generated method stub
		
	}
}
