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
package org.n52.android.view.map;

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.core.GeoPoint;
import org.n52.android.R;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceLoader;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * 
 * 
 * 
 */
public class MapFragment extends SherlockFragment {

	private MapView mapView;

	private MapActivityContext mapActivity; // Special ontext to use MapView
											// without MapActivity

	// Overlay fields
	private Map<DataSourceHolder, DataSourceOverlayHandler> overlayHandlerMap = new HashMap<DataSourceHolder, DataSourceOverlayHandler>();
	private DataSourcesOverlay dataSourcesOverlay;

	// Listener for data source enabled state
	private OnCheckedChangedListener<DataSourceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceHolder item, boolean newState) {
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
					overlayHandler.clear();
				}
			}
		}
	};

	public MapFragment() {
		setRetainInstance(true);
	}

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
		// Get Layout root to add mapview programmatically
		FrameLayout layout = (FrameLayout) getView().findViewById(
				R.id.frameLayout);

		// Use special map activity as context since it is required by
		// mapsforge's mapview
		mapActivity = new MapActivityContext(getActivity());
		mapView = new MapView(mapActivity);

		layout.addView(mapView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

		// Offline rendering here
		// setMapFile(new File(Environment.getExternalStorageDirectory()
		// + "/GeoAR/map.map"));
		mapView.setClickable(true);
		// setRenderTheme(DEFAULT_RENDER_THEME);

		mapView.setMapGenerator(new MapnikTileDownloader());
		mapView.setBuiltInZoomControls(true);

		// Center and zoom
		MapController controller = mapView.getController();
		controller.setZoom(15);
		controller.setCenter(new GeoPoint(51.965344, 7.600003)); // Coesfelder
																	// Kreuz

		// Data source handling
		dataSourcesOverlay = new DataSourcesOverlay();
		mapView.getOverlays().add(dataSourcesOverlay);
		mapView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent motionEvent) {
				// Use motion event to inform overlay handlers that they should
				// update their data if needed
				for (DataSourceOverlayHandler handler : overlayHandlerMap
						.values()) {
					handler.onTouchEvent(motionEvent, mapView);
				}
				return false;
			}
		});

		// add overlay handler for each enabled data source
		for (DataSourceHolder dataSource : DataSourceLoader
				.getSelectedDataSources().getCheckedItems()) {
			DataSourceOverlayHandler overlayHandler = new DataSourceOverlayHandler(
					dataSourcesOverlay, dataSource);
			overlayHandler.updateOverlay(mapView, true);
			overlayHandlerMap.put(dataSource, overlayHandler);
		}

		// register for update events
		DataSourceLoader.getSelectedDataSources().addOnCheckedChangeListener(
				dataSourceListener);
	}

	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.menu_map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onDestroy() {
		DataSourceLoader.getSelectedDataSources()
				.removeOnCheckedChangeListener(dataSourceListener);
		mapActivity.destroy();
		super.onDestroy();
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
