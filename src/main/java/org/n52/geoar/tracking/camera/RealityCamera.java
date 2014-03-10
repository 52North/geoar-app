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

import java.util.ArrayList;
import java.util.List;

import org.n52.geoar.GeoARApplication;
import org.n52.geoar.view.geoar.Settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera.Size;

/**
 * This class sums up all relevant camera parameters and also informs listeners
 * of changed parameters. Data gets updated by {@link CameraView}
 * 
 * @author Holger Hopmann
 * 
 */
public class RealityCamera {
	private static final String CAMERA_HEIGHT_PREF = "cameraHeight";

	public interface CameraUpdateListener {
		void onCameraUpdate();
	}

	public static float height = 1.6f; // "usage height", distance between
										// ground and device
	public static float fovY = 42.5f;
	// Viewport of camera preview
	public static int cameraViewportWidth;
	public static int cameraViewportHeight;
	private static boolean hasViewportSize = false;

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
		boolean changed = RealityCamera.fovY != fov;
		RealityCamera.fovY = fov;
		if (changed)
			onUpdate();
	}

	public static void setViewportSize(Size size) {
		setViewportSize(size.width, size.height);
	}

	public static void setViewportSize(int width, int height) {
		boolean changed = cameraViewportHeight != height
				|| cameraViewportWidth != width;
		cameraViewportHeight = height;
		cameraViewportWidth = width;
		RealityCamera.aspect = width / (float) height;
		hasViewportSize = true;
		if (changed)
			onUpdate();
	}

	public static boolean hasViewportSize() {
		return hasViewportSize;
	}

	@Deprecated
	public static void setAspect(float aspect) {
		// RealityCamera.aspect = aspect;
	}

	public static void changeHeight(float inc) {
		height += inc;
		onUpdate();
	}

	public static void setHeight(float height) {
		RealityCamera.height = height;
		onUpdate();
	}

	public static void saveState() {
		SharedPreferences preferences = GeoARApplication.applicationContext
				.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
						Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putFloat(CAMERA_HEIGHT_PREF, RealityCamera.height);
		editor.commit();
	}

	public static void restoreState() {
		SharedPreferences prefs = GeoARApplication.applicationContext
				.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
						Context.MODE_PRIVATE);
		RealityCamera.setHeight(prefs.getFloat(CAMERA_HEIGHT_PREF, 1.6f));
	}
}
