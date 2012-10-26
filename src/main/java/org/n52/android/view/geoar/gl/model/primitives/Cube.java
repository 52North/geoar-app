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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.n52.android.view.geoar.gl.model.Geometry;
import org.n52.android.view.geoar.gl.model.RenderNode;

import android.graphics.Color;
import android.opengl.GLES20;

public class Cube extends RenderNode{
	
	private class CubeGeometry {
		private final float size;
		private final float[] vertices;
		private Geometry geometry;
		
		CubeGeometry(float size){
			this.size = size;
			final float divSize = size * 0.5f;
			vertices = new float[]{
                            divSize, divSize, divSize, -divSize, divSize, divSize, -divSize,-divSize, divSize, divSize,-divSize, divSize, 	//0-1-halfSize-3 front
                            divSize, divSize, divSize, divSize,-divSize, divSize,  divSize,-divSize,-divSize, divSize, divSize,-divSize,	//0-3-4-5 right
                            divSize,-divSize,-divSize, -divSize,-divSize,-divSize, -divSize, divSize,-divSize, divSize, divSize,-divSize,	//4-7-6-5 back
                            -divSize, divSize, divSize, -divSize, divSize,-divSize, -divSize,-divSize,-divSize, -divSize,-divSize, divSize,	//1-6-7-halfSize left
                            divSize, divSize, divSize, divSize, divSize,-divSize, -divSize, divSize,-divSize, -divSize, divSize, divSize, 	//top
                            divSize,-divSize, divSize, -divSize,-divSize, divSize, -divSize,-divSize,-divSize, divSize,-divSize,-divSize,	//bottom
			};
		}
	}
	
	private static Map<Float, CubeGeometry> cubeVertices = Collections.synchronizedMap(
			new WeakHashMap<Float, CubeGeometry>());
	
	private static final float[] colors = {
            1, 1, 1, 1, 	1, 1, 1, 1,	1, 1, 1, 1,	1, 1, 1, 1,
            1, 1, 1, 1,		1, 1, 1, 1,	1, 1, 1, 1, 	1, 1, 1, 1,
            1, 1, 1, 1, 	1, 1, 1, 1,	1, 1, 1, 1,	1, 1, 1, 1,
            1, 1, 1, 1,		1, 1, 1, 1,	1, 1, 1, 1, 	1, 1, 1, 1,
            1, 1, 1, 1, 	1, 1, 1, 1,	1, 1, 1, 1,	1, 1, 1, 1,
            1, 1, 1, 1,		1, 1, 1, 1,	1, 1, 1, 1, 	1, 1, 1, 1
	};
	
	private static final float[] normals = {
            0, 0, 1,   0, 0, 1,   0, 0, 1,   0, 0, 1,     //front
            1, 0, 0,   1, 0, 0,   1, 0, 0,   1, 0, 0,     // right
            0, 0,-1,   0, 0,-1,   0, 0,-1,   0, 0,-1,     //back
            -1, 0, 0,  -1, 0, 0,  -1, 0, 0,  -1, 0, 0,     // left
            0, 1, 0,   0, 1, 0,   0, 1, 0,   0, 1, 0,     //  top                          
            0,-1, 0,   0,-1, 0,   0,-1, 0,   0,-1, 0,     // bottom
	};
	
	private static final int[] indices = {
            0,1,2, 	0,2,3,
            2,1,0, 	2,3,0,
            4,5,6, 	4,6,7,
            8,9,10, 	8,10,11,
            12,13,14, 	12,14,15,
            16,17,18, 	16,18,19,
            20,21,22, 	20,22,23,
	};

	public Cube(float size){
		super();
		init(size, Cube.colors);

	}
	
	public Cube(float size, int color, Float alpha){
		float r = Color.red(color);
		float g = Color.green(color);
		float b = Color.blue(color);
		float a = alpha != null ? alpha : Color.alpha(color);
		float[] colors = {
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,
        		r, g, b, a, 	r, g, b, a,	r, g, b, a,	r, g, b, a,			
		};
		init(size, colors);
	}

	private void init(float size, float[] colors){
		CubeGeometry cv = cubeVertices.containsKey(size) ? cubeVertices.get(size) : new CubeGeometry(size);
		setRenderObjectives(cv.vertices, colors, normals, indices);
		if(cv.geometry == null){
			cv.geometry = geometry;
			cubeVertices.put(size, cv);
		}
		drawingMode = GLES20.GL_TRIANGLES;
	}
	
	@Override
	protected void onPreRender() {
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		
	}
}
