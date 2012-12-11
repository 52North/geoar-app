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
package org.n52.android.view.geoar.gl.mode;

public class CubeFeature extends RenderFeature {
	
	/** standard color array */
	private static final float[] colors = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

	/** standard vertices array */
	private static final float[] vertices = { 
		0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f,	-0.5f,0.5f,	0.5f,-0.5f,	0.5f, // 0-1-halfSize-3 front
		0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f,-0.5f,-0.5f,0.5f,	0.5f, -0.5f, // 0-3-4-5 right
		0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f,-0.5f,	0.5f, 0.5f,	-0.5f, // 4-7-6-5 back
		-0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f,	-0.5f,-0.5f,0.5f, // 1-6-7-halfSize left
		0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f,0.5f,0.5f, // top0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, 0.5f,
		-0.5f, -0.5f, // bottom
	};

	/** standard normals array */
	private static final float[] normals = { 
			0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, // front
			1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, // right
			0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, // back
			-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, // left
			0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, // top
			0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, // bottom
	};

	/** standard indices array */ 
	private static final short[] indices = { 0, 1, 2, 0, 2, 3, 2, 1, 0, 2, 3, 0,
			4, 5, 6, 4, 6, 7, 8, 9, 10, 8, 10, 11, 12, 13, 14, 12, 14, 15, 16,
			17, 18, 16, 18, 19, 20, 21, 22, 20, 22, 23, };

	/**
	 * Variables
	 */
	private float[] myColor;
	private Float alpha;

	/***************************
	 * Constructor
	 ***************************/
	public CubeFeature() {
		super();
		setRenderObjectives(vertices, colors, normals, indices);
		this.renderer = new ColoredFeatureShader();
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
	public void enableCullface(boolean cullface) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableBlending(boolean blending, float alpha) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void enableDepthtest(boolean depthTest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDrawingMode(int drawingMode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateInGLESThread() {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void onPreRender() {
		// TODO Auto-generated method stub
		
	}

}
