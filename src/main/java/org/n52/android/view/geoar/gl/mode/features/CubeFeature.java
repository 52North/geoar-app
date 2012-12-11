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

import org.n52.android.view.geoar.gl.mode.ColoredFeatureShader;
import org.n52.android.view.geoar.gl.mode.FeatureShader;
import org.n52.android.view.geoar.gl.mode.RenderFeature;
import org.n52.android.view.geoar.gl.model.primitives.Cube;

import android.opengl.GLES20;

public class CubeFeature extends RenderFeature {

	/*****************************
	 * static variables
	 *****************************/
	private static final String TAG = Cube.class.getSimpleName();

	/** standard color array */
	private static final float[] colors = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };

	/** standard vertices array */
	private static final float[] vertices = { 0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
			0.5f, -0.5f, -0.5f,
			0.5f,
			0.5f,
			-0.5f,
			0.5f, // 0-1-halfSize-3 front
			0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f,
			-0.5f,
			0.5f,
			0.5f,
			-0.5f, // 0-3-4-5 right
			0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f,
			0.5f,
			0.5f,
			-0.5f, // 4-7-6-5 back
			-0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,
			-0.5f,
			0.5f, // 1-6-7-halfSize left
			0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f,
			0.5f, 0.5f, // top0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f,
						// -0.5f, -0.5f, 0.5f,
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

	/***************************
	 * Constructor
	 ***************************/
	public CubeFeature() {
		this(new ColoredFeatureShader());
	}
	
	public CubeFeature(FeatureShader renderer){
		this.drawingMode = GLES20.GL_TRIANGLES;
		this.renderer = renderer;
	}
	
	@Override
	public void onCreateInGLESThread() {
		setRenderObjectives(vertices, colors, normals, indices);
	}

	@Override
	public void setOpenGLPreRenderingSettings() {

	}

	@Override
	public void onPreRender() {

	}
}
