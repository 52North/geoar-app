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

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.n52.android.newdata.CheckList;
import org.n52.android.newdata.DataSourceFragment;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceLoader;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesChangeListener;
import org.n52.android.newdata.Visualization;
import org.n52.android.tracking.camera.RealityCamera;
import org.n52.android.view.geoar.ARFragment2;
import org.n52.android.view.map.MapFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

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
public class GeoARActivity3 extends SherlockFragmentActivity {


	private MapFragment mapFragment = new MapFragment();
	private ARFragment2 arFragment = new ARFragment2();
	private DataSourceFragment cbFragment = new DataSourceFragment();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// // Get MeasurementManager from previous instance or create new one
		// Object lastMeasureManager = getLastCustomNonConfigurationInstance();
		// if (lastMeasureManager != null) {
		// measurementManager = (MeasurementManager) lastMeasureManager;
		// } else {
		// measurementManager = DataSourceAdapter.createMeasurementManager();
		// }

		// First time init, create the UI.
		if (savedInstanceState == null) {
			// Fragment newFragment = ViewFragment.newInstance();
			// getSupportFragmentManager().beginTransaction()
			// .add(android.R.id.content, newFragment).commit();

			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.info_use);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.ok, null);
			builder.setTitle(R.string.advice);
			builder.show();
		}
		//
		// mPager = (GeoARViewPager) findViewById(R.id.pager);
		// mPager.setFragmentManager(getSupportFragmentManager());
		// mPager.addFragment(mapFragment);
		// mPager.addFragment(arFragment);
		// mPager.addFragment(cbFragment);
		// mPager.showFragment(mapFragment);

		showFragment(mapFragment);

		// Reset camera height if set
		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		RealityCamera.setHeight(prefs.getFloat("cameraHeight", 1.6f));

		// if (savedInstanceState != null) {
		//
		// // restore manual positioning
		// // locationHandler.onRestoreInstanceState(savedInstanceState);
		// } else {
		// Builder builder = new AlertDialog.Builder(this);
		// builder.setMessage(R.string.info_use);
		// builder.setCancelable(true);
		// builder.setPositiveButton(R.string.ok, null);
		// builder.setTitle(R.string.advice);
		// builder.show();
		// }

	}

	private void showFragment(Fragment fragment) {
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
		// locationHandler.onSaveInstanceState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("cameraHeight", RealityCamera.height);
		editor.commit();

		DataSourceLoader.saveDataSourceSelection();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate common general menu definition
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.menu_general, menu);

		menu.findItem(R.id.item_datasource).setActionProvider(
				new DataSourcesActionProvider());
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		// TODO
		// case R.id.item_filter:
		// // Get current measurement filter
		// MeasurementFilter filter = measurementManager
		// .getMeasurementFilter();
		// if (filter == null) {
		// filter = new MeasurementFilter();
		// }
		// new FilterDialog(this, filter, measurementManager).show();
		// break;
		// case R.id.item_source:
		// // show data sources dialog
		// // TODO
		// // new DataSourceDialog(this, dataSources, measurementManager)
		// new DataSourceDialog(this, null, measurementManager).show();
		// break;
		case R.id.item_ar:
			showFragment(arFragment);
			return true;

		case R.id.item_map:
			showFragment(mapFragment);
			return true;

		case R.id.item_filter:
			DataSourceLoader.getSelectedDataSources().iterator().next()
					.createFilterDialog(this);
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

	/**
	 * Reusable {@link ActionProvider} for data sources ActionBar menu. Shows a
	 * {@link PopupWindow} with options to enable/diable data sources and their
	 * visualizations
	 */
	public class DataSourcesActionProvider extends ActionProvider {

		private PopupWindow mPopup;
		private LayoutInflater mInflater;
		private ExpandableListView mListView;

		public DataSourcesActionProvider() {
			super(GeoARActivity3.this);
			mInflater = LayoutInflater.from(GeoARActivity3.this);
		}

		@Override
		public View onCreateActionView() {
			// Inflate the action view to be shown on the action bar.

			final View actionView = mInflater.inflate(
					R.layout.datasource_list_actionitem, null);
			actionView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (getPopup().isShowing()) {
						mPopup.dismiss();
					} else {
						mPopup.showAsDropDown(actionView);
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

				DataSourceListAdapter sourceListAdapter = new DataSourceListAdapter();
				mListView.setAdapter(sourceListAdapter);

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
				mPopup.setBackgroundDrawable(new BitmapDrawable(getResources()));
				mPopup.setWindowLayoutMode(0, LayoutParams.WRAP_CONTENT);
				mPopup.setWidth(150); // Sets width of menu
			}
			return mPopup;
		}

		private class DataSourceListAdapter extends BaseExpandableListAdapter
				implements OnDataSourcesChangeListener {

			private class OnDataSourceCheckedChangeListener implements
					OnCheckedChangeListener {

				private int position;

				public void setPosition(int position) {
					this.position = position;
				}

				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					DataSourceHolder dataSource = selectedDataSources
							.get(position);

					if (selectedDataSources.isChecked(dataSource) != isChecked) {
						selectedDataSources.checkItem(dataSource, isChecked);
						notifyDataSetChanged();
					}
				}

			}

			private class OnDataSourceClickListener implements OnClickListener {

				private int position;

				public void setPosition(int position) {
					this.position = position;
				}

				@Override
				public void onClick(View v) {
					if (!mListView.isGroupExpanded(position))
						mListView.expandGroup(position);
					else
						mListView.collapseGroup(position);
				}

			}

			private class DataSourceViewHolder {
				public ImageView imageView;
				public TextView textView;
				public CheckBox checkBox;
				public OnDataSourceCheckedChangeListener checkListener;
				public OnDataSourceClickListener clickListener;
			}

			private class VisualizationViewHolder {
				public TextView textView;
				public CheckBox checkBox;
			}

			private CheckList<DataSourceHolder> selectedDataSources;

			public DataSourceListAdapter() {

				selectedDataSources = DataSourceLoader.getSelectedDataSources();

				DataSourceLoader.addOnSelectedDataSourcesUpdateListener(this);
				// TODO remove listener somehow
			}

			@Override
			public boolean areAllItemsEnabled() {
				return true;
			}

			@Override
			public Object getChild(int groupPosition, int childPosition) {
				return null;
			}

			@Override
			public long getChildId(int groupPosition, int childPosition) {
				return childPosition;
			}

			@Override
			public View getChildView(int groupPosition, int childPosition,
					boolean isLastChild, View view, ViewGroup parent) {
				VisualizationViewHolder viewHolder;

				if (view == null) {
					view = mInflater.inflate(
							R.layout.datasource_list_visualization_item,
							parent, false);
					viewHolder = new VisualizationViewHolder();
					viewHolder.textView = (TextView) view
							.findViewById(R.id.textView);

					viewHolder.checkBox = (CheckBox) view
							.findViewById(R.id.checkBox);

					view.setTag(viewHolder);
				} else {
					viewHolder = (VisualizationViewHolder) view.getTag();
				}

				DataSourceHolder dataSource = selectedDataSources
						.get(groupPosition);
				Visualization visualization = dataSource.getVisualizations()
						.get(childPosition);
				viewHolder.textView.setText(visualization.getClass()
						.getSimpleName());

				viewHolder.checkBox.setChecked(dataSource.getVisualizations()
						.isChecked(visualization));

				viewHolder.textView.setEnabled(selectedDataSources
						.isChecked(dataSource));

				return view;
			}

			@Override
			public int getChildrenCount(int groupPosition) {
				return selectedDataSources.get(groupPosition)
						.getVisualizations().size();
			}

			@Override
			public Object getGroup(int groupPosition) {
				return selectedDataSources.get(groupPosition);
			}

			@Override
			public int getGroupCount() {
				return selectedDataSources.size();
			}

			@Override
			public long getGroupId(int groupPosition) {
				return groupPosition;
			}

			@Override
			public View getGroupView(int groupPosition, boolean isExpanded,
					View view, ViewGroup parent) {
				DataSourceViewHolder viewHolder;

				if (view == null) {
					view = mInflater.inflate(
							R.layout.datasource_list_datasource_item, parent,
							false);
					viewHolder = new DataSourceViewHolder();
					viewHolder.imageView = (ImageView) view
							.findViewById(R.id.imageView);
					viewHolder.textView = (TextView) view
							.findViewById(R.id.textView);

					viewHolder.clickListener = new OnDataSourceClickListener();
					viewHolder.textView
							.setOnClickListener(viewHolder.clickListener);

					viewHolder.checkBox = (CheckBox) view
							.findViewById(R.id.checkBox);

					viewHolder.checkListener = new OnDataSourceCheckedChangeListener();
					viewHolder.checkBox
							.setOnCheckedChangeListener(viewHolder.checkListener);

					view.setTag(viewHolder);
				} else {
					viewHolder = (DataSourceViewHolder) view.getTag();
				}

				viewHolder.checkListener.setPosition(groupPosition);
				viewHolder.clickListener.setPosition(groupPosition);
				DataSourceHolder dataSource = selectedDataSources
						.get(groupPosition);

				viewHolder.textView.setText(dataSource.getName());
				viewHolder.checkBox.setChecked(selectedDataSources
						.isChecked(dataSource));

				return view;
			}

			@Override
			public boolean hasStableIds() {
				return false;
			}

			@Override
			public boolean isChildSelectable(int groupPosition,
					int childPosition) {
				return true;
			}

			@Override
			public boolean isEmpty() {
				return selectedDataSources.isEmpty();
			}

			@Override
			public void onDataSourcesChange() {
				notifyDataSetChanged();
			}

		}
	}

}