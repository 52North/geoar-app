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

import java.util.HashMap;
import java.util.Map;

import org.n52.android.R;
import org.n52.android.newdata.CheckList;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.android.utils.GeoLocation;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class ARFragment2 extends SherlockFragment implements
		OnLocationUpdateListener {

	private Map<DataSourceInstanceHolder, DataSourceVisualizationHandler2> mVisualizationHandlerMap = new HashMap<DataSourceInstanceHolder, DataSourceVisualizationHandler2>();

	// Listener for data source enabled state
	private OnCheckedChangedListener<DataSourceInstanceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceInstanceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceInstanceHolder item,
				boolean newState) {
			if (newState == true && !mVisualizationHandlerMap.containsKey(item)) {
				// new data source selected -> add new overlay handler
				DataSourceVisualizationHandler2 visualizationHandler = new DataSourceVisualizationHandler2(
						mARView, item);
				mVisualizationHandlerMap.put(item, visualizationHandler);
				GeoLocation loc = new GeoLocation(LocationHandler
						.getLastKnownLocation().getLatitude(), LocationHandler
						.getLastKnownLocation().getLongitude());
				visualizationHandler.setCenter(loc);
			} else if (newState == false) {
				// data source disabled -> remove corresponding overlay handler
				DataSourceVisualizationHandler2 visualizationHandler = mVisualizationHandlerMap
						.remove(item);
				if (visualizationHandler != null) {
					visualizationHandler.destroy();
				}
			}
		}
	};

	private ARView mARView;

	@Override
	public void onLocationChanged(Location location) {
		updateVisualizationHandlers(location);
	}

	private void updateVisualizationHandlers(Location location) {
		GeoLocation gLoc = new GeoLocation(location.getLatitude(),
				location.getLongitude());
		updateVisualizationHandlers(gLoc);
	}

	private void updateVisualizationHandlers(GeoLocation location) {
		for (DataSourceVisualizationHandler2 handler : mVisualizationHandlerMap
				.values()) {
			handler.setCenter(location);
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mARView = (ARView) getView().findViewById(R.id.arview);

		// Update all overlays after layouting the view
		new Handler().post(new Runnable() {
			@Override
			public void run() {
				updateVisualizationHandlers(LocationHandler
						.getLastKnownLocation());
			}
		});

		// add overlay handler for each enabled data source
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			CheckList<DataSourceInstanceHolder> instances = dataSource
					.getInstances();
			for (DataSourceInstanceHolder instance : instances
					.getCheckedItems()) {
				DataSourceVisualizationHandler2 visualizationHandler = new DataSourceVisualizationHandler2(
						mARView, instance);
				mVisualizationHandlerMap.put(instance, visualizationHandler);
			}

			// register for update events
			instances.addOnCheckedChangeListener(dataSourceListener);
		}

		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_ar, container, false);
	}

	@Override
	public void onPause() {
		super.onPause();
		mARView.onPause();
		LocationHandler.removeLocationUpdateListener(this);
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
		// PluginLoader.getDataSources().removeOnCheckedChangeListener(
		// dataSourceListener);
		// TODO
		mARView.onResume();
		LocationHandler.addLocationUpdateListener(this);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			dataSource.getInstances().removeOnCheckedChangeListener(
					dataSourceListener);
		}
		for (DataSourceVisualizationHandler2 handler : mVisualizationHandlerMap.values()) {
			handler.destroy();
		}
		mVisualizationHandlerMap.clear();
		// if (augmentedView != null)
		// augmentedView.destroyDrawingCache();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

}
