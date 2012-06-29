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
package org.n52.android.tracking.camera;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.view.geoar.Settings;

import android.hardware.Camera.Size;

/**
 * This class sums up all relevant camera parameters and also informs listeners
 * of changed parameters. Data gets updated by {@link CameraView}
 * 
 * @author Holger Hopmann
 * 
 */
public class RealityCamera {
	public interface CameraUpdateListener {
		void onCameraUpdate();
	}

	public static float height = 1.6f;
	public static float fovY = 42.5f;
	public static float scale = 1f;
	// Viewport of camera preview
	public static int cameraViewportWidth;
	public static int cameraViewportHeight;

	public static float zNear = 0.5f;
	public static float zFar = Settings.SIZE_AR_INTERPOLATION
			+ Settings.RELOAD_DIST_AR;

	private static List<CameraUpdateListener> listeners = new ArrayList<CameraUpdateListener>();
	public static float aspect;


	public static void addCameraUpdateListener(CameraUpdateListener listener) {
		listeners.add(listener);
	}

	public static void removeCameraUpdateListener(CameraUpdateListener listener) {
		listeners.remove(listener);
	}

	private static void onUpdate() {
		for (CameraUpdateListener listener : listeners) {
			listener.onCameraUpdate();
		}
	}

	public static void setFovY(float fov) {
		RealityCamera.fovY = fov;
		onUpdate();
	}

	public static void setScale(float scale) {
		RealityCamera.scale = scale;
		onUpdate();
	}

	public static void setViewportSize(Size size) {
		setViewportSize(size.width, size.height);
	}

	public static void setViewportSize(int width, int height) {
		cameraViewportHeight = height;
		cameraViewportWidth = width;
		onUpdate();
	}

	public static void setAspect(float aspect) {
		RealityCamera.aspect = aspect;
	}

	public static void changeHeight(float inc) {
		height += inc;
		onUpdate();
	}

	public static void setHeight(float height) {
		RealityCamera.height = height;
		onUpdate();
	}
}
