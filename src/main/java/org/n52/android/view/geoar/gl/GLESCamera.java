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

import java.util.Arrays;

import javax.microedition.khronos.opengles.GL10;

import org.n52.android.tracking.camera.RealityCamera;

import android.opengl.Matrix;
import android.util.FloatMath;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class GLESCamera {

	private static class GeometryPlane {
		private static float[] mTmp1;
		private static float[] mTmp2;
		final float[] normal = new float[3];
		float dot = 0;

		void set(float[] p1, float[] p2, float[] p3) {
			mTmp1 = Arrays.copyOf(p1, 3);
			mTmp2 = Arrays.copyOf(p2, 3);

			mTmp1[0] -= mTmp2[0];
			mTmp1[1] -= mTmp2[1];
			mTmp1[2] -= mTmp2[2];

			mTmp2[0] -= p3[0];
			mTmp2[1] -= p3[1];
			mTmp2[2] -= p3[2];

			// cross produkt in order to calculate the normal
			normal[0] = mTmp1[1] * mTmp2[2] - mTmp1[2] * mTmp2[1];
			normal[1] = mTmp1[2] * mTmp2[0] - mTmp1[0] * mTmp2[2];
			normal[2] = mTmp1[0] * mTmp2[1] - mTmp1[1] * mTmp2[0];

			// normalizing the result
			// According to Lint faster than FloatMath
			float a = (float) Math.sqrt(normal[0] * normal[0] + normal[1]
					* normal[1] + normal[2] * normal[2]);
			if (a != 0 && a != 1) {
				a = 1 / a;
				normal[0] *= a;
				normal[1] *= a;
				normal[2] *= a;
			}

			dot = -(p1[0] * normal[0] + p1[1] * normal[1] + p1[2] * normal[2]);
		}

		boolean isOutside(float[] p) {
			float dist = p[0] * normal[0] + p[1] * normal[1] + p[2] * normal[2]
					+ dot;
			return dist < 0;
		}
	}

	/** private constructor -> just a static class */
	private GLESCamera() {
	}

	// Viewport of OpenGL Viewport
	public static int glViewportWidth;
	public static int glViewportHeight;
	
	public static float zFar;
	public static float zNear; // TODO FIXME XXX this has to be in this class, not RealityCamera

	public static float[] projectionMatrix;
	// Store the view matrix. This matrix transforms world space to eye space;
	// it positions things relative to our eye.
	public static float[] viewMatrix;

	public static int[] viewPortMatrix;

	public static float[] cameraPosition = new float[] { 0.f, 1.6f, 0.f };

	private final static float[][] planePoints = new float[8][3];

	// TODO FIXME XXX clipSpace needs to be setted with real frustum coordinates
	private final static float[][] clipSpace = new float[][] {
			new float[] { 0, 0, 0 }, new float[] { 1, 0, 0 },
			new float[] { 1, 1, 0 }, new float[] { 0, 1, 0 },
			new float[] { 0, 0, 1 }, new float[] { 1, 0, 1 },
			new float[] { 1, 1, 1 }, new float[] { 0, 1, 1 }, };
	private final static GeometryPlane[] frustumPlanes = new GeometryPlane[6];

	static {
		for (int i = 0; i < 8; i++) {
			planePoints[i] = new float[3];
		}
		for (int i = 0; i < 6; i++) {
			frustumPlanes[i] = new GeometryPlane();
		}
	}

	public static void createViewMatrix() {
		// calculate the viewMatrix for OpenGL rendering
		float[] newViewMatrix = new float[16];
		Matrix.setIdentityM(newViewMatrix, 0);
		gluLookAt(newViewMatrix, cameraPosition[0], cameraPosition[1],
				cameraPosition[2], // camera position
				0.0f, cameraPosition[1], -5.0f, // look at
				0.0f, 1.0f, 0.0f); // up-vektor
		// Matrix.setLookAtM(newViewMatrix, 0, 0.0f, 0.0f, 0.0f, // camera
		// position
		// 0.0f, 0.0f, -5.0f, // look at
		// 0.0f, 1.0f, 0.0f); // up-vektor
		viewMatrix = newViewMatrix;
	}

	public static boolean pointInFrustum(float[] p) {
		for (int i = 0; i < frustumPlanes.length; i++) {
			if (!frustumPlanes[i].isOutside(p))
				return false;
		}
		return true;
	}

	public static void updateFrustum(float[] projectionMatrix,
			float[] viewMatrix) {
		float[] projectionViewMatrix = new float[16];
		float[] invertPVMatrix = new float[16];
		Matrix.multiplyMM(projectionViewMatrix, 0, projectionMatrix, 0,
				viewMatrix, 0);
		Matrix.invertM(invertPVMatrix, 0, projectionViewMatrix, 0);

		for (int i = 0; i < 8; i++) {
			float[] point = Arrays.copyOf(clipSpace[i], 3);

			float rw = point[0] * invertPVMatrix[3] + point[1]
					* invertPVMatrix[7] + point[2] * invertPVMatrix[11]
					+ invertPVMatrix[15];

			planePoints[i] = clipSpace[i];

			float[] newPlanePoints = new float[3];
			newPlanePoints[0] = (point[0] * invertPVMatrix[0] + point[1]
					* invertPVMatrix[4] + point[2] * invertPVMatrix[8] + invertPVMatrix[12])
					/ rw;
			newPlanePoints[1] = (point[0] * invertPVMatrix[1] + point[1]
					* invertPVMatrix[5] + point[2] * invertPVMatrix[9] + invertPVMatrix[13])
					/ rw;
			newPlanePoints[2] = (point[0] * invertPVMatrix[2] + point[1]
					* invertPVMatrix[6] + point[2] * invertPVMatrix[10] + invertPVMatrix[14])
					/ rw;
			planePoints[i] = newPlanePoints;
		}

		frustumPlanes[0].set(planePoints[1], planePoints[0], planePoints[2]);
		frustumPlanes[1].set(planePoints[4], planePoints[5], planePoints[7]);
		frustumPlanes[2].set(planePoints[0], planePoints[4], planePoints[3]);
		frustumPlanes[3].set(planePoints[5], planePoints[1], planePoints[6]);
		frustumPlanes[4].set(planePoints[2], planePoints[3], planePoints[6]);
		frustumPlanes[5].set(planePoints[4], planePoints[0], planePoints[1]);
	}
	
	public static boolean frustumCulling(float[] positionVec){
		float z = -positionVec[2];
		if(z > RealityCamera.zFar || z < RealityCamera.zNear)
			return false;
		return true;
//		float h = z * 2 * Math.tan(RealityCamera.)
	}
	
	
	public static void gluLookAt(float[] m, float eyeX, float eyeY, float eyeZ,
			float centerX, float centerY, float centerZ, float upX, float upY,
			float upZ) {

		// See the OpenGL GLUT documentation for gluLookAt for a description
		// of the algorithm. We implement it in a straightforward way:

		float fx = centerX - eyeX;
		float fy = centerY - eyeY;
		float fz = centerZ - eyeZ;

		// Normalize f
		float rlf = 1.0f / Matrix.length(fx, fy, fz);
		fx *= rlf;
		fy *= rlf;
		fz *= rlf;

		// compute s = f x up (x means "cross product")
		float sx = fy * upZ - fz * upY;
		float sy = fz * upX - fx * upZ;
		float sz = fx * upY - fy * upX;

		// and normalize s
		float rls = 1.0f / Matrix.length(sx, sy, sz);
		sx *= rls;
		sy *= rls;
		sz *= rls;

		// compute u = s x f
		float ux = sy * fz - sz * fy;
		float uy = sz * fx - sx * fz;
		float uz = sx * fy - sy * fx;

		m[0] = sx;
		m[1] = ux;
		m[2] = -fx;
		m[3] = 0.0f;

		m[4] = sy;
		m[5] = uy;
		m[6] = -fy;
		m[7] = 0.0f;

		m[8] = sz;
		m[9] = uz;
		m[10] = -fz;
		m[11] = 0.0f;

		m[12] = 0.0f;
		m[13] = 0.0f;
		m[14] = 0.0f;
		m[15] = 1.0f;

		// Matrix.m
		// gl.glMultMatrixf(m, 0);
		// gl.glTranslatef(-eyeX, -eyeY, -eyeZ);
	}

	public static void createProjectionMatrix(int width, int height) {
		glViewportHeight = height;
		glViewportWidth = width;
		viewPortMatrix = new int[] { 0, 0, width, height };

		if (RealityCamera.cameraViewportHeight == 0
				|| RealityCamera.cameraViewportWidth == 0) {
			// Set camera viewport if none exists
			RealityCamera.setViewportSize(width, height);
		}

		float[] newProjMatrix = new float[16];

		// final float ratio = (float) width / height;
		// float top = RealityCamera.zNear
		// * (float) Math.tan(RealityCamera.fovY * (Math.PI / 360.0));
		// float bottom = -top;
		// float left = bottom * ratio;
		// float right = top * ratio;
		//
		// Matrix.frustumM(newProjMatrix, 0, left, right, bottom, top,
		// RealityCamera.zNear, RealityCamera.zFar);
		// projectionMatrix = newProjMatrix;

		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = RealityCamera.zNear;
		final float far = RealityCamera.zFar;

		Matrix.frustumM(newProjMatrix, 0, left, right, bottom, top, near, far);

		projectionMatrix = newProjMatrix;
		updateFrustum(newProjMatrix, viewMatrix);
	}

	public static void createProjectionMatrix(GL10 gl, int width, int height) {
		glViewportHeight = height;
		glViewportWidth = width;
		viewPortMatrix = new int[] { 0, 0, width, height };

		if (RealityCamera.cameraViewportHeight == 0
				|| RealityCamera.cameraViewportWidth == 0) {
			// Set camera viewport if none exists
			RealityCamera.setViewportSize(width, height);
		}

		// TODO own perspective matrix multiplication
		float[] newProjMatrix = new float[16];
		// perspectiveMatrix(newProjMatrix, 0, RealityCamera.fovY,
		// RealityCamera.aspect, RealityCamera.zNear,
		// RealityCamera.zFar);
		// projectionMatrix = newProjMatrix;

		// buildOpenGLProjectionByIntrinsics(newProjMatrix, 500, 500, 0, 240,
		// 180, width, height, RealityCamera.zNear,
		// RealityCamera.zFar);

		// TODO this worked but has to be optimized
		// openGLProjectionByIntrinsicCameraParamters(newProjMatrix, 500, 500,
		// height/2, width/2, height, width, RealityCamera.zNear,
		// RealityCamera.zFar);
		// projectionMatrix = newProjMatrix;

		final float ratio = (float) width / height;
		// final float left = -ratio;
		// final float right = ratio;
		// final float bottom = -1.0f;
		// final float top = 1.0f;
		// final float near = RealityCamera.zNear;
		// final float far = RealityCamera.zFar;
		//
		// Matrix.frustumM(newProjMatrix, 0, left, right, bottom, top, near,
		// far);
		//
		// projectionMatrix = newProjMatrix;

		perspectiveMatrix(newProjMatrix, width / height, RealityCamera.fovY,
				ratio, RealityCamera.zNear, RealityCamera.zFar);

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
	private static void perspectiveMatrix(float[] m, int offset, float fovy,
			float aspect, float zNear, float zFar) {

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
	private static void buildOpenGLProjectionByIntrinsics(float[] frustum,
			float alpha, float beta, float skew, float u0, float v0,
			int imgWidth, int imgHeight, float nearClip, float farClip) {
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

		final float[] ortho = new float[] { 2.0f / (R - L), 0, 0,
				-(R + L) / (R - L), 0, 2.0f / (T - B), 0, -(T + B) / (T - B),
				0, 0, -2.0f / (F - N), -(F + N) / (F - N), 0, 0, 0, 1.0f };

		final float[] proj = new float[] { alpha, skew, u0, 0, 0, beta, v0, 0,
				0, 0, -(N + F), -N * F, 0, 0, 1.0f, 0 };

		Matrix.multiplyMM(frustum, 0, ortho, 0, proj, 0);
	}

	/**
	 * Works fine These Function builds the OpenGL Projection Matrix with the
	 * intrinsic camera parameters. The OpenGL Camera model is quite different
	 * from the intrinsic paramters of a camera.
	 * 
	 * Math can be found at:
	 * http://www.cl.cam.ac.uk/techreports/UCAM-CL-TR-634.pdf
	 * 
	 * @param frustum
	 *            stores the resulting paramters
	 * @param fx
	 *            focal length in x-direction, from camera intrinsics
	 * @param fy
	 *            focal length in y-direction, from camera intrinsics
	 * @param cx
	 *            image origin translation in x direction, from camera
	 *            intrinsics
	 * @param cy
	 *            image origin translation in y direction, from camera
	 *            intrinsics
	 * @param imgWidth
	 *            image width, in pixels
	 * @param imgHeight
	 *            image height, in pixels
	 * @param nearClip
	 *            near clipping plane z-location,
	 * @param farClip
	 *            far clipping plane z-location
	 */
	private static void openGLProjectionByIntrinsicCameraParamters(
			float[] frustum, float fx, float fy, float cx, float cy,
			float imgWidth, float imgHeight, float nearClip, float farClip) {

		final float[] yea = new float[] { 2 * fx / imgWidth, 0,
				(2 * cx / imgWidth) - 1, 0, 0, 2 * fy / imgHeight,
				(2 * cy / imgHeight) - 1, 0, 0, 0,
				-(farClip + nearClip) / (farClip - nearClip),
				-2 * farClip * nearClip / (farClip - nearClip), 0, 0, -1, 0 };

		System.arraycopy(yea, 0, frustum, 0, 16);
	}
}