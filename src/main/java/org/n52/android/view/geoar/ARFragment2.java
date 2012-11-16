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

import org.n52.android.R;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;

public class ARFragment2 extends SherlockFragment {

	private ARSurfaceView augmentedView;

	public ARFragment2() {
		this.setRetainInstance(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		FrameLayout layout = (FrameLayout) getView().findViewById(R.id.layout);

		final ActivityManager activityManager = (ActivityManager) getActivity()
				.getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo config = activityManager
				.getDeviceConfigurationInfo();

		if (config.reqGlEsVersion >= 0x20000) {
			augmentedView = new ARSurfaceView(getActivity());
			layout.addView(augmentedView, 0, new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			final DisplayMetrics displayMetrics = new DisplayMetrics();
			getActivity().getWindowManager().getDefaultDisplay()
					.getMetrics(displayMetrics);

		}

		// FrameLayout layout = (FrameLayout) getView().findViewById(
		// R.id.frameLayout);
		//
		// augmentedView = new ARSurfaceView(getActivity());
		// layout.addView(augmentedView, LayoutParams.MATCH_PARENT,
		// LayoutParams.MATCH_PARENT);
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_ar, container,
				false);

		// // When working with the camera, it's useful to stick to one
		// orientation.
		// Activity activity = getActivity();
		// activity.setRequestedOrientation(
		// ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE );
		//
		// // Next, we disable the application's title bar...
		// activity.requestWindowFeature( Window.FEATURE_NO_TITLE );
		// // ...and the notification bar. That way, we can use the full screen.
		// activity.getWindow().setFlags(
		// WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN );

		if (savedInstanceState == null) {
			// TODO
			// augmentedView = (ARSurfaceView)
			// view.findViewById(R.id.glNoiseView);
			// augmentedView.setInfoHandler(mInfoHandler);
			// augmentedView.setLocationHandler(mLocationHandler);

			// Chart
			// NoiseChartView diagramView = (NoiseChartView)
			// view.findViewById(R.id.noiseDiagramView);
			// diagramView.setNoiseGridValueProvider(augmentedView.getNoiseGridValueProvider());
			// geoARViews.add(diagramView);

			// Calibration View
			// CalibrationControlView calibrationView = (CalibrationControlView)
			// view
			// .findViewById(R.id.calibrationView);
			// geoARViews.add(calibrationView);
		}
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (augmentedView != null)
			augmentedView.onPause();
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.menu_ar, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (augmentedView != null)
			augmentedView.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (augmentedView != null)
			augmentedView.destroyDrawingCache();
	}

}
