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
package org.n52.android;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.n52.android.newdata.PluginFragment;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.newdata.Visualization;
import org.n52.android.tracking.camera.RealityCamera;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.utils.GeoLocation;
import org.n52.android.view.geoar.ARFragment2;
import org.n52.android.view.map.MapFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.PopupWindow;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionProvider;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Core and only {@link Activity} in this application. Coordinates all its child
 * views, manager classes and inter-view communication. Derived from
 * {@link MapActivity} to utilize a {@link MapView} as child.
 * 
 * Uses an icon from www.androidicons.com
 * 
 * 
 */
public class GeoARActivity extends SherlockFragmentActivity {

	private MapFragment mapFragment = new MapFragment();
	private ARFragment2 arFragment = new ARFragment2();
	private PluginFragment cbFragment = new PluginFragment();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		if (savedInstanceState == null) {
			// First time init
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.info_use);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.ok, null);
			builder.setTitle(R.string.advice);
			builder.show();
		}

		if (GeoARApplication.checkAppFailed()) {
			// App Failed
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.info_failed_email);
			builder.setCancelable(true);
			builder.setPositiveButton(getString(R.string.send_report),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							GeoARApplication.sendFailMail(GeoARActivity.this);
							GeoARApplication.clearAppFailed();
						}
					});
			builder.setNegativeButton(android.R.string.no, null);
			builder.setTitle(R.string.sorry);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					// remove stack trace file
					GeoARApplication.clearAppFailed();
				}
			});
			builder.show();
		}

		showFragment(mapFragment);

		// Reset camera height if set
		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		RealityCamera.setHeight(prefs.getFloat("cameraHeight", 1.6f));

		if (savedInstanceState != null) {
			// restore manual positioning
			LocationHandler.onRestoreInstanceState(savedInstanceState);
		}
		LocationHandler.setManualLocation(new GeoLocation(51.965344, 7.600003));
	}

	private void showFragment(Fragment fragment) {
		if (fragment.isAdded()) {
			return;
		}
		FragmentTransaction transaction = getSupportFragmentManager()
				.beginTransaction();
		transaction.replace(R.id.fragmentContainer, fragment);
		transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		transaction.commit();
	}

	// @Override
	// public Object onRetainCustomNonConfigurationInstance() {
	// // Lets measurementManager survive a screen orientation change, so that
	// // no measurements need to get recached
	// return measurementManager;
	// }

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.TRANSLUCENT);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save manual positioning
		LocationHandler.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("cameraHeight", RealityCamera.height);
		editor.commit();

		PluginLoader.saveSelection();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate common general menu definition
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_general, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Set data source action providers here, since it depends on this
		// instance
		MenuItem menuItem = menu.findItem(R.id.item_map_datasource);
		if (menuItem != null)
			menuItem.setActionProvider(new DataSourcesActionProvider(
					Visualization.MapVisualization.class));

		menuItem = menu.findItem(R.id.item_ar_datasource);
		if (menuItem != null)
			menuItem.setActionProvider(new DataSourcesActionProvider(
					Visualization.ARVisualization.class));

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.item_ar:
			showFragment(arFragment);
			return true;

		case R.id.item_map:
			showFragment(mapFragment);
			return true;

		case R.id.item_selectsources:
			showFragment(cbFragment);
			return true;

		case R.id.item_about:
			AboutDialog aboutDialog = new AboutDialog(this);
			aboutDialog.setTitle(R.string.about_titel);
			aboutDialog.show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocationHandler.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		LocationHandler.onResume();
	}

	/**
	 * Reusable {@link ActionProvider} for data sources ActionBar menu. Shows a
	 * {@link PopupWindow} with options to enable/disable data sources and their
	 * visualizations
	 */
	public class DataSourcesActionProvider extends ActionProvider {

		private PopupWindow mPopup;
		private LayoutInflater mInflater;
		private ExpandableListView mListView;
		private Class<? extends Visualization> visualizationClass;

		public <E extends Visualization> DataSourcesActionProvider(
				Class<E> visualizationClass) {
			super(GeoARActivity.this);
			this.visualizationClass = visualizationClass;

			mInflater = LayoutInflater.from(GeoARActivity.this);
		}

		@Override
		public View onCreateActionView() {
			// Inflate the action view to be shown on the action bar.

			final View actionView = mInflater.inflate(
					R.layout.datasource_list_actionitem, null);
			actionView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (getPopup().isShowing()) {
						mPopup.dismiss();
					} else {
						// Offset by top margin to align top
						mPopup.showAsDropDown(actionView, 0, -mPopup
								.getContentView().getPaddingTop());
					}
				}
			});

			return actionView;
		}

		private PopupWindow getPopup() {
			if (mPopup == null) {
				ViewGroup layout = (ViewGroup) mInflater.inflate(
						R.layout.datasource_list_window, null);

				mListView = (ExpandableListView) layout
						.findViewById(R.id.expandableListView);

				Button moreButton = (Button) layout
						.findViewById(R.id.buttonMore);

				DataSourceListAdapter sourceListAdapter = new DataSourceListAdapter(
						GeoARActivity.this, mListView, visualizationClass);
				mListView.setAdapter(sourceListAdapter);
				mListView.setGroupIndicator(null);

				// Click event for "More" button
				moreButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						showFragment(cbFragment);
						mPopup.dismiss();
					}
				});

				mPopup = new PopupWindow(layout);
				mPopup.setTouchable(true);
				mPopup.setOutsideTouchable(true);

				TypedArray typedArray = obtainStyledAttributes(new int[] { android.R.attr.actionDropDownStyle });
				int resId = typedArray.getResourceId(0, 0);
				typedArray = obtainStyledAttributes(resId,
						new int[] { android.R.attr.popupBackground });
				mPopup.setBackgroundDrawable(new BitmapDrawable(getResources()));
				layout.setBackgroundResource(typedArray.getResourceId(0, 0));
				// mPopup.setBackgroundDrawable(typedArray.getDrawable(0));
				mPopup.setWindowLayoutMode(0, LayoutParams.WRAP_CONTENT);

				// Set width of menu
				mPopup.setWidth((int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 250, getResources()
								.getDisplayMetrics()));

			}
			return mPopup;
		}
	}

}