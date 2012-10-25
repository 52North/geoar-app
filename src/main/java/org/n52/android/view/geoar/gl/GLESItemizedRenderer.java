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
package org.n52.android.view.geoar.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.data.Measurement;
import org.n52.android.data.MeasurementManager.MeasurementsCallback;
import org.n52.android.tracking.location.LocationConverter;
import org.n52.android.tracking.location.LocationVector;
import org.n52.android.view.geoar.GLESUtils;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.IRotationMatrixProvider;
import org.osmdroid.util.GeoPoint;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.SystemClock;

public class GLESItemizedRenderer  {
	
	private class POI{
		
		private final FloatBuffer mRenderObjectBuffer;
		private LocationVector relativeLocation;
		private Measurement measurement;
		private LocationVector screenCoords;
		private float[] screenCoordinates;
		
		// Store the model matrix. This matrix is used to move models from object space (where each model can be thought
		// of being located at the center of the universe) to world space.
	    private float[] mModelMatrix = new float[16];
	    
	    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
	    private float[] mMVPMatrix = new float[16];
		

		public POI(Measurement measurement){ 
			this.measurement = measurement;
			this.relativeLocation = LocationConverter.getRelativePositionVec(
					currentCenterGPoint, measurement.getLongitude(), measurement.getLatitude(), 0);
			
//			float color = (measurement.getValue() - min) / (max - min);
			float[] color = getColor(measurement.getValue());
			// This triangle is red, green, and blue.

			final float[] triangleVerticesData = {
				// X, Y, Z
				// R, G, B, A,
		        // Front face
		        -0.25f,-0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,-0.25f,0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Right face
		        0.25f,0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,-0.25f,0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,-0.25f,-0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Back face
		        0.25f,-0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,-0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,0.25f,-0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Left face
		        -0.25f,0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,-0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,0.25f,0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,-0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Bottom face
		        -0.25f,-0.25f,0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,-0.25f,-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,-0.25f,0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f,0.25f,-0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Move to top
		        0.25f,-0.25f,-0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,0.25f,0.25f,
		        color[0], color[1], 0.0f, 1.0f,
		        // Top face
		        -0.25f,	0.25f, 	0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f, 	0.25f, 	0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        -0.25f,	0.25f,	-0.25f, 
		        color[0], color[1], 0.0f, 1.0f,
		        0.25f, 	0.25f,	-0.25f,
		        color[0], color[1], 0.0f, 1.0f
//				color[0], color[1], 0.0f, 1.0f,
//				-0.25f, 1.0f, -0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				0.25f, 1.0f, -0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				
//				0.0f, 0.0f, 0.0f,
//				color[0], color[1], 0.0f, 1.0f,
//				-0.25f, 1.0f, 0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				-0.25f, 1.0f, -0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				
//				0.0f, 0.0f, 0.0f,
//				color[0], color[1], 0.0f, 1.0f,
//				0.25f, 1.0f, 0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				-0.25f, 1.0f, 0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				
//				
//				0.0f, 0.0f, 0.0f,
//				color[0], color[1], 0.0f, 1.0f,
//				0.25f, 1.0f, 0.25f,
//				color[0], color[1], 0.0f, 1.0f,
//				0.25f, 1.0f, -0.25f,
//				color[0], color[1], 0.0f, 1.0f,
			};
			// init the buffer
			mRenderObjectBuffer = ByteBuffer.allocateDirect(triangleVerticesData.length * mBytesPerFloat)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			
			mRenderObjectBuffer.put(triangleVerticesData).position(0);
		}
		
		private float[] getColor(float value){
			float interval = max-min;
			float threshold = min + interval/2;
			float[] res = new float[4];
			if(value <= threshold){ // grÃ¼n
				res[1] = 1;
				res[0] = 2*(value-min)/interval;
			}
			if(value > threshold){
				res[0] = 1;
				res[1] = 1 - 2* ((value-min)/interval - 0.5f);
			}
			return res;
		}
		/**
		 * Updates the position of the POI relative to the current location of the device.
		 * @param locationUpdate
		 *					new device location
		 */
		public void updateRelativePosition(GeoPoint locationUpdate) {
			this.relativeLocation = LocationConverter.getRelativePositionVec(
					locationUpdate, measurement.getLongitude(), measurement.getLatitude(), 0);
		}
		
		/**
		 * Draw the POI
		 */
		public void onDrawFrame() {
//			GLES20.glClear(mask)
			Matrix.setIdentityM(mModelMatrix, 0);
			 
			Matrix.translateM(mModelMatrix, 0, 0.0f, -1.6f, 0.0f);
			Matrix.translateM(mModelMatrix,  0, relativeLocation.x, relativeLocation.y, relativeLocation.z);
//			Matrix.rotateM(mModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);    
		    // Pass in the position information
			mRenderObjectBuffer.position(mPositionOffset);
		    GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
		            mStrideBytes, mRenderObjectBuffer);
		 
		    GLES20.glEnableVertexAttribArray(mPositionHandle);
			
		    // Pass in the color information
		    mRenderObjectBuffer.position(mColorOffset);
		    GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
		            mStrideBytes, mRenderObjectBuffer);
		 
		    GLES20.glEnableVertexAttribArray(mColorHandle);
			
		    // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
		    // (which currently contains model * view).
		    Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
		 
		    // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
		    // (which now contains model * view * projection).
		    Matrix.multiplyMM(mMVPMatrix, 0, GLESCamera.projectionMatrix, 0, mMVPMatrix, 0);
		 
		    GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
//			GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3);

			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 26);

		}
	}
	
	
	private boolean mIsVisible;
	// How many bytes per float
	private final int mBytesPerFloat = 4;
        private final int mStrideBytes = 7 * mBytesPerFloat;
        private final int mPositionOffset = 0;
        private final int mPositionDataSize = 3;
        private final int mColorOffset = 3;
        private final int mColorDataSize = 4; 
        // Shader-id
	private int mSimpleColorProgram;
	// various matrices handle
	private int mMVPMatrixHandle;
	private int mPositionHandle;
	private int mColorHandle;
	// Allocate storage for the final combined matrix. This will be passed into the shader program.
	private float[] mViewMatrix = new float[16];
    
	float angleInDegrees;
	
	private float min = Float.MAX_VALUE, max;
	private float currentGroundResolution;
	private GeoPoint currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
    
	private List<POI> poiList;
	private final IRotationMatrixProvider mRotationProvider;
	

	public GLESItemizedRenderer (IRotationMatrixProvider rotationProvider){
		
		final String vertexShader =
			    "uniform mat4 u_MVPMatrix;      \n"     // A constant representing the combined model/view/projection matrix.
			 
			  + "attribute vec4 a_Position;     \n"     // Per-vertex position information we will pass in.
			  + "attribute vec4 a_Color;        \n"     // Per-vertex color information we will pass in.
			 
			  + "varying vec4 v_Color;          \n"     // This will be passed into the fragment shader.
			 
			  + "void main()                    \n"     // The entry point for our vertex shader.
			  + "{                              \n"
			  + "   v_Color = a_Color;          \n"     // Pass the color through to the fragment shader.
			                                            // It will be interpolated across the triangle.
			  + "   gl_Position = u_MVPMatrix   \n"     // gl_Position is a special variable used to store the final position.
			  + "               * a_Position;   \n"     // Multiply the vertex by the matrix to get the final point in
			  + "}                              \n";    // normalized screen coordinates.
		
		final String fragmentShader =
			    "precision mediump float;       \n"     // Set the default precision to medium. We don't need as high of a
			                                            // precision in the fragment shader.
			  + "varying vec4 v_Color;          \n"     // This is the color from the vertex shader interpolated across the
			                                            // triangle per fragment.
			  + "void main()                    \n"     // The entry point for our fragment shader.
			  + "{                              \n"
			  + "   gl_FragColor = v_Color;     \n"     // Pass the color directly through the pipeline.
			  + "}                              \n";
		
		mSimpleColorProgram = GLESUtils.createProgram(vertexShader, fragmentShader);
		if(mSimpleColorProgram == 0)
			throw new RuntimeException("Could not compile the program");
		
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mSimpleColorProgram, "u_MVPMatrix");
		if(mMVPMatrixHandle == -1)
			throw new RuntimeException("get attrib location uMVPMatrix failed");
		
		mPositionHandle = GLES20.glGetAttribLocation(mSimpleColorProgram, "a_Position");
		if(mPositionHandle == -1)
			throw new RuntimeException("get attrib location aVertexPosition failed");
		
		mColorHandle = GLES20.glGetAttribLocation(mSimpleColorProgram, "a_Color");  
		if(mColorHandle == -1)
			throw new RuntimeException("get attrib location fVertexColor failed");
		
		this.mRotationProvider = rotationProvider;
		
		poiList = new ArrayList<POI>();
	}


	public void onDrawFrame() {
//		if(!mIsVisible)
//			return;
		
        // Do a complete rotation every 10 seconds.
        long time = SystemClock.uptimeMillis() % 10000L;
        angleInDegrees = (360.0f / 10000.0f) * ((int) time);
        
        
		GLES20.glUseProgram(mSimpleColorProgram);
		Matrix.multiplyMM(mViewMatrix, 0, mRotationProvider.getRotationMatrix(), 0, GLESCamera.viewMatrix, 0);
		for(POI p : poiList)
			p.onDrawFrame();
	}

	
	public void onLocationUpdate(GeoPoint locationUpdate) {
		this.currentCenterGPoint = locationUpdate;
		for(POI p : poiList)
			p.updateRelativePosition(currentCenterGPoint);
	}


	
	public void unloadResources() {
		this.mIsVisible = false;
	}
	
	
	public void loadResources() {
		this.mIsVisible = true;
	}

	/**
	 * Sets the new observations and 
	 * @param measurements
	 * 					list of measurements
	 */
	
	public void onObservationUpdate(MeasurementsCallback m) {
		// get max and min values for color interpolation
		float value;
		for(Measurement measure : m.measurementBuffer){
			value = measure.getValue();
			if(value > max)
				max = value;
			if(value < min)
				min = value;
		}
		List<POI> res = new ArrayList<POI>();
		for(Measurement measure : m.measurementBuffer){
			POI p = new POI(measure);
			res.add(p);
		}
		poiList = res;
	}
	
}
