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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceLoader;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.gl.model.RenderNode;
import org.n52.android.view.geoar.gl.model.primitives.Cube;
import org.n52.android.view.geoar.gl.model.primitives.HeightMap;
import org.n52.android.view.geoar.gl.model.rendering.ReferencedHeightMap;
import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.location.Location;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

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
		void onPreRender();

		void onRender(float[] projectionMatrix, float[] viewMatrix,
				final float[] parentMatrix);

		void onCreateInGLESThread();
	}

	// public interface OnObservationUpdateListener {
	// public void onObservationUpdate(List<? extends SpatialEntity> m);
	// }
	//
	// public interface GeoLocationUpdateListener {
	// /**
	// * Updates the relative location of the Renderable according to the
	// * location device
	// *
	// * @param locationUpdate
	// * current location of the device.
	// */
	// public void onGeoLocationUpdate(GeoPoint g);
	// }

	/**
	 * Public interface to create an object which can supplies the renderer with
	 * the current extrinsic camera rotation. Ensures that matrices are just
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

	// protected class UpdateHolder implements Runnable {
	// protected boolean canceled;
	// protected MercatorRect bounds;
	// protected RequestHolder requestHolder;
	//
	// protected GetDataBoundsCallback callback = new GetDataBoundsCallback() {
	//
	// @Override
	// public void onProgressUpdate(int progress, int maxProgress, int step) {
	// if (mInfoHandler != null) {
	// String stepTitle = "";
	// // switch (step) {
	// // // case NoiseInterpolation.STEP_CLUSTERING:
	// // // stepTitle = mContext.getString(R.string.clustering);
	// // // break;
	// // // case NoiseInterpolation.STEP_INTERPOLATION:
	// // // stepTitle =
	// // mContext.getString(R.string.interpolation);
	// // // break;
	// // case DataCache.STEP_REQUEST:
	// // stepTitle = mContext
	// // .getString(R.string.measurement_request);
	// // break;
	// // }
	//
	// mInfoHandler.setProgressTitle(stepTitle,
	// ARSurfaceViewRenderer.this);
	// mInfoHandler.setProgress(progress, maxProgress,
	// ARSurfaceViewRenderer.this);
	// }
	// }
	//
	// @Override
	// public void onAbort(MercatorRect bounds, int reason) {
	// if (mInfoHandler != null) {
	// // mInfoHandler.clearProgress(ARSurfaceViewRenderer.this);
	// // if (reason == DataCache.ABORT_NO_CONNECTION) {
	// // mInfoHandler.setStatus(R.string.connection_error, 5000,
	// // ARSurfaceViewRenderer.this);
	// // } else if (reason == DataCache.ABORT_UNKOWN) {
	// // mInfoHandler.setStatus(R.string.unkown_error, 5000,
	// // ARSurfaceViewRenderer.this);
	// // }
	// }
	// }
	//
	// @Override
	// public void onReceiveDataUpdate(MercatorRect bounds,
	// List<? extends SpatialEntity> data) {
	// synchronized (up) {
	//
	// }
	// // Save result reference in variable. Those should always be the
	// // same
	// currentInterpolationRect = bounds;
	// currentMeasurement = data;
	// if (currentCenterGPoint == null)
	// return;
	//
	// // for (OnObservationUpdateListener r : observationUpdateListener)
	// // r.onObservationUpdate(currentMeasurement);
	// //
	// // for (GeoLocationUpdateListener lu : geoLocationUpdateListener)
	// // lu.onGeoLocationUpdate(currentCenterGPoint);
	//
	// // TODO needed for Interpolation
	// // glInterpolation.setWidth(bounds.width());
	// // glInterpolation.setHeight(bounds.height());
	// // Ask the corresponding texture to reload its data on next draw
	// // interpolationTexture.reload();
	// }
	//
	//
	// };
	//
	//
	// @Override
	// public void run() {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// }

	// TODO what do we need
	private MercatorRect currentInterpolationRect;
	private GeoPoint currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
	// current resolution to calculate distances in meter
	private float currentGroundResolution;

	// private List<OnObservationUpdateListener> observationUpdateListener;
	// private List<GeoLocationUpdateListener> geoLocationUpdateListener;
	private Context mContext;

	private final IRotationMatrixProvider mRotationProvider;

	private InfoView mInfoHandler;
	protected Object updateLock = new Object();

	private Stack<RenderNode> children;
	private int numChildren;
	private GLSurfaceView glSurfaceView;

	private boolean enableDepthBuffer;

	private ARVisualizationFactory factory;
	private List<DataSourceVisualizationHandler> visualizationHandler = new ArrayList<DataSourceVisualizationHandler>();

	private OnCheckedChangedListener<DataSourceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceHolder item, boolean newState) {
			if (newState = true) {
				DataSourceVisualizationHandler handler = new DataSourceVisualizationHandler(
						ARSurfaceViewRenderer.this.glSurfaceView, item, factory);
				visualizationHandler.add(handler);
			}
			for (Iterator<DataSourceVisualizationHandler> it = visualizationHandler
					.iterator(); it.hasNext();) {
				DataSourceVisualizationHandler current = it.next();
				if (current.getDataSourceHolder() == item) {
					current.clear();
					it.remove();
					break;
				}
			}
		}
	};

	public ARSurfaceViewRenderer(Context context,
			final GLSurfaceView glSurfaceView) {
		this.mContext = context;
		this.mRotationProvider = (IRotationMatrixProvider) glSurfaceView;
		this.glSurfaceView = glSurfaceView;

		// this.observationUpdateListener = new
		// ArrayList<OnObservationUpdateListener>();
		children = new Stack<RenderNode>();

		// this.geoLocationUpdateListener = new
		// ArrayList<ARSurfaceViewRenderer.GeoLocationUpdateListener>();
		DataSourceLoader.getSelectedDataSources()
				.addOnCheckedChangeListener(dataSourceListener);

		factory = new ARVisualizationFactory(glSurfaceView);
	}

	public void setInfoHandler(InfoView infoHandler) {
		this.mInfoHandler = infoHandler;
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		// GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		// GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT |
		// GLES20.GL_COLOR_BUFFER_BIT);
		int clearMask = GLES20.GL_COLOR_BUFFER_BIT;
		if (enableDepthBuffer) {
			clearMask |= GLES20.GL_DEPTH_BUFFER_BIT;
			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
			GLES20.glDepthFunc(GLES20.GL_LESS);
			GLES20.glDepthMask(true);
			GLES20.glClearDepthf(1.f);
		}
		GLES20.glClear(clearMask);

		for (int i = 0; i < numChildren; i++) {
			children.get(i).onRender(GLESCamera.projectionMatrix,
					GLESCamera.viewMatrix,
					mRotationProvider.getRotationMatrix());
		}

	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		GLESCamera.createProjectionMatrix(width, height);
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {

		// set the background color to transparent
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// set up the view matrix
		GLESCamera.createViewMatrix();

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glClearDepthf(1.0f);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		GLES20.glDepthMask(true);

		// No culling of back faces
		GLES20.glDisable(GLES20.GL_CULL_FACE);

		// No depth testing
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		// Enable blending
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE);

		// backface culling
		// GLES20.glEnable(GLES20.GL_CULL_FACE);
		// GLES20.glCullFace(GLES20.GL_BACK);
		initScene();

	}

	private void initScene() {

		// Set<DataSourceHolder> list =
		// DataSourceLoader.getDataSources();
		// // DataSourceLoader.
		// for(DataSourceHolder holder : list){
		// ARVisualizationFactory fac = new ARVisualizationFactory();
		// }
		// Grid grid = new Grid();
		// grid.setRenderer(SimpleColorRenderer.getInstance());
		// addRenderNode(grid);

		// PointsOfInteresst poi = new
		// PointsOfInteresst(SimpleColorRenderer.getInstance(), glSurfaceView );
		// poi.onGeoLocationUpdate(currentCenterGPoint);
		// addRenderNode(poi);

		// Sphere sphere = new Sphere(1, 10, 10);
		// sphere.setPosition(new float[] {0,0,5});
		// addRenderNode(sphere);
		//
		 Cube cube = new Cube();
		 cube.setPosition(new float[] {0,5,10});
		 addRenderNode(cube);
		 this.children.add(cube);

		// HeightMap map = new HeightMap();
		// addRenderNode(map);
		//
		// ReferencedHeightMap hMap = new ReferencedHeightMap();
		// addRenderNode(hMap);

		// Cube cube = new Cube(1);
		// cube.setPosition(new float[] {0,0,5});
		// cube.setRenderer(SimpleColorRenderer.getInstance());
		// addRenderNode(cube);
		//
		// Cube cube2 = new Cube(1);
		// cube2.setPosition(new float[] {0,2,5});
		// cube2.setRenderer(SimpleColorRenderer.getInstance());
		// cube.addChild(cube2);

		// GLESItemizedRenderer p = new GLESItemizedRenderer(mRotationProvider);
		// p.onLocationUpdate(currentCenterGPoint);
		// mRenderObjects.add(p);

		// GridRenderer g = new GridRenderer(mRotationProvider);
		// mRenderObjects.add(g);
	}

	@Deprecated
	private void addRenderNode(RenderNode renderNode) {
		// if (renderNode instanceof GeoLocationUpdateListener)
		// this.geoLocationUpdateListener
		// .add((GeoLocationUpdateListener) renderNode);
		// if (renderNode instanceof OnObservationUpdateListener)
		// this.observationUpdateListener
		// .add((OnObservationUpdateListener) renderNode);
		// this.children.add(renderNode);
		// this.numChildren++;
	}

	@Override
	public void onCameraUpdate() {
		// resetProjection = true;

	}

	public void setCenter(Location location) {
		for (DataSourceVisualizationHandler handler : visualizationHandler)
			handler.setCenter(new GeoPoint(
					(int) (location.getLatitude() * 1E6), (int) (location
							.getLongitude() * 1E6)));
	}

	/**
	 * Ask renderer to reload its interpolation
	 */
	public void reload() {
		if (currentCenterGPoint != null)
			for (DataSourceVisualizationHandler handler : visualizationHandler)
				handler.setCenter(currentCenterGPoint);
	}

	public void checkPOI() {
		// if (mRenderObjects.size() == 0) {
		//
		// }
	}
}