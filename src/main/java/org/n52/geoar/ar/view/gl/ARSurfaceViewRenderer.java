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
package org.n52.geoar.ar.view.gl;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.n52.geoar.ar.view.ARObject;
import org.n52.geoar.tracking.camera.RealityCamera;
import org.n52.geoar.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.geoar.view.geoar.gl.mode.FeatureShader;
import org.n52.geoar.view.geoar.gl.mode.RenderFeature2;
import org.n52.geoar.view.geoar.gl.mode.Texture;
import org.n52.geoar.view.geoar.gl.mode.features.NewGridFeature;

import android.location.Location;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class ARSurfaceViewRenderer implements GLSurfaceView.Renderer,
		CameraUpdateListener {

	/**
	 * Interface for the Methods which are called inside the OpenGL specific
	 * thread.
	 */
	public interface OpenGLCallable {
		void onPreRender(); // unused

		void render(final float[] projectionMatrix, final float[] viewMatrix,
				final float[] parentMatrix, final float[] lightPosition);
	}

	public interface OnInitializeInGLThread {
		void onCreateInGLESThread();
	}

	/**
	 * Public interface to create an object which supplies the renderer with the
	 * current extrinsic camera rotation. Ensures that matrices are just
	 * computed as needed, since the renderer asks for data and does not listen
	 * for them.
	 */
	public interface IRotationMatrixProvider {
		/**
		 * Returns the current extrinsic rotation matrix
		 * 
		 * @return row major 4x4 matrix
		 */
		float[] getRotationMatrix();
	}

	private final IRotationMatrixProvider mRotationProvider;

	// private List<RenderFeature2> renderFeatures = new
	// ArrayList<RenderFeature2>();

	private RenderFeature2 grid;
	// private RenderFeature2 test;

	/** light parameters */
	private final float[] lightDirection = new float[] { 3.0f, 10.0f, 2.0f,
			1.0f };

	private final float[] lightDirectionMVP = new float[4];

	private final float[] mvMatrix = new float[16];

	private boolean mARObjectsChanged;

	private ARSurfaceView mSurfaceView;

	// Currently always a copy of ARView's ARObjects
	private List<ARObject> mARObjects = new ArrayList<ARObject>();

	// FlatCircleFeature circleFeature;

	// TriangleFeature triangle;

	private boolean mLocationChanged;

	private Location mUserLocation;

	private boolean mProjectionChanged;

	public ARSurfaceViewRenderer(ARSurfaceView surfaceView,
			IRotationMatrixProvider rotationMatrixProvider) {
		this.mSurfaceView = surfaceView;
		mUserLocation = mSurfaceView.getUserLocation();
		this.mRotationProvider = rotationMatrixProvider;
		mLocationChanged = true;
		mARObjectsChanged = true;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		if (mProjectionChanged) {
			// Camera may get initialized after creating the GL surface,
			// projection matrix will be reset here if camera settings changed
			GLESCamera.resetProjectionMatrix();

			GLESCamera.resetViewMatrix();
			mProjectionChanged = false;
		}

		// Update ARObjects if required
		if (mARObjectsChanged) {
			// Copy list of ARObjects to avoid the need for synchronization
			mARObjects.clear();
			mARObjects.addAll(mSurfaceView.getARObjects());
			for (ARObject feature : mARObjects) {
				feature.initializeRendering();
			}
			mARObjectsChanged = false;
		}
		if (mARObjects == null) {
			// Not all information available
			return;
		}

		// Update location information if required
		if (mLocationChanged) {
			mUserLocation = mSurfaceView.getUserLocation();
			// TODO evaluate synchronized vs. copying list; list is reused by
			// mSurfaceView for because of performance reasons

			for (ARObject feature : mARObjects) {
				feature.onLocationUpdate(mUserLocation);
			}

			mLocationChanged = false;
		}
		if (mUserLocation == null) {
			// Not all information available
			return;
		}

		/** clear color buffer and depth buffer */
		// GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		// GLES20.glDepthFunc(GLES20.GL_LESS);
		// GLES20.glDepthMask(true);
		// GLES20.glClearDepthf(1.f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		/** extrinsic camera parameters for matching camera- with virtual view */
		System.arraycopy(mRotationProvider.getRotationMatrix(), 0, mvMatrix, 0,
				16);

		/** calculate the light position in eye space */
		Matrix.multiplyMV(lightDirectionMVP, 0, mvMatrix, 0, lightDirection, 0);

		/** render grid */
		// triangle.render(GLESCamera.projectionMatrix, GLESCamera.viewMatrix,
		// mvMatrix, lightDirection);
		grid.render(GLESCamera.projectionMatrix, GLESCamera.viewMatrix,
				mvMatrix, lightDirection);

		// circleFeature.render(GLESCamera.projectionMatrix,
		// GLESCamera.viewMatrix, mvMatrix, lightDirection);

		/** render DataSources data */
		for (ARObject feature : mARObjects) {
			feature.render(GLESCamera.projectionMatrix, GLESCamera.viewMatrix,
					mvMatrix, lightDirection);
		}

		// /** for testing purposes */
		// for (RenderFeature2 r : renderFeatures) {
		// r.render(GLESCamera.projectionMatrix, GLESCamera.viewMatrix,
		// mvMatrix, lightDirection);
		// }
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		if (!RealityCamera.hasViewportSize()) {
			// Set camera viewport if none exists
			RealityCamera.setViewportSize(width, height);
			// RealityCamera.setAspect((float) width / (float) height);
		}
		GLESCamera.resetViewportMatrix(width, height);
		GLESCamera.resetProjectionMatrix();
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		/** Set the background clear color to "black" and transparent */
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		/** set up the view matrix */
		GLESCamera.resetViewMatrix();

		/** Enable depth testing */
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glClearDepthf(1.0f);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glDepthMask(true);

		/** Enable texture mapping */
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);

		/** Enable blending */
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		// GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

		/**
		 * Backface culling - here back-facing facets are culled when facet
		 * culling is enabled
		 */
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK); // GL_FRONT_AND_BACK for no facets

		// Resets all cached handlers because the context was lost so that they
		// are recreated later on demand.
		FeatureShader.resetShaders();
		Texture.resetTextures();
		initScene();
	}

	private void initScene() {
		grid = new NewGridFeature();
		grid.onCreateInGLESThread();
		// test = new CubeFeature2();
		// test.setTextureCallback(new Callable<Bitmap>() {
		// @Override
		// public Bitmap call() throws Exception {
		// return BitmapFactory.decodeResource(
		// GeoARApplication.applicationContext.getResources(),
		// R.drawable.n52_logo_highreso);
		// }
		// });
		// test.setRelativePosition(new float[] { 0, 0, -6 });
		// test.onCreateInGLESThread();
		// circleFeature = new FlatCircleFeature();
		// circleFeature.setRelativePosition(new float[] { 2, 0, -6 });
		// circleFeature.onCreateInGLESThread();
		// test.setSubVisualization(circleFeature);

		// triangle = new TriangleFeature();
		// triangle.setRelativePosition(new float[] {-2, 0, -6});
		// triangle.onCreateInGLESThread();

		// test.setSubVisualization(triangle);

		// renderFeatures.add(test);

		// // first cube
		// RenderFeature2 first = new CubeFeature2();
		// first.setPosition(new float[] { 1, 0, 5 });
		// first.onCreateInGLESThread();
		// renderFeatures.add(first);
		//
		// RenderFeature2 sec = new CubeFeature2();
		// sec.setPosition(new float[] { 5, 0, 5 });
		// sec.onCreateInGLESThread();
		// renderFeatures.add(sec);
		//
		// RenderFeature2 third = new CubeFeature2();
		// third.setPosition(new float[] { 2.5f, 0, 5 });
		// third.onCreateInGLESThread();
		// renderFeatures.add(third);
	}

	@Override
	public void onCameraUpdate() {
		mProjectionChanged = true;
	}

	/**
	 * Ask renderer to reload
	 */
	public void reload() {
		mARObjectsChanged = true;
	}

	/**
	 * Notification for this {@link Renderer} that the objects to display were
	 * changed. The renderer will schedule an update of the features to draw for
	 * the next draw cycle.
	 */
	public void notifyARObjectsChanged() {
		mARObjectsChanged = true;
	}

	/**
	 * Notification for this {@link Renderer} that the user location changed.
	 * The renderer will schedule an update of the relative positions of its
	 * displayed features for the next draw cycle.
	 */
	public void notifyLocationChanged() {
		mLocationChanged = true;
	}

}