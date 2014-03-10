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

import org.n52.geoar.view.geoar.gl.mode.RenderFeature2;

import android.opengl.GLES20;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class CubeFeature2 extends RenderFeature2 {
	

	// X, Y, Z
	// Unused
	final float[] cubePositionData =
	{
			// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
			// if the points are counter-clockwise we are looking at the "front". If not we are looking at
			// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
			// usually represent the backside of an object and aren't visible anyways.
			
			// Front face
			-1.0f, 1.0f, 1.0f,				
			-1.0f, -1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 
			-1.0f, -1.0f, 1.0f, 				
			1.0f, -1.0f, 1.0f,
			1.0f, 1.0f, 1.0f,
			
			// Right face
			1.0f, 1.0f, 1.0f,				
			1.0f, -1.0f, 1.0f,
			1.0f, 1.0f, -1.0f,
			1.0f, -1.0f, 1.0f,				
			1.0f, -1.0f, -1.0f,
			1.0f, 1.0f, -1.0f,
			
			// Back face
			1.0f, 1.0f, -1.0f,				
			1.0f, -1.0f, -1.0f,
			-1.0f, 1.0f, -1.0f,
			1.0f, -1.0f, -1.0f,				
			-1.0f, -1.0f, -1.0f,
			-1.0f, 1.0f, -1.0f,
			
			// Left face
			-1.0f, 1.0f, -1.0f,				
			-1.0f, -1.0f, -1.0f,
			-1.0f, 1.0f, 1.0f, 
			-1.0f, -1.0f, -1.0f,				
			-1.0f, -1.0f, 1.0f, 
			-1.0f, 1.0f, 1.0f, 
			
			// Top face
			-1.0f, 1.0f, -1.0f,				
			-1.0f, 1.0f, 1.0f, 
			1.0f, 1.0f, -1.0f, 
			-1.0f, 1.0f, 1.0f, 				
			1.0f, 1.0f, 1.0f, 
			1.0f, 1.0f, -1.0f,
			
			// Bottom face
			1.0f, -1.0f, -1.0f,				
			1.0f, -1.0f, 1.0f, 
			-1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, 1.0f, 				
			-1.0f, -1.0f, 1.0f,
			-1.0f, -1.0f, -1.0f,
	};	
	
	// R, G, B, A
	// TODO unused
	final float[] cubeColorData =
	{				
			// Front face (red)
			1.0f, 0.0f, 0.0f, 1.0f,				
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,				
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			
			// Right face (green)
			0.0f, 1.0f, 0.0f, 1.0f,				
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,				
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			
			// Back face (blue)
			0.0f, 0.0f, 1.0f, 1.0f,				
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,				
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,
			
			// Left face (yellow)
			1.0f, 1.0f, 0.0f, 1.0f,				
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,				
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			
			// Top face (cyan)
			0.0f, 1.0f, 1.0f, 1.0f,				
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,				
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,
			
			// Bottom face (magenta)
			1.0f, 0.0f, 1.0f, 1.0f,				
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f,				
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f
	};
	
	// X, Y, Z
	// The normal is used in light calculations and is a vector which points
	// orthogonal to the plane of the surface. For a cube model, the normals
	// should be orthogonal to the points of each face.
	// TODO Unused
	final float[] cubeNormalData =
	{												
			// Front face
			0.0f, 0.0f, 1.0f,				
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f,				
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f,
			
			// Right face 
			1.0f, 0.0f, 0.0f,				
			1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f,				
			1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f,
			
			// Back face 
			0.0f, 0.0f, -1.0f,				
			0.0f, 0.0f, -1.0f,
			0.0f, 0.0f, -1.0f,
			0.0f, 0.0f, -1.0f,				
			0.0f, 0.0f, -1.0f,
			0.0f, 0.0f, -1.0f,
			
			// Left face 
			-1.0f, 0.0f, 0.0f,				
			-1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f,				
			-1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f,
			
			// Top face 
			0.0f, 1.0f, 0.0f,			
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,				
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			
			// Bottom face 
			0.0f, -1.0f, 0.0f,			
			0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f,				
			0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f
	};
	
	// S, T (or X, Y)
	// Texture coordinate data.
	// Because images have a Y axis pointing downward (values increase as you move down the image) while
	// OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
	// What's more is that the texture coordinates are the same for every face.
	final float[] cubeTextureCoordinateData =
	{												
			// Front face
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,				
			
			// Right face 
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,	
			
			// Back face 
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,	
			
			// Left face 
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,	
			
			// Top face 
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f,	
			
			// Bottom face 
			0.0f, 0.0f, 				
			0.0f, 1.0f,
			1.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f
	};

	/*****************************
	 * static variables
	 *****************************/
	
	// R, G, B, A
	private final float[] colors =
	{				
			// Front face (red)
			1.0f, 0.0f, 0.0f, 1.0f,				
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,				
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			
			// Right face (green)
			0.0f, 1.0f, 0.0f, 1.0f,				
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,				
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			
			// Back face (blue)
			0.0f, 0.0f, 1.0f, 1.0f,				
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,				
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f,
			
			// Left face (yellow)
			1.0f, 1.0f, 0.0f, 1.0f,				
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,				
			1.0f, 1.0f, 0.0f, 1.0f,
			1.0f, 1.0f, 0.0f, 1.0f,
			
			// Top face (cyan)
			0.0f, 1.0f, 1.0f, 1.0f,				
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,				
			0.0f, 1.0f, 1.0f, 1.0f,
			0.0f, 1.0f, 1.0f, 1.0f,
			
			// Bottom face (magenta)
			1.0f, 0.0f, 1.0f, 1.0f,				
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f,				
			1.0f, 0.0f, 1.0f, 1.0f,
			1.0f, 0.0f, 1.0f, 1.0f
	};

	// R, G, B, A
	private final float[] whitecolors =
	{				
			// Front face (red)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			
			// Right face (green)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			
			// Back face (blue)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			
			// Left face (yellow)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			
			// Top face (cyan)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			
			// Bottom face (magenta)
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,				
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f
	};

	
	
	private final float[] vertices =
	{
			// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
			// if the points are counter-clockwise we are looking at the "front". If not we are looking at
			// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
			// usually represent the backside of an object and aren't visible anyways.
			
			// Front face
			-0.5f, 0.5f, 0.5f,				
			-0.5f, -0.5f, 0.5f,
			0.5f, 0.5f, 0.5f, 
			-0.5f, -0.5f, 0.5f, 				
			0.5f, -0.5f, 0.5f,
			0.5f, 0.5f, 0.5f,
			
			// Right face
			0.5f, 0.5f, 0.5f,				
			0.5f, -0.5f, 0.5f,
			0.5f, 0.5f, -0.5f,
			0.5f, -0.5f, 0.5f,				
			0.5f, -0.5f, -0.5f,
			0.5f, 0.5f, -0.5f,
			
			// Back face
			0.5f, 0.5f, -0.5f,				
			0.5f, -0.5f, -0.5f,
			-0.5f, 0.5f, -0.5f,
			0.5f, -0.5f, -0.5f,				
			-0.5f, -0.5f, -0.5f,
			-0.5f, 0.5f, -0.5f,
			
			// Left face
			-0.5f, 0.5f, -0.5f,				
			-0.5f, -0.5f, -0.5f,
			-0.5f, 0.5f, 0.5f, 
			-0.5f, -0.5f, -0.5f,				
			-0.5f, -0.5f, 0.5f, 
			-0.5f, 0.5f, 0.5f, 
			
			// Top face
			-0.5f, 0.5f, -0.5f,				
			-0.5f, 0.5f, 0.5f, 
			0.5f, 0.5f, -0.5f, 
			-0.5f, 0.5f, 0.5f, 				
			0.5f, 0.5f, 0.5f, 
			0.5f, 0.5f, -0.5f,
			
			// Bottom face
			0.5f, -0.5f, -0.5f,				
			0.5f, -0.5f, 0.5f, 
			-0.5f, -0.5f, -0.5f,
			0.5f, -0.5f, 0.5f, 				
			-0.5f, -0.5f, 0.5f,
			-0.5f, -0.5f, -0.5f,
	};	
	
	/** standard vertices array */
	private final float[] normals = {
			// Front face
			0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			0.0f, 0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,

			// Right face
			1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f,
			1.0f,
			0.0f,
			0.0f,
			1.0f,
			0.0f,
			0.0f,

			// Back face
			0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f,
			-1.0f,
			0.0f,
			0.0f,
			-1.0f,

			// Left face
			-1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f,
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
			-1.0f,
			0.0f,
			0.0f,

			// Top face
			0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
			0.0f,

			// Bottom face
			0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
			0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
			-1.0f, 0.0f };


	
//	/** standard indices array */
//	private static final short[] indices = { 0, 4, 5, 0, 5, 1, 1, 5, 6, 1, 6,
//			2, 2, 6, 7, 2, 7, 3, 3, 7, 4, 3, 4, 0, 4, 7, 6, 4, 6, 5, 3, 0, 1,
//			3, 1, 2 };

	/***************************
	 * Constructor
	 ***************************/
	public CubeFeature2() {
		this.drawingMode = GLES20.GL_TRIANGLES;
		this.heightOffset = -0.5f;
	}

	@Override
	public void onCreateInGLESThread() {
		setRenderObjectives(vertices, whitecolors, normals, cubeTextureCoordinateData);
	}

	@Override
	public void setOpenGLPreRenderingSettings() {

	}

	@Override
	public void onPreRender() {

	}


	
	
//	/** standard color array */
//	private static final float[] whitecolors = { 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1,	1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
//		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
//
//	/** standard vertices array */
//	private static final float[] vertices = {
//        -0.5f, 	-0.5f, 	-0.5f,
//        0.5f, 	-0.5f, 	-0.5f,
//        0.5f,  	0.5f, 	-0.5f,
//        -0.5f,  0.5f, 	-0.5f,
//        -0.5f, 	-0.5f,  0.5f,
//        0.5f, 	-0.5f,  0.5f,
//        0.5f,  	0.5f,  	0.5f,
//        -0.5f,  0.5f,  	0.5f,
//	};
//
//	/** standard normals array */
//	private static final float[] normals = { 
//			0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, // front
//			1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, // right
//			0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, // back
//			-1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, // left
//			0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, // top
//			0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, // bottom
//	};
//
//	/** standard indices array */
//	private static final short[] indices = {
//        0, 4, 5,    0, 5, 1,
//        1, 5, 6,    1, 6, 2,
//        2, 6, 7,    2, 7, 3,
//        3, 7, 4,    3, 4, 0,
//        4, 7, 6,    4, 6, 5,
//        3, 0, 1,    3, 1, 2
//	};
}
