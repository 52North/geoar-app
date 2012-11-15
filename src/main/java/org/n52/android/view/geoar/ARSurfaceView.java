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
package org.n52.android.view.geoar;

import org.n52.android.geoar.R;
import org.n52.android.tracking.camera.RealityCamera;
import org.n52.android.tracking.location.LowPassSensorBuffer;
import org.n52.android.tracking.location.SensorBuffer;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.IRotationMatrixProvider;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;

/**
 * View to show virtual information based on the camera's settings. It also
 * performs sensor based tracking
 * 
 * @author Arne de Wall
 */
public class ARSurfaceView extends GLSurfaceView implements
		SensorEventListener, IRotationMatrixProvider {

	// Sensor related
	private SensorBuffer magnetValues = new LowPassSensorBuffer(3, 0.05f);
	private SensorBuffer accelValues = new LowPassSensorBuffer(3, 0.15f);
	private Sensor magnet, accel;
	private SensorManager mSensorManager;
	private Display display;

	// Arrays to maintain transformation matrices
	private float[] rotMatrixSensor = new float[16];
	private float[] rotMatrix = new float[16];

	private ARSurfaceViewRenderer renderer;
	private InfoView infoHandler;

	private boolean updateMagneticVector = true;
	private boolean sensorValuesChanged;

	public ARSurfaceView(Context context) {
		super(context);
		init();
	}

	public ARSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
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

		renderer = new ARSurfaceViewRenderer(getContext(), this);
		setEGLContextClientVersion(2);
		setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Forces to make translucent
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
		mSensorManager.unregisterListener(this);
		// if (locationHandler != null) {
		// locationHandler.removeLocationUpdateListener(this);
		// }
		super.onPause();
	}

	@Override
	public void onResume() {
		RealityCamera.addCameraUpdateListener(renderer);
		if (!mSensorManager.registerListener(this, magnet,
				SensorManager.SENSOR_DELAY_GAME)) {
			infoHandler.setStatus(R.string.magnetic_field_not_started, 5000,
					magnet);
		}
		if (!mSensorManager.registerListener(this, accel,
				SensorManager.SENSOR_DELAY_GAME)) {
			infoHandler.setStatus(R.string.accel_not_started, 5000, accel);
		}
		// if (locationHandler != null) {
		// locationHandler.addLocationUpdateListener(this);
		// }
		super.onResume();
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		if (accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
			if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
				infoHandler.setStatus(R.string.magnetic_field_not_calibrated,
						5000, magnet);
			} else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				infoHandler.setStatus(R.string.accel_not_calibrated, 5000,
						accel);
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

	// //

	public void onLocationChanged(Location location) {
		renderer.setCenter(location);
	}

	public Integer getMenuGroupId() {
		return 0;
		// return R.id.group_noiseview;
	}

	public boolean isVisible() {
		return isShown();
	}

}
