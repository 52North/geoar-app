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
package org.n52.android.view.geoar.gl.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.mapsforge.core.GeoPoint;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.IRotationMatrixProvider;
import org.n52.android.view.geoar.gl.GLESCamera;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public class GLESGridRenderer  {
	
	/** Used for debug logs. */
	private static final String TAG = "LessonTwoRenderer";
	
	/**
	 * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
	 * of being located at the center of the universe) to world space.
	 */
	private float[] mModelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
	 * it positions things relative to our eye.
	 */
	private float[] mViewMatrix = new float[16];

	/** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
	private float[] mProjectionMatrix = new float[16];
	
	/** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	private float[] mMVPMatrix = new float[16];
	
	/** 
	 * Stores a copy of the model matrix specifically for the light position.
	 */
	private float[] mLightModelMatrix = new float[16];	
	
	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	private final FloatBuffer mCubeColors;
	private final FloatBuffer mCubeNormals;
	
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;
	
	/** This will be used to pass in the modelview matrix. */
	private int mMVMatrixHandle;
	
	/** This will be used to pass in the light position. */
	private int mLightPosHandle;
	
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	
	/** This will be used to pass in model color information. */
	private int mColorHandle;
	
	/** This will be used to pass in model normal information. */
	private int mNormalHandle;

	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;	
	
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;	
	
	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;	
	
	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;
	
	/** Used to hold a light centered on the origin in model space. We need a 4th coordinate so we can get translations to work when
	 *  we multiply this by our transformation matrices. */
	private final float[] mLightPosInModelSpace = new float[] {0.0f, 0.0f, 0.0f, 1.0f};
	
	/** Used to hold the current position of the light in world space (after transformation via model matrix). */
	private final float[] mLightPosInWorldSpace = new float[4];
	
	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];
	
	/** This is a handle to our per-vertex cube shading program. */
	private int mPerVertexProgramHandle;
		
	/** This is a handle to our light point program. */
	private int mPointProgramHandle;		
				
	
	
	public GLESGridRenderer(IRotationMatrixProvider rotationProvider){
		// Define points for a cube.		
		
		// X, Y, Z
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
		
		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(cubePositionData).position(0);		
		
		mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeColors.put(cubeColorData).position(0);
		
		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubeNormals.put(cubeNormalData).position(0);
	}
	
	protected String getVertexShader()
	{
		// TODO: Explain why we normalize the vectors, explain some of the vector math behind it all. Explain what is eye space.
		final String vertexShader =
			"uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
		  + "uniform mat4 u_MVMatrix;       \n"		// A constant representing the combined model/view matrix.	
		  + "uniform vec3 u_LightPos;       \n"	    // The position of the light in eye space.
			
		  + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
		  + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.
		  + "attribute vec3 a_Normal;       \n"		// Per-vertex normal information we will pass in.
		  
		  + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
		  
		  + "void main()                    \n" 	// The entry point for our vertex shader.
		  + "{                              \n"		
		// Transform the vertex into eye space.
		  + "   vec3 modelViewVertex = vec3(u_MVMatrix * a_Position);              \n"
		// Transform the normal's orientation into eye space.
		  + "   vec3 modelViewNormal = vec3(u_MVMatrix * vec4(a_Normal, 0.0));     \n"
		// Will be used for attenuation.
		  + "   float distance = length(u_LightPos - modelViewVertex);             \n"
		// Get a lighting direction vector from the light to the vertex.
		  + "   vec3 lightVector = normalize(u_LightPos - modelViewVertex);        \n"
		// Calculate the dot product of the light vector and vertex normal. If the normal and light vector are
		// pointing in the same direction then it will get max illumination.
		  + "   float diffuse = max(dot(modelViewNormal, lightVector), 0.1);       \n" 	  		  													  
		// Attenuate the light based on distance.
		  + "   diffuse = diffuse * (1.0 / (1.0 + (0.25 * distance * distance)));  \n"
		// Multiply the color by the illumination level. It will be interpolated across the triangle.
		  + "   v_Color = a_Color * diffuse;                                       \n" 	 
		// gl_Position is a special variable used to store the final position.
		// Multiply the vertex by the matrix to get the final point in normalized screen coordinates.		
		  + "   gl_Position = u_MVPMatrix * a_Position;                            \n"     
		  + "}                                                                     \n"; 
		
		return vertexShader;
	}
	
	protected String getFragmentShader()
	{
		final String fragmentShader =
			"precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a 
													// precision in the fragment shader.				
		  + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the 
		  											// triangle per fragment.			  
		  + "void main()                    \n"		// The entry point for our fragment shader.
		  + "{                              \n"
		  + "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.		  
		  + "}                              \n";
		
		return fragmentShader;
	}
	
	
	public void onDrawFrame() {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);			        
        
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;        
        float angleInDegrees = (360.0f / 10000.0f) * ((int) time);                
        
        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mPerVertexProgramHandle);
        
        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_MVMatrix"); 
        mLightPosHandle = GLES20.glGetUniformLocation(mPerVertexProgramHandle, "u_LightPos");
        mPositionHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Position");
        mColorHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Color");
        mNormalHandle = GLES20.glGetAttribLocation(mPerVertexProgramHandle, "a_Normal"); 
        
        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, -5.0f);      
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);
               
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mViewMatrix, 0, mLightPosInWorldSpace, 0);                        
        
        // Draw some cubes.        
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.translateM(mModelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 1.0f, 0.0f, 0.0f);        
        drawCube();
	}
	
	/**
	 * Draws a cube.
	 */			
	private void drawCube()
	{		
		// Pass in the position information
		mCubePositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mCubePositions);        
                
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        		0, mCubeColors);        
        
        GLES20.glEnableVertexAttribArray(mColorHandle);
        
        // Pass in the normal information
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
        		0, mCubeNormals);
        
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        
		// This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   
        
        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                
        
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        
        // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);                               
	}	


}
