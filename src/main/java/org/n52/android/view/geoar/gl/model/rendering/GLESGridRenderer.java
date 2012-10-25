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
package org.n52.android.view.geoar.gl.model.rendering;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.n52.android.data.MeasurementManager.MeasurementsCallback;
import org.n52.android.view.geoar.ARViewController.IRenderable;
import org.n52.android.view.geoar.gl.GLESCamera;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.IRotationMatrixProvider;
import org.osmdroid.util.GeoPoint;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class GLESGridRenderer  {
	
	private FloatBuffer vertexBuffer;
	
	private float[] vertices = {
			0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 10.0f,
			0.0f, 0.0f, 0.0f,
			0.0f, 10.0f, 0.0f,
			0.0f, 0.0f, 0.0f,
			10.0f, 0.0f, 0.0f,
	};
	
	/** Store our model data in a float buffer. */
	private final FloatBuffer mCubePositions;
	
	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;	
	
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
	
	/** This will be used to pass in the transformation matrix. */
	private int mMVPMatrixHandle;
	
	/** This will be used to pass in model position information. */
	private int mPositionHandle;
	
	/** This will be used to pass in model color information. */
	private int mColorHandle;
	
	
    private final int mStrideBytes = 7 * mBytesPerFloat;
    private final int mPositionOffset = 0;
    private final int mPositionDataSize = 3;
    private final int mColorOffset = 3;
    private final int mColorDataSize = 4; 
	
	
	public GLESGridRenderer(IRotationMatrixProvider rotationProvider){
		
		final String vertexShader =
				"uniform mat4 u_MVPMatrix;      \n"		// A constant representing the combined model/view/projection matrix.
				
			  + "attribute vec4 a_Position;     \n"		// Per-vertex position information we will pass in.
			  + "attribute vec4 a_Color;        \n"		// Per-vertex color information we will pass in.			  
			  
			  + "varying vec4 v_Color;          \n"		// This will be passed into the fragment shader.
			  
			  + "void main()                    \n"		// The entry point for our vertex shader.
			  + "{                              \n"
			  + "   v_Color = a_Color;          \n"		// Pass the color through to the fragment shader. 
			  											// It will be interpolated across the triangle.
			  + "   gl_Position = u_MVPMatrix   \n" 	// gl_Position is a special variable used to store the final position.
			  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in 			                                            			 
			  + "}                              \n";    // normalized screen coordinates.
			
			final String fragmentShader =
				"precision mediump float;       \n"		// Set the default precision to medium. We don't need as high of a 
														// precision in the fragment shader.				
			  + "varying vec4 v_Color;          \n"		// This is the color from the vertex shader interpolated across the 
			  											// triangle per fragment.			  
			  + "void main()                    \n"		// The entry point for our fragment shader.
			  + "{                              \n"
			  + "   gl_FragColor = v_Color;     \n"		// Pass the color directly through the pipeline.		  
			  + "}                              \n";		
			
			// Load in the vertex shader.
			int vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);

			if (vertexShaderHandle != 0) 
			{
				// Pass in the shader source.
				GLES20.glShaderSource(vertexShaderHandle, vertexShader);

				// Compile the shader.
				GLES20.glCompileShader(vertexShaderHandle);

				// Get the compilation status.
				final int[] compileStatus = new int[1];
				GLES20.glGetShaderiv(vertexShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

				// If the compilation failed, delete the shader.
				if (compileStatus[0] == 0) 
				{				
					GLES20.glDeleteShader(vertexShaderHandle);
					vertexShaderHandle = 0;
				}
			}

			if (vertexShaderHandle == 0)
			{
				throw new RuntimeException("Error creating vertex shader.");
			}
			
			// Load in the fragment shader shader.
			int fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

			if (fragmentShaderHandle != 0) 
			{
				// Pass in the shader source.
				GLES20.glShaderSource(fragmentShaderHandle, fragmentShader);

				// Compile the shader.
				GLES20.glCompileShader(fragmentShaderHandle);

				// Get the compilation status.
				final int[] compileStatus = new int[1];
				GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

				// If the compilation failed, delete the shader.
				if (compileStatus[0] == 0) 
				{				
					GLES20.glDeleteShader(fragmentShaderHandle);
					fragmentShaderHandle = 0;
				}
			}

			if (fragmentShaderHandle == 0)
			{
				throw new RuntimeException("Error creating fragment shader.");
			}
			
			// Create a program object and store the handle to it.
			int programHandle = GLES20.glCreateProgram();
			
			if (programHandle != 0) 
			{
				// Bind the vertex shader to the program.
				GLES20.glAttachShader(programHandle, vertexShaderHandle);			

				// Bind the fragment shader to the program.
				GLES20.glAttachShader(programHandle, fragmentShaderHandle);
				
				// Bind attributes
				GLES20.glBindAttribLocation(programHandle, 0, "a_Position");
				GLES20.glBindAttribLocation(programHandle, 1, "a_Color");
				
				// Link the two shaders together into a program.
				GLES20.glLinkProgram(programHandle);

				// Get the link status.
				final int[] linkStatus = new int[1];
				GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);

				// If the link failed, delete the program.
				if (linkStatus[0] == 0) 
				{				
					GLES20.glDeleteProgram(programHandle);
					programHandle = 0;
				}
			}
			
			if (programHandle == 0)
			{
				throw new RuntimeException("Error creating program.");
			}
	        
	        // Set program handles. These will later be used to pass in values to the program.
	        mMVPMatrixHandle = GLES20.glGetUniformLocation(programHandle, "u_MVPMatrix");        
	        mPositionHandle = GLES20.glGetAttribLocation(programHandle, "a_Position");
	        mColorHandle = GLES20.glGetAttribLocation(programHandle, "a_Color");        
	        
	        // Tell OpenGL to use this program when rendering.
	        GLES20.glUseProgram(programHandle);        

		// Initialize the buffers.
		mCubePositions = ByteBuffer.allocateDirect(vertices.length * mBytesPerFloat)
        .order(ByteOrder.nativeOrder()).asFloatBuffer();							
		mCubePositions.put(vertices).position(0);	
	}
	
	
	public void onDrawFrame() {

//		GLES20.glClear(mask)
		Matrix.setIdentityM(mModelMatrix, 0);
		 
		Matrix.translateM(mModelMatrix, 0, 0.0f, -1.6f, 0.0f);
//		Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);    
	    // Pass in the position information
		mCubePositions.position(mPositionOffset);
	    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
	            mStrideBytes, mCubePositions);
	 
	    GLES20.glEnableVertexAttribArray(mPositionHandle);
		
//	    // Pass in the color information
//	    mCubePositions.position(mColorOffset);
//	    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
//	            mStrideBytes, mCubePositions);
	 
//	    GLES20.glEnableVertexAttribArray(mColorHandle);
		
	    // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
	    // (which currently contains model * view).
	    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
	 
	    // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
	    // (which now contains model * view * projection).
	    Matrix.multiplyMM(mMVPMatrix, 0, GLESCamera.projectionMatrix, 0, mMVPMatrix, 0);
	 
	    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
//		GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

		GLES20.glDrawArrays(GLES20.GL_LINES, 0, 3);
	}

	
//	public void onLocationUpdate(GeoPoint locationUpdate) {
//		// TODO Auto-generated method stub
//		
//	}

	
	public void onObservationUpdate(MeasurementsCallback m) {
		// TODO Auto-generated method stub
		
	}

	
	public void loadResources() {
		// TODO Auto-generated method stub
		
	}

	
	public void unloadResources() {
		// TODO Auto-generated method stub
		
	}

}
