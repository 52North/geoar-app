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
package org.n52.android.ar.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.android.ar.view.gl.ARSurfaceView;
import org.n52.android.ar.view.overlay.ARCanvasSurfaceView;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.tracking.camera.CameraView;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class ARView extends FrameLayout {

	private ARCanvasSurfaceView mCanvasOverlayView;
	private ARSurfaceView mARSurfaceView;
	private Map<Object, List<ARObject>> mARObjectMap = new HashMap<Object, List<ARObject>>();
	private ArrayList<ARObject> mARObjectsReusableList = new ArrayList<ARObject>();
//	private CameraView mCameraView;

	public ARView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ARView(Context context) {
		super(context);
		init();
	}

	private void init() {
		if (isInEditMode()) {
			return;
		}

//		mCameraView = new CameraView(getContext());
//		addView(mCameraView, LayoutParams.MATCH_PARENT,
//				LayoutParams.MATCH_PARENT);

		final ActivityManager activityManager = (ActivityManager) getContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo config = activityManager
				.getDeviceConfigurationInfo();

		if (config.reqGlEsVersion >= 0x20000 || Build.PRODUCT.startsWith("sdk")) {
			// Add ARSurfaceView only if OpenGL ES Version 2 supported
			mARSurfaceView = new ARSurfaceView(this);
			mARSurfaceView.setKeepScreenOn(true);
			mARSurfaceView.setZOrderMediaOverlay(true);
			addView(mARSurfaceView, new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			// final DisplayMetrics displayMetrics = new DisplayMetrics();
			// getActivity().getWindowManager().getDefaultDisplay()
			// .getMetrics(displayMetrics);
		}

		mCanvasOverlayView = new ARCanvasSurfaceView(this);
		addView(mCanvasOverlayView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
	}

	/**
	 * Sets the ARObjects to render with a specified key. Objects previously set
	 * using this key will get removed by this call.
	 * 
	 * @param arObjects
	 * @param key
	 */
	public void setARObjects(final List<ARObject> arObjects, final Object key) {
		synchronized (this.mARObjectMap) {
			List<ARObject> previousMapping = this.mARObjectMap.put(key,
					arObjects);
			if (previousMapping != null) {
				previousMapping.clear();
			}
		}

		mARSurfaceView.notifyARObjectsChanged();
		mCanvasOverlayView.notifyARObjectsChanged();
	}

	/**
	 * Removes all ARObjects which were previously set using the specified key
	 * 
	 * @param key
	 */
	public void clearARObjects(Object key) {
		synchronized (this.mARObjectMap) {
			mARObjectMap.remove(key);
		}

		mARSurfaceView.notifyARObjectsChanged();
		mCanvasOverlayView.notifyARObjectsChanged();
	}

	/**
	 * Removes all ARObjects
	 * 
	 */
	// TODO usage?
	public void clearARObjects() {
		synchronized (this.mARObjectMap) {
			for (List<ARObject> itemList : mARObjectMap.values()) {
				itemList.clear();
			}
			mARObjectMap.clear();
		}

		mARSurfaceView.notifyARObjectsChanged();
		mCanvasOverlayView.notifyARObjectsChanged();
	}

	public List<ARObject> getARObjects() {
		synchronized (mARObjectsReusableList) {
			synchronized (this.mARObjectMap) {
				mARObjectsReusableList.clear();
				for (List<ARObject> itemList : mARObjectMap.values()) {
					mARObjectsReusableList.addAll(itemList);
				}
				return mARObjectsReusableList;
			}
		}
	}

	public void onPause() {
		mARSurfaceView.onPause();
	}

	public void onResume() {
		mARSurfaceView.onResume();
	}

}
