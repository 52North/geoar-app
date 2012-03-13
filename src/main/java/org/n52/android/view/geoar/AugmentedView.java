/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */
package org.n52.android.view.geoar;

import org.n52.android.NoiseARView;
import org.n52.android.alg.NoiseView.NoiseGridValueProvider;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.tracking.camera.RealityCamera;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.android.tracking.location.LowPassSensorBuffer;
import org.n52.android.tracking.location.SensorBuffer;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.gl.GLESAugmentedRenderer;
import org.n52.android.view.geoar.gl.GLESAugmentedRenderer.IRotationMatrixProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;


/**
 * View to show virtual information based on the camera's settings. It also
 * performs sensor based tracking
 * 
 * @author Holger Hopmann
 */
public class AugmentedView extends GLSurfaceView implements SensorEventListener,
		OnLocationUpdateListener, NoiseARView, IRotationMatrixProvider {

	/**
	 * Dialog to allow users to choose overlays. Nested class makes modification
	 * of renderer easier and it is only used here.
	 * 
	 * @author Holger Hopmann
	 */
	private class AROverlayDialog extends AlertDialog implements
			OnCheckedChangeListener {

		private ToggleButton buttonOverlayNoise;

		public AROverlayDialog() {
			super(AugmentedView.this.getContext());
			// Inflate Layout
			View layout = LayoutInflater.from(getContext()).inflate(
					R.layout.ar_overlay_dialog, null);

			// Find Button Views
			ToggleButton buttonCalibration = (ToggleButton) layout
					.findViewById(R.id.toggleButtonOverlayCalib);
			buttonCalibration.setChecked(renderer.showsCalibration());

			buttonOverlayNoise = (ToggleButton) layout
					.findViewById(R.id.toggleButtonOverlayNoise);
			buttonOverlayNoise.setChecked(renderer.showsInterpolation());

			// buttonOverlayNoiseGrid = (ToggleButton) layout
			// .findViewById(R.id.toggleButtonOverlayNoiseGrid);
			// buttonOverlayNoiseGrid
			// .setChecked(renderer.showsInterpolationGrid());

			// Bind Check Listeners
			buttonCalibration.setOnCheckedChangeListener(this);
			buttonOverlayNoise.setOnCheckedChangeListener(this);
			// buttonOverlayNoiseGrid.setOnCheckedChangeListener(this);

			// Set Dialog Options
			setView(layout);
			setCancelable(true);
			setTitle(R.string.choose_overlay);
			setButton(BUTTON_NEUTRAL, getContext().getString(R.string.ok),
					(Message) null);
		}

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// Change renderer state based on user check input
			switch (buttonView.getId()) {
			case R.id.toggleButtonOverlayCalib:
				renderer.showCalibration(isChecked);
				break;
			case R.id.toggleButtonOverlayNoise:
				renderer.showInterpolation(isChecked);
				// if (isChecked) {
				// buttonOverlayNoiseGrid.setChecked(false);
				// }
				break;
			// case R.id.toggleButtonOverlayNoiseGrid:
			// renderer.showInterpolationGrid(isChecked);
			// if (isChecked) {
			// buttonOverlayNoise.setChecked(false);
			// }
			// break;
			}
		}
	}

	// Sensor related
	private SensorBuffer magnetValues = new LowPassSensorBuffer(3, 0.3f);
	private SensorBuffer accelValues = new LowPassSensorBuffer(3, 0.5f);
	private Sensor magnet, accel;
	private SensorManager mSensorManager;
	private Display display;

	// Arrays to maintain transformation matrices
	private float[] rotMatrixSensor = new float[16];
	private float[] rotMatrix = new float[16];

	private GLESAugmentedRenderer renderer;
	private InfoView infoHandler;
	private LocationHandler locationHandler;
	private boolean updateMagneticVector = true;
	private boolean sensorValuesChanged;


	public AugmentedView(Context context) {
		super(context);
		init();
	}

	public AugmentedView(Context context, AttributeSet attrs) {
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

		renderer = new GLESAugmentedRenderer(getContext(), this);
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
	public NoiseGridValueProvider getNoiseGridValueProvider() {
//		return renderer;
		return null;
	}

	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
//		renderer.setInfoHandler(infoHandler);
	}

	public void setMeasureManager(MeasurementManager measureManager) {
		renderer.setMeasureManager(measureManager);
	}

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
		if (locationHandler != null) {
			locationHandler.removeLocationUpdateListener(this);
		}
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
		if (locationHandler != null) {
			locationHandler.addLocationUpdateListener(this);
		}
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

	public void setLocationHandler(LocationHandler locationHandler) {
		this.locationHandler = locationHandler;
		locationHandler.addLocationUpdateListener(this);
	}

////

	public void onLocationChanged(Location location) {
		
		renderer.setCenter(location);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.item_select_ar_overlay) {
			new AROverlayDialog().show();
			return true;
		} else if (item.getItemId() == R.id.item_reload_ar) {
			renderer.reload();
			return true;
		} else if (item.getItemId() == R.id.item_calibrate) {
			renderer.showCalibration(true);
		}
		return false;
	}

	public Integer getMenuGroupId() {
		return R.id.group_noiseview;
	}

	public boolean isVisible() {
		return isShown();
	}

	public boolean showsInterpolation() {
		return renderer.showsInterpolation();
	}

	public boolean showsCalibration() {
		return renderer.showsCalibration();
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("ARNoiseViewCalibration", showsCalibration());
		outState.putBoolean("ARNoiseViewInterpolation", showsInterpolation());
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		renderer.showCalibration(savedInstanceState.getBoolean(
				"ARNoiseViewCalibration", false));
		renderer.showInterpolation(savedInstanceState.getBoolean(
				"ARNoiseViewInterpolation", true));
	}

}
