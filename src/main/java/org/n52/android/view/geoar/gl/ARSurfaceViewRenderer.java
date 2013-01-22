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

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.n52.android.GeoARApplication;
import org.n52.android.R;
import org.n52.android.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.android.utils.GeoLocation;
import org.n52.android.view.geoar.ARFragment2;
import org.n52.android.view.geoar.ARFragment2.ARViewComponent;
import org.n52.android.view.geoar.gl.mode.FeatureShader;
import org.n52.android.view.geoar.gl.mode.RenderFeature2;
import org.n52.android.view.geoar.gl.mode.features.CubeFeature2;
import org.n52.android.view.geoar.gl.mode.features.GridFeature;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

/**
 *  
 * @author Arne de Wall
 * 
 */
public class ARSurfaceViewRenderer implements GLSurfaceView.Renderer,
		CameraUpdateListener, ARViewComponent {

	/**
	 * Interface for the Methods which are called inside the OpenGL specific
	 * thread.
	 */
	public interface OpenGLCallable {
		void onPreRender();

		void onRender(final float[] projectionMatrix, final float[] viewMatrix,
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

	protected Object updateLock = new Object();
	protected final Context mContext;

	private List<DataSourceVisualizationHandler> visualizationHandler = new ArrayList<DataSourceVisualizationHandler>();

	private List<RenderFeature2> renderFeatures = new ArrayList<RenderFeature2>();

	private RenderFeature2 renderFeature;
	public static RenderFeature2 test;
	
	
	/** light paramters */	
	private final float[] lightDirection = new float[] { 3.0f, 10.0f,
			2.0f, 1.0f };
	
	private final float[] lightDirectionMVP = new float[4];

	public ARSurfaceViewRenderer(Context context,
			IRotationMatrixProvider rotationMatrixProvider) {
		this.mContext = context;
		this.mRotationProvider = rotationMatrixProvider;
		ARFragment2.addARViewComponent(this);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		/** clear color buffer and depth buffer iff activated */
		int clearMask = GLES20.GL_COLOR_BUFFER_BIT;
		if (true) { // enableDepthMask = true
			clearMask |= GLES20.GL_DEPTH_BUFFER_BIT;
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			GLES20.glDepthFunc(GLES20.GL_LESS);
			GLES20.glDepthMask(true);
			GLES20.glClearDepthf(1.f);
		}
		GLES20.glClear(clearMask);

		/** extrinsic camera parameters for matching camera- with virtual view */
		float[] rotationMatrix = mRotationProvider.getRotationMatrix();
		
		/** calculate the light position in eye space */
		Matrix.multiplyMV(lightDirectionMVP, 0, rotationMatrix, 0, lightDirection, 0);

		/** render grid */
		renderFeature.onRender(GLESCamera.projectionMatrix,
				GLESCamera.viewMatrix, rotationMatrix, lightDirection);

		/** render DataSources data */
//		synchronized (visualizationHandler) {
			for (DataSourceVisualizationHandler handler : visualizationHandler) {
				for (ARObject feature : handler.getARObjects()) {
					feature.onRender(GLESCamera.projectionMatrix,
							GLESCamera.viewMatrix, rotationMatrix, lightDirection);
				}
			}
//		}
		/** for testing purposes */
		for (RenderFeature2 r : renderFeatures) {
			r.onRender(GLESCamera.projectionMatrix, GLESCamera.viewMatrix,
					rotationMatrix, lightDirection);
		}
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		// GLESCamera.createProjectionMatrix(gl, width, height);
		GLESCamera.createProjectionMatrix(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		/** Set the background clear color to "black" and transparent */
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		/** set up the view matrix */
		GLESCamera.createViewMatrix();

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

		FeatureShader.resetShaders();
		initScene();
	}

	private void initScene() {
		renderFeature = new GridFeature();
		renderFeature.onCreateInGLESThread();
		test = new CubeFeature2();
		test.setTexture(			// Read in the resource
				BitmapFactory.decodeResource(GeoARApplication.applicationContext.getResources(), R.drawable.n52_logo_highreso));
		test.setRelativePosition(new float[] { 0, 0, -4 });
		test.onCreateInGLESThread();
		renderFeatures.add(test);

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
		// resetProjection = true;

	}

	public void setCenter(Location location) {
		synchronized (visualizationHandler) {
			for (DataSourceVisualizationHandler handler : visualizationHandler)
				handler.setCenter(new GeoLocation(location.getLatitude(),
						location.getLongitude()));
		}
	}

	/**
	 * Ask renderer to reload its interpolation
	 */
	public void reload() {
		// if (currentCenterGPoint != null)
		// for (DataSourceVisualizationHandler handler : visualizationHandler)
		// handler.setCenter(currentCenterGPoint);
	}

	@Override
	public void onVisualizationHandlerAdded(
			DataSourceVisualizationHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVisualizationHandlerRef(
			List<DataSourceVisualizationHandler> handlers) {
		this.visualizationHandler = handlers;
	}

}