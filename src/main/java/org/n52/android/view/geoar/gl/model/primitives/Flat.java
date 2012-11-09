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

/**
 * 
 * @author Arne de Wall
 *
 */
@Deprecated
public class Flat extends RenderNode {

	float width, height;
	int segmentsW, segmentsH;
	
	
	@Override
	public void onPreRender() {
		// TODO Auto-generated method stub
		
	}

	
	private void init(){
		int verticesCount = (segmentsW + 1) * (segmentsH + 1);
		float[] vertices = new float[verticesCount * 3];
		float[] normals = new float[verticesCount * 3];
		float[] colors = new float[verticesCount * 4];
		int[] indices = new int[segmentsW * segmentsH * 6];
		
		int vertexCount = 0, indexCount = 0;
		int colspan = segmentsW + 1;
		
		for(int i = 1; i <= segmentsW; i++){
			for(int j = 1; j <= segmentsH; j++){
				vertices[vertexCount] = ( i / segmentsH - .5f) * width;
				vertices[vertexCount+1] = 0;
				vertices[vertexCount+2] = (j / segmentsH - .5f) * height;
				
				normals[vertexCount] = 0;
				normals[vertexCount+1] = 1;
				normals[vertexCount+2] = 0;
				
				vertexCount += 3;
				
				int lr = i * colspan + j;
				int ll = lr - 1;
				int ur = lr - colspan;
				int ul = ur - 1;
				
				indices[indexCount++] = ul;
				indices[indexCount++] = ur;
				indices[indexCount++] = lr;
				indices[indexCount++] = ul;
				indices[indexCount++] = lr;
				indices[indexCount++] = ll;
			}
		}
		
		for(int i = 0, colorCount = vertexCount * 4; i < colorCount; i+=4){
			colors[i] = 1.0f;
			colors[i+1] = 1.0f;
			colors[i+2] = 1.0f;
			colors[i+3] = 1.0f;
		}
		
		setRenderObjectives(vertices, colors, normals, indices);
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
