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
package org.n52.geoar;

import java.io.IOException;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.n52.geoar.ar.view.ARFragment;
import org.n52.geoar.ar.view.IntroController;
import org.n52.geoar.map.view.MapFragment;
import org.n52.geoar.newdata.PluginFragment;
import org.n52.geoar.newdata.PluginLoader;
import org.n52.geoar.newdata.Visualization;
import org.n52.geoar.tracking.camera.RealityCamera;
import org.n52.geoar.tracking.location.LocationHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
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
 */
public class GeoARActivity extends SherlockFragmentActivity {

	private static final String CURRENT_FRAGMENT_KEY = "current_fragment";
	private MapFragment mMapFragment = new MapFragment();
	private ARFragment mARFragment = new ARFragment();
	private PluginFragment mPluginFragment = new PluginFragment();
	private Fragment[] mFragments = new Fragment[] { mMapFragment, mARFragment,
			mPluginFragment };

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		AssetManager assetManager = getAssets();
		String[] files;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			builder.setNegativeButton(android.R.string.no,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							GeoARApplication.clearAppFailed();
						}
					});
			builder.setTitle(R.string.sorry);
			builder.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					GeoARApplication.clearAppFailed();
				}
			});
			builder.show();
		}

		Fragment fragmentToShow = mMapFragment;
		if (savedInstanceState != null) {
			String currentFragmentClassName = savedInstanceState
					.getString(CURRENT_FRAGMENT_KEY);
			if (currentFragmentClassName != null) {
				for (Fragment fragment : mFragments) {
					if (fragment.getClass().getSimpleName()
							.equals(currentFragmentClassName)) {
						fragmentToShow = fragment;
						break;
					}
				}
			}

		}
		showFragment(fragmentToShow);

		RealityCamera.restoreState();

		if (savedInstanceState != null) {
			// restore manual positioning
			LocationHandler.onRestoreInstanceState(savedInstanceState);
		}

		IntroController.initPopupShow(this);

		// // TODO Debug only
		// LocationHandler.setManualLocation(new GeoLocation(51.965344,
		// 7.600003));
		
	}

	private void showFragment(Fragment fragment) {
		if (fragment.isAdded()) {
			return;
		}
		getSupportFragmentManager().executePendingTransactions();
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
		for (Fragment fragment : mFragments) {
			if (fragment.isAdded()) {
				outState.putString(CURRENT_FRAGMENT_KEY, fragment.getClass()
						.getSimpleName());
				break;
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		RealityCamera.saveState();
		PluginLoader.saveState();
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

		menuItem = menu.findItem(R.id.item_map);
		if (menuItem != null)
			IntroController.addViewToStep(7, menuItem.getActionView());

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.item_ar:
			showFragment(mARFragment);
			return true;

		case R.id.item_map:
			showFragment(mMapFragment);
			IntroController.notify(R.string.intro_desc_3_2);
			return true;

		case R.id.item_selectsources:
			showFragment(mPluginFragment);
			return true;

		case R.id.item_about:
			AboutDialog aboutDialog = new AboutDialog(this);
			aboutDialog.setTitle(R.string.about_titel);
			aboutDialog.show();
			return true;

		case R.id.item_givefeedbak:
			GeoARApplication.sendFeedbackMail(this);
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
		private View actionView;
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
			actionView = mInflater.inflate(R.layout.datasource_list_actionitem,
					null);

			// TODO use ActionMenuItemView when ABS resources work

			// actionView.findViewById(R.id.button).setOnClickListener(
			// new OnClickListener() {
			// @Override
			// public void onClick(View v) {
			// if (getPopup().isShowing()) {
			// mPopup.dismiss();
			// } else {
			// // Offset by top margin to align top
			// mPopup.showAsDropDown(actionView, 0, -mPopup
			// .getContentView().getPaddingTop());
			// }
			// }
			// });

			final View view = actionView.findViewById(R.id.button);
			view.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

					if (getPopup().isShowing()) {
						mPopup.dismiss();
					} else {
						// Offset by top margin to align top
						mPopup.showAsDropDown(actionView, 0, -mPopup
								.getContentView().getPaddingTop());

						IntroController.notify(getPopup().getContentView()
								.findViewById(R.id.buttonMore));
						IntroController.notify(R.string.intro_desc_3_3);
					}
				}
			});

			IntroController.addViewToStep(1, view);
			IntroController.addViewToStep(2, getPopup().getContentView()
					.findViewById(R.id.buttonMore));
			IntroController.addViewToStep(8, view);

			IntroController.notify(view);

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
				IntroController.addViewToStep(9, mListView.getChildAt(mListView.getFirstVisiblePosition()));
				mListView.setGroupIndicator(null);

				// Click event for "More" button
				moreButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						showFragment(mPluginFragment);
						mPopup.dismiss();
					}
				});

				mPopup = new ActionProviderPopupWindow(layout);
				mPopup.setTouchable(true);
				mPopup.setOutsideTouchable(true);

				TypedArray typedArray = obtainStyledAttributes(new int[] { R.attr.actionDropDownStyle });
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

		private class ActionProviderPopupWindow extends PopupWindow {

			public ActionProviderPopupWindow(ViewGroup layout) {
				super(layout);
			}

			@Override
			public void dismiss() {
				final View view = actionView.findViewById(R.id.button);
				IntroController.notify(view);
				super.dismiss();
			}

		}
	}

}