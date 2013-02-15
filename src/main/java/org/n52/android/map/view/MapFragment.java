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
package org.n52.android.map.view;

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.core.GeoPoint;
import org.n52.android.R;
import org.n52.android.map.view.DataSourcesOverlay.OnOverlayItemTapListener;
import org.n52.android.map.view.GeoARMapView.OnZoomChangeListener;
import org.n52.android.newdata.CheckList;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.utils.GeoLocation;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;

public class MapFragment extends SherlockFragment {

	private GeoARMapView mapView;

	private MapActivityContext mapActivity; // Special context to use MapView
											// without MapActivity

	// Overlay fields
	private Map<DataSourceInstanceHolder, DataSourceOverlayHandler> overlayHandlerMap;
	private DataSourcesOverlay dataSourcesOverlay;

	// Listener for data source enabled state
	private OnCheckedChangedListener<DataSourceInstanceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceInstanceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceInstanceHolder item,
				boolean newState) {
			if (newState == true && !overlayHandlerMap.containsKey(item)) {
				// new data source selected -> add new overlay handler
				DataSourceOverlayHandler overlayHandler = new DataSourceOverlayHandler(
						dataSourcesOverlay, item);
				overlayHandlerMap.put(item, overlayHandler);
				overlayHandler.updateOverlay(mapView, true);
			} else if (newState == false) {
				// data source disabled -> remove corresponding overlay handler
				DataSourceOverlayHandler overlayHandler = overlayHandlerMap
						.remove(item);
				if (overlayHandler != null) {
					overlayHandler.destroy();
				}
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.map_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Add MapView programmatically, since it needs a special context
		// depending on a call to getActivity, so it happens here and not in
		// onCreateView.

		mapActivity = new MapActivityContext(getActivity());
		mapView = new GeoARMapView(mapActivity);

		// Offline rendering here
		// setMapFile(new File(Environment.getExternalStorageDirectory()
		// + "/GeoAR/map.map"));
		mapView.setClickable(true);
		// setRenderTheme(DEFAULT_RENDER_THEME);

		mapView.setMapGenerator(new MapnikTileDownloader());
		mapView.setBuiltInZoomControls(true);
		mapView.getMapZoomControls().setZoomControlsGravity(
				Gravity.LEFT | Gravity.TOP);

		// controller.setZoom(15);
		// controller.setCenter(new GeoPoint(51.965344, 7.600003)); //
		// Coesfelder
		// Kreuz
		// LocationHandler.setManualLocation(new GeoLocation(51.965344,
		// 7.600003));
		// Data source handling
		overlayHandlerMap = new HashMap<DataSourceInstanceHolder, DataSourceOverlayHandler>();

		dataSourcesOverlay = new DataSourcesOverlay();
		dataSourcesOverlay
				.setOverlayItemTapListener(new OnOverlayItemTapListener() {

					@Override
					public boolean onOverlayItemTap(
							VisualizationOverlayItem item) {
						Builder builder = new AlertDialog.Builder(getActivity());
						builder.setTitle(item.getTitle())
								.setMessage(item.getSnippet())
								.setNeutralButton(R.string.cancel, null);

						// TODO use view caching with convertView parameter
						View featureView = item.getVisualization()
								.getFeatureView(item.getSpatialEntity(), null,
										null, getActivity());

						if (featureView != null) {
							builder.setView(featureView);
						}
						builder.show();
						return true;
					}
				});

		mapView.getOverlays().add(dataSourcesOverlay);

		mapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent motionEvent) {
				// Use motion event to inform overlay handlers that they
				// should update their data if needed
				if (motionEvent.getAction() == MotionEvent.ACTION_UP
						|| motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {

					updateOverlays();
				}
				return false;
			}
		});

		mapView.setOnZoomChangeListener(new OnZoomChangeListener() {
			@Override
			public void onZoomChange() {
				updateOverlays();
			}
		});

		// Update all overlays after layouting the mapview
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				updateOverlays();
			}
		});
		// Get Layout root
		ViewGroup layout = (ViewGroup) getView();
		layout.addView(mapView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		layout.requestLayout();

		// add overlay handler for each enabled data source
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			CheckList<DataSourceInstanceHolder> instances = dataSource
					.getInstances();
			for (DataSourceInstanceHolder instance : instances
					.getCheckedItems()) {
				DataSourceOverlayHandler overlayHandler = new DataSourceOverlayHandler(
						dataSourcesOverlay, instance);
				overlayHandlerMap.put(instance, overlayHandler);
			}

			// register for update events
			instances.addOnCheckedChangeListener(dataSourceListener);
		}

	}

	private void updateOverlays() {
		for (DataSourceOverlayHandler handler : overlayHandlerMap.values()) {
			handler.updateOverlay(mapView, false);
		}
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.menu_map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onDestroy() {
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			dataSource.getInstances().removeOnCheckedChangeListener(
					dataSourceListener);
		}
		for (DataSourceOverlayHandler handler : overlayHandlerMap.values()) {
			handler.destroy();
		}
		overlayHandlerMap.clear();
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		mapActivity.destroy();

		((ViewGroup) getView()).removeView(mapView);
		dataSourcesOverlay.clear();
		super.onDestroyView();
	}

	@Override
	public void onPause() {
		mapActivity.pause();
		for (DataSourceOverlayHandler handler : overlayHandlerMap.values()) {
			handler.cancel();
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		mapActivity.resume();
		super.onResume();
	}
}
