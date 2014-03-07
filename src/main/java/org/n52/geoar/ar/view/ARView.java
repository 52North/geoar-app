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
package org.n52.geoar.ar.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.geoar.ar.view.gl.ARSurfaceView;
import org.n52.geoar.ar.view.overlay.ARCanvasSurfaceView;
import org.n52.geoar.tracking.camera.CameraView;
import org.n52.geoar.tracking.camera.RealityCamera;
import org.n52.geoar.tracking.camera.RealityCamera.CameraUpdateListener;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class ARView extends FrameLayout implements CameraUpdateListener {

	private ARCanvasSurfaceView mCanvasOverlayView;
	private ARSurfaceView mARSurfaceView;
	private Map<Object, List<ARObject>> mARObjectMap = new HashMap<Object, List<ARObject>>();
	private ArrayList<ARObject> mARObjectsReusableList = new ArrayList<ARObject>();

	private CameraView mCameraView;

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

		mCameraView = new CameraView(getContext());
		addView(mCameraView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

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
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
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
		RealityCamera.removeCameraUpdateListener(this);
		mARSurfaceView.onPause();
	}

	public void onResume() {
		RealityCamera.addCameraUpdateListener(this);
		mARSurfaceView.onResume();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		if (!RealityCamera.hasViewportSize()) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		} else {
			int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
			int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

			mCameraView.setCameraSizeHint(maxWidth, maxHeight);

			float ratio = maxWidth / (float) RealityCamera.cameraViewportWidth;

			int widthSpec = MeasureSpec.makeMeasureSpec(maxWidth,
					MeasureSpec.EXACTLY);
			int heightSpec = MeasureSpec.makeMeasureSpec(
					(int) (RealityCamera.cameraViewportHeight * ratio),
					MeasureSpec.EXACTLY);
			super.onMeasure(widthSpec, heightSpec);
			setMeasuredDimension(widthSpec, heightSpec);
		}
	}

	@Override
	public void onCameraUpdate() {
		post(new Runnable() {
			@Override
			public void run() {
				requestLayout();
			}
		});
	}

}
