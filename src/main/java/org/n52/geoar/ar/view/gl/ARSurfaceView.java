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

import java.util.List;

import org.n52.geoar.R;
import org.n52.geoar.ar.view.ARObject;
import org.n52.geoar.ar.view.ARView;
import org.n52.geoar.ar.view.gl.ARSurfaceViewRenderer.IRotationMatrixProvider;
import org.n52.geoar.ar.view.gl.ARSurfaceViewRenderer.OnInitializeInGLThread;
import org.n52.geoar.tracking.camera.RealityCamera;
import org.n52.geoar.tracking.location.AdaptiveLowPassSensorBuffer;
import org.n52.geoar.tracking.location.LocationHandler;
import org.n52.geoar.tracking.location.SensorBuffer;
import org.n52.geoar.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.geoar.view.InfoView;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.View.MeasureSpec;

/**
 * View to show virtual information based on the camera's settings. It also
 * performs sensor based tracking
 * 
 * @author Arne de Wall
 * @author Holger Hopmann
 */
public class ARSurfaceView extends GLSurfaceView implements
		SensorEventListener, IRotationMatrixProvider, OnLocationUpdateListener {

	// Sensor related
	private SensorBuffer magnetValues = new AdaptiveLowPassSensorBuffer(3, 1,
			10, 0.002f, 0.1f); // new LowPassSensorBuffer(3, 0.05f);
	private SensorBuffer accelValues = new AdaptiveLowPassSensorBuffer(3, 0.5f,
			4, 0.01f, 0.15f); // new LowPassSensorBuffer(3, 0.15f);
	private Sensor magnet, accel;
	private SensorManager mSensorManager;
	private Display display;

	// Arrays to maintain transformation matrices
	private float[] rotMatrixSensor = new float[16];
	private float[] rotMatrix = new float[16];

	private ARSurfaceViewRenderer renderer;

	private boolean updateMagneticVector = true;
	private boolean sensorValuesChanged;

	private Location mLastLocation;
	private Object locationInfoKey = new Object();
	private ARView mARView;

	public ARSurfaceView(ARView arView) {
		super(arView.getContext());
		mARView = arView;
		init();
	}

	private void init() {
		// System services
		mSensorManager = (SensorManager) getContext().getSystemService(
				Context.SENSOR_SERVICE);
		magnet = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		display = ((WindowManager) getContext().getSystemService(
				Context.WINDOW_SERVICE)).getDefaultDisplay();

		mLastLocation = LocationHandler.getLastKnownLocation();

		renderer = new ARSurfaceViewRenderer(this, this);
		setEGLContextClientVersion(2);
		setEGLConfigChooser(new MultisampleConfigs());
		// XXX Does this support transparent background?

		// setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Forces to make translucent
		// drawing available
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setRenderer(renderer);
	}

	/**
	 * Get a {@link NoiseGridValueProvider} to access the raw noise
	 * interpolation data currently in use
	 * 
	 * @return
	 */
	// public NoiseGridValueProvider getNoiseGridValueProvider() {
	// // return renderer;
	// return null;
	// }

	public float[] getRotationMatrix() {
		if (sensorValuesChanged) {
			computeRotationMatrix();
			sensorValuesChanged = false;
		}
		return rotMatrix;
	}

	/**
	 * Computes the Transformation from device to world coordinates
	 */
	private void computeRotationMatrix() {
		synchronized (rotMatrix) {
			if (magnetValues.hasValues() && accelValues.hasValues()) {
				SensorManager.getRotationMatrix(rotMatrixSensor, null,
						accelValues.get(), magnetValues.get());
				// transforms from sensor to world

				switch (display.getOrientation()) {
				case Surface.ROTATION_0:
					// No adjustment
					SensorManager.remapCoordinateSystem(rotMatrixSensor,
							SensorManager.AXIS_X, SensorManager.AXIS_Y,
							rotMatrix);
					break;
				case Surface.ROTATION_90:
					SensorManager.remapCoordinateSystem(rotMatrixSensor,
							SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
							rotMatrix);
					break;
				case Surface.ROTATION_180:
					SensorManager.remapCoordinateSystem(rotMatrixSensor,
							SensorManager.AXIS_MINUS_X,
							SensorManager.AXIS_MINUS_Y, rotMatrix);
					break;
				case Surface.ROTATION_270:
					SensorManager.remapCoordinateSystem(rotMatrixSensor,
							SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
							rotMatrix);
					break;
				}
				// transforms from device to world

				Matrix.rotateM(rotMatrix, 0, 90, 1, 0, 0);
				// Account for the upward usage of this app
			}
		}
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		if (renderer != null) {
			if (isShown()) {
				onResume();
			} else {
				onPause();
			}
		}
		super.onVisibilityChanged(changedView, visibility);

	}

	@Override
	public void onPause() {
		RealityCamera.removeCameraUpdateListener(renderer);
		LocationHandler.removeLocationUpdateListener(this);
		mSensorManager.unregisterListener(this);

		InfoView.clearStatus(locationInfoKey);

		super.onPause();
	}

	@Override
	public void onResume() {
		RealityCamera.addCameraUpdateListener(renderer);
		LocationHandler.addLocationUpdateListener(this);
		if (!mSensorManager.registerListener(this, magnet,
				SensorManager.SENSOR_DELAY_GAME)) {
			InfoView.setStatus(R.string.magnetic_field_not_started, 5000,
					magnet);
		}
		if (!mSensorManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_GAME)) {
			InfoView.setStatus(R.string.accel_not_started, 5000, accel);
		}

		getUserLocation();

		super.onResume();
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
			if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				InfoView.setStatus(R.string.magnetic_field_not_calibrated,
						5000, magnet);
			} else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				InfoView.setStatus(R.string.accel_not_calibrated, 5000, accel);
			}
		}
	}

	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			if (updateMagneticVector) {
				magnetValues.put(event.values);
			}
		} else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			accelValues.put(event.values);
		}
		sensorValuesChanged = true;
	}

	public Location getUserLocation() {
		if (mLastLocation == null) {
			InfoView.setStatus(R.string.waiting_for_location_fix, -1,
					locationInfoKey);
		} else {
			InfoView.clearStatus(locationInfoKey);
		}
		return mLastLocation;
	}

	@Override
	public void onLocationChanged(Location location) {
		mLastLocation = location;
		renderer.notifyLocationChanged();
	}

	public void notifyARObjectsChanged() {
		renderer.notifyARObjectsChanged();
	}

	public List<ARObject> getARObjects() {
		return mARView.getARObjects();
	}

}
