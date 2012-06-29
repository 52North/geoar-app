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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.MeasurementManager.GetMeasurementBoundsCallback;
import org.n52.android.data.MeasurementManager.MeasurementsCallback;
import org.n52.android.data.MeasurementManager.RequestHolder;
import org.n52.android.geoar.R;
import org.n52.android.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
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
public class GLESAugmentedRenderer implements GLSurfaceView.Renderer, 
						CameraUpdateListener, Closeable {

	public interface IRenderable {
		/**
		 * Renders the current frame of the Renderable
		 */
		public void onDrawFrame();
		/**
		 * Updates the relative location of the Renderable according 
		 * to the location device
		 * 
		 * @param locationUpdate
		 * 					current location of the device.
		 */
		public void onLocationUpdate(GeoPoint locationUpdate);
		/**
		 * Updates the current measurements to render
		 * 
		 * @param m 
		 * 			new measurement callback
		 */
		public void onObservationUpdate(MeasurementsCallback m);
		public void loadResources();
		public void unloadResources();
	}
	
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
	
	private GetMeasurementBoundsCallback callback = new GetMeasurementBoundsCallback() {

		@Override
		public void onProgressUpdate(int progress, int maxProgress, int step) {
			if (mInfoHandler != null) {
				String stepTitle = "";
				switch (step) {
//				case NoiseInterpolation.STEP_CLUSTERING:
//					stepTitle = mContext.getString(R.string.clustering);
//					break;
//				case NoiseInterpolation.STEP_INTERPOLATION:
//					stepTitle = mContext.getString(R.string.interpolation);
//					break;
				case MeasurementManager.STEP_REQUEST:
					stepTitle = mContext.getString(R.string.measurement_request);
					break;
				}

				mInfoHandler.setProgressTitle(stepTitle, GLESAugmentedRenderer.this);
				mInfoHandler.setProgress(progress, maxProgress,
						GLESAugmentedRenderer.this);
			}
		}

		@Override
		public void onReceiveDataUpdate(MercatorRect bounds,
				MeasurementsCallback measureCallback) {
			// Save result reference in variable. Those should always be the same
			currentInterpolationRect = bounds;
			currentMeasurement = measureCallback;
			if(currentCenterGPoint == null)
				return;
			
			for(IRenderable r : mRenderObjects){
				r.onLocationUpdate(currentCenterGPoint);
				r.onObservationUpdate(currentMeasurement);
			}
				
			
			// TODO needed for Interpolation
//			glInterpolation.setWidth(bounds.width());
//			glInterpolation.setHeight(bounds.height());
			// Ask the corresponding texture to reload its data on next draw
//			interpolationTexture.reload();
		}

		@Override
		public void onAbort(MercatorRect bounds, int reason) {
			if (mInfoHandler != null) {
				mInfoHandler.clearProgress(GLESAugmentedRenderer.this);
				if ( reason == MeasurementManager.ABORT_NO_CONNECTION) {
					mInfoHandler.setStatus(R.string.connection_error, 5000, 
							GLESAugmentedRenderer.this);
				} else if(reason == MeasurementManager.ABORT_UNKOWN){
					mInfoHandler.setStatus(R.string.unkown_error, 5000, 
							GLESAugmentedRenderer.this);
				}
			}
		}	
	};
	
	private boolean showPointsOfInterest;
	private boolean showInterpolation;
	private boolean showCalibration;
	private boolean resetProjection;
	
	// TODO what do we need
	private MeasurementsCallback currentMeasurement;
	private MercatorRect currentInterpolationRect;
	private GeoPoint currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
	// current resolution to calculate distances in meter
	private float currentGroundResolution;
	
	private RequestHolder currentRequest;

	
	private List<IRenderable> mRenderObjects;
	private Context mContext;
	
	private final IRotationMatrixProvider mRotationProvider;

	private InfoView mInfoHandler;
	private MeasurementManager mMeasureManager;
	
	
	public GLESAugmentedRenderer(Context context, IRotationMatrixProvider rotationProvider){
		this.mContext = context;
		this.mRotationProvider = rotationProvider;
		
		mRenderObjects = new ArrayList<IRenderable>();

	}
	
	public void setInfoHandler(InfoView infoHandler){
		this.mInfoHandler = infoHandler;
	}
	
	public void setMeasureManager(MeasurementManager measureManager){
		this.mMeasureManager = measureManager;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {			
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		
		for(IRenderable r : mRenderObjects)
			r.onDrawFrame();
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
		
		// backface culling
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_BACK);
		
		GLESItemizedRenderer p = new GLESItemizedRenderer(mRotationProvider);
		p.onLocationUpdate(currentCenterGPoint);
		mRenderObjects.add(p);
	}

	@Override
	public void onCameraUpdate() {
		resetProjection = true;
		
	}
	
	public void setCenter(Location location){
		setCenter(new GeoPoint((int) (location.getLatitude() * 1E6),
				(int) (location.getLongitude() * 1E6)));
	}
	
	public void setCenter(GeoPoint gPoint){
		if(mMeasureManager == null)
			return;
		
		currentCenterGPoint = gPoint;
		
		// Calculate thresholds for request of data
		double meterPerPixel = MercatorProj.getGroundResolution(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		int pixelRadius = (int) (Settings.SIZE_AR_INTERPOLATION / meterPerPixel);
		int pixelReloadDist = (int) (Settings.RELOAD_DIST_AR / meterPerPixel);

		// Calculate new center point in world coordinates
		int centerPixelX = (int) MercatorProj.transformLonToPixelX(
				gPoint.getLongitudeE6() / 1E6f, Settings.ZOOM_AR);
		int centerPixelY = (int) MercatorProj.transformLatToPixelY(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		currentCenterMercator = new MercatorPoint(centerPixelX, centerPixelY,
				Settings.ZOOM_AR);
		

		currentGroundResolution = (float) MercatorProj.getGroundResolution(
				currentCenterGPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);

		// determination if data request is needed or if just a simple shift is
		// enough
		boolean requestInterpolation = false;
		if (currentInterpolationRect == null) {
			// Get new data if there were none before
			requestInterpolation = true;
		} else {
			MercatorPoint interpolationCenter = currentInterpolationRect
					.getCenter();
			if (currentCenterMercator.zoom != currentInterpolationRect.zoom
					|| currentCenterMercator.distanceTo(interpolationCenter) > pixelReloadDist) {
				// request data if new center offsets more than
				// Settings.RELOAD_DIST_AR meters
				requestInterpolation = true;
			}
		}

		if (requestInterpolation) {
			// if new data is needed
			if (currentRequest != null) {
				// cancel currently running data request
				currentRequest.cancel();
			}
			// trigger data request
			currentRequest = mMeasureManager.getMeasurementCallback(new MercatorRect(
					currentCenterMercator.x - pixelRadius,
					currentCenterMercator.y - pixelRadius,
					currentCenterMercator.x + pixelRadius,
					currentCenterMercator.y + pixelRadius, Settings.ZOOM_AR),
					callback, false, currentMeasurement);
		}
		
		
		for(IRenderable r : mRenderObjects) {
			r.onLocationUpdate(currentCenterGPoint);
		}
			
	}
	
	public void showCalibration(boolean show){
		showCalibration = show;
	}
	
	public void showInterpolation(boolean show){
		showInterpolation = show;
	}
	
	public boolean showsCalibration(){
		return this.showCalibration;
	}
	
	public boolean showsInterpolation(){
		return this.showInterpolation;
	}
	
	/**
	 * Ask renderer to reload its interpolation
	 */
	public void reload() {
		if (currentCenterGPoint != null) 
			setCenter(currentCenterGPoint);
	}
	
	public void checkPOI(){
		if(mRenderObjects.size() == 0){

		}
	}
}