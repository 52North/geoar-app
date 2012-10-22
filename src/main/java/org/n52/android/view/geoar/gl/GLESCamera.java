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

import org.n52.android.tracking.camera.RealityCamera;

import android.opengl.Matrix;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class GLESCamera {

    // Viewport of OpenGL Viewport
    public static int glViewportWidth;
    public static int glViewportHeight;

    public static float[] projectionMatrix;
    // Store the view matrix. This matrix transforms world space to eye space;
    // it positions things relative to our eye.
    public static float[] viewMatrix;

    public static int[] viewPortMatrix;

    public static void createViewMatrix() {
	// calculate the viewMatrix for OpenGL rendering
	float[] newViewMatrix = new float[16];
	Matrix.setIdentityM(newViewMatrix, 0);
	Matrix.setLookAtM(newViewMatrix, 0, 0.0f, 0.0f, 0.0f, // camera position
		0.0f, 0.0f, -5.0f, // look at
		0.0f, 1.0f, 0.0f); // up-vektor
	viewMatrix = newViewMatrix;
    }

    public static void createProjectionMatrix(int width, int height) {
	glViewportHeight = height;
	glViewportWidth = width;
	viewPortMatrix = new int[] { 0, 0, width, height };

	if (RealityCamera.cameraViewportHeight == 0 || RealityCamera.cameraViewportWidth == 0) {
	    // Set camera viewport if none exists
	    RealityCamera.setViewportSize(width, height);
	}

	// TODO own perspective matrix multiplication
	float[] newProjMatrix = new float[16];
	// perspectiveMatrix(newProjMatrix, 0, RealityCamera.fovY,
	// RealityCamera.aspect, RealityCamera.zNear,
	// RealityCamera.zFar);
	// projectionMatrix = newProjMatrix;

//	buildOpenGLProjectionByIntrinsics(newProjMatrix, 500, 500, 0, 240, 180, width, height, RealityCamera.zNear,
//		RealityCamera.zFar);
	openGLProjectionByIntrinsicCameraParamters(newProjMatrix, 500, 500, 240, 180, 480, 360, RealityCamera.zNear,
		RealityCamera.zFar);
	projectionMatrix = newProjMatrix;
    }

    /**
     * Define a projection matrix in terms of a field of view angle, an aspect
     * ratio, and z clip planes SOURCE: Android 4.0.3 API-LEVEL 15
     * 
     * @param m
     *            the float array that holds the perspective matrix
     * @param offset
     *            the offset into float array m where the perspective matrix
     *            data is written
     * @param fovy
     *            field of view in y direction, in degrees
     * @param aspect
     *            width to height aspect ratio of the viewport
     * @param zNear
     * @param zFar
     */
    private static void perspectiveMatrix(float[] m, int offset, float fovy, float aspect, float zNear, float zFar) {

	float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
	float rangeReciprocal = 1.0f / (zNear - zFar);

	m[offset + 0] = f / aspect;
	m[offset + 1] = 0.0f;
	m[offset + 2] = 0.0f;
	m[offset + 3] = 0.0f;

	m[offset + 4] = 0.0f;
	m[offset + 5] = f;
	m[offset + 6] = 0.0f;
	m[offset + 7] = 0.0f;

	m[offset + 8] = 0.0f;
	m[offset + 9] = 0.0f;
	m[offset + 10] = (zFar + zNear) * rangeReciprocal;
	m[offset + 11] = -1.0f;

	m[offset + 12] = 0.0f;
	m[offset + 13] = 0.0f;
	m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
	m[offset + 15] = 0.0f;

    }
    
    @Deprecated
    /**
     * 	Does not work!
     * @param frustum
     * @param alpha
     * @param beta
     * @param skew
     * @param u0
     * @param v0
     * @param imgWidth
     * @param imgHeight
     * @param nearClip
     * @param farClip
     */
    private static void buildOpenGLProjectionByIntrinsics(float[] frustum, float alpha, float beta, float skew,
	    float u0, float v0, int imgWidth, int imgHeight, float nearClip, float farClip) {
	// These parameters define the final viewport that is rendered into the
	// camera
	float L = 0;
	float R = imgWidth;
	float B = 0;
	float T = imgHeight;

	// near and far clipping planes, these only matter for the mapping from
	// world-space z-coordinate
	// into the depth coordinate for OpenGL
	float N = nearClip;
	float F = farClip;

	final float[] ortho = new float[] { 2.0f / (R - L), 0, 0, -(R + L) / (R - L), 0, 2.0f / (T - B), 0,
		-(T + B) / (T - B), 0, 0, -2.0f / (F - N), -(F + N) / (F - N), 0, 0, 0, 1.0f };

	final float[] proj = new float[] { alpha, skew, u0, 0, 0, beta, v0, 0, 0, 0, -(N + F), -N * F, 0, 0, 1.0f, 0 };

	Matrix.multiplyMM(frustum, 0, ortho, 0, proj, 0);
    }
    
    /**
     * Works fine
     * 		These Function builds the OpenGL Projection Matrix with the intrinsic camera parameters.
     * 		The OpenGL Camera model is quite different from the intrinsic paramters of a camera.
     * 
     * 		Math can be found at:
     * 			http://www.cl.cam.ac.uk/techreports/UCAM-CL-TR-634.pdf
     *
     * @param frustum 	stores the resulting paramters
     * @param fx	focal length in x-direction, from camera intrinsics
     * @param fy	focal length in y-direction, from camera intrinsics
     * @param cx	image origin translation in x direction, from camera intrinsics
     * @param cy	image origin translation in y direction, from camera intrinsics
     * @param imgWidth	image width, in pixels
     * @param imgHeight image height, in pixels
     * @param nearClip	near clipping plane z-location, 
     * @param farClip	far clipping plane z-location
     */
    private static void openGLProjectionByIntrinsicCameraParamters(float[] frustum, float fx, float fy, float cx, float cy, float imgWidth, float imgHeight, float nearClip, float farClip){
	
	final float[] yea = new float[] { 
		2 * fx / imgWidth, 	0, 			(2*cx/imgWidth)-1, 	0,
		0,			2*fy/imgHeight, 	(2*cy/imgHeight)-1,	0,
		0,			0,			-(farClip+nearClip)/(farClip-nearClip), -2*farClip*nearClip/(farClip-nearClip),
		0,			0,			-1,			0
	};
	
	System.arraycopy(yea, 0, frustum, 0, 16);	
    }
}