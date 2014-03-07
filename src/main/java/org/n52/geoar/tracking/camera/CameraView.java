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
package org.n52.geoar.tracking.camera;

import java.util.List;

import org.n52.geoar.R;

import android.app.AlertDialog;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;

/**
 * A View which encapsulates a camera preview. Takes care of freeing all
 * resources as needed
 * 
 * @author Holger Hopmann
 * 
 */
public class CameraView extends SurfaceView implements Callback {

	private SurfaceHolder holder;
	private Camera camera;
	private int cameraId;
	private int cameraSizeHintWidth;
	private int cameraSizeHintHeight;

	public CameraView(Context context) {
		super(context);
		init();
	}

	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // kept for
																	// older
																	// versions
	}

	/**
	 * Methode �bernommen aus Android API Demos, siehe
	 * http://developer.android.com
	 * /resources/samples/ApiDemos/src/com/example/android
	 * /apis/graphics/CameraPreview.html
	 * 
	 * Copyright (C) 2007 The Android Open Source Project
	 * 
	 * @param sizes
	 * @param w
	 * @param h
	 * @return
	 */
	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (camera == null)
			return;
		// �nderung des Surface f�r Kameravorschau ber�cksichtigen

		// Bilddrehung
		// Systemdienstinstanz erhalten
		WindowManager windowManager = (WindowManager) getContext()
				.getSystemService(Context.WINDOW_SERVICE);

		// Rotationskonstante Surface.ROTATION_...
		int rotation = windowManager.getDefaultDisplay().getRotation();

		// According to API Documentation
		// http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation%28int%29
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		// // Annahme, dass Kamera 90� verdreht zu Standardausrichtung
		// platziert
		// // ist. Tats�chlicher Wert erst ab Android 2.3 abrufbar. TODO
		// int cameraRotation = (90 - degrees + 360) % 360;
		CameraInfo info = new CameraInfo();
		Camera.getCameraInfo(cameraId, info);

		int cameraRotation;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			cameraRotation = (info.orientation + degrees) % 360;
			cameraRotation = (360 - cameraRotation) % 360; // compensate the
															// mirror
		} else { // back-facing
			cameraRotation = (info.orientation - degrees + 360) % 360;
		}

		camera.stopPreview();	// Fix for 2.3.3 Bug?
		camera.setDisplayOrientation(cameraRotation);

		// Bildgr��e
		Camera.Parameters parameters = camera.getParameters();

		Size bestSize = getOptimalPreviewSize(
				parameters.getSupportedPreviewSizes(),
				cameraSizeHintWidth != 0 ? cameraSizeHintWidth : width,
				cameraSizeHintHeight != 0 ? cameraSizeHintHeight : height);
		parameters.setPreviewSize(bestSize.width, bestSize.height);

		// Update static camera settings fields
		if (cameraRotation == 0 || cameraRotation == 180) {
			RealityCamera.setViewportSize(bestSize);
			RealityCamera.setFovY(parameters.getVerticalViewAngle());
			// RealityCamera.setAspect(parameters.getHorizontalViewAngle()
			// / parameters.getVerticalViewAngle());
			// TODO merge updates
		} else {
			RealityCamera.setViewportSize(bestSize.height, bestSize.width);
			RealityCamera.setFovY(parameters.getHorizontalViewAngle());
			// RealityCamera.setAspect(parameters.getVerticalViewAngle()
			// / parameters.getHorizontalViewAngle());
			// TODO merge updates
		}
		camera.setParameters(parameters);
		// Neustart der Vorschaudarstellung
		camera.startPreview();

		requestLayout();
	}

	// @Override
	// protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	//
	// if (!RealityCamera.hasViewportSize()) {
	// super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	// } else {
	// int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
	// // int maxHeight = MeasureSpec.getSize(heightMeasureSpec);
	//
	// float ratio = maxWidth / (float) RealityCamera.cameraViewportWidth;
	//
	// int widthSpec = MeasureSpec.makeMeasureSpec(maxWidth,
	// MeasureSpec.EXACTLY);
	// int heightSpec = MeasureSpec.makeMeasureSpec(
	// (int) (RealityCamera.cameraViewportHeight * ratio),
	// MeasureSpec.EXACTLY);
	//
	// setMeasuredDimension(widthSpec, heightSpec);
	// }
	// }

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			// Kameraobjekt erzeugen
			int numCameras = Camera.getNumberOfCameras();
			CameraInfo cameraInfo = new CameraInfo();
			for (int i = 0; i < numCameras; i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
					camera = Camera.open(i);
					cameraId = i;
					break;
				}
			}

			if (camera == null) {
				throw new RuntimeException(); // jump to exception block
			}

			camera.setPreviewDisplay(this.holder);
		} catch (Exception e) {
			new AlertDialog.Builder(getContext()).setTitle(R.string.error)
					.setMessage(R.string.camera_not_available)
					.setNeutralButton(R.string.ok, null).show();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (camera != null) {
			/** stop and release the camera */
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		if (camera != null) {
			if (isShown()) {
				camera.startPreview();
			} else {
				camera.stopPreview();
			}
		}
		super.onVisibilityChanged(changedView, visibility);
	}

	public void setCameraSizeHint(int width, int height) {
		cameraSizeHintWidth = width;
		cameraSizeHintHeight = height;
	}

}
