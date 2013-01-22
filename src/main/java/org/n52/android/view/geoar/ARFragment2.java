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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.n52.android.R;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.android.view.geoar.gl.DataSourceVisualizationHandler;
import org.n52.android.view.geoar.gui.ARCanvasSurfaceView;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class ARFragment2 extends SherlockFragment implements
		OnLocationUpdateListener {

	private static final List<DataSourceVisualizationHandler> checkedVisualizationHandler = new LinkedList<DataSourceVisualizationHandler>();
	private static final List<ARViewComponent> arViewComponents = new ArrayList<ARViewComponent>();

	public static final void addARViewComponent(ARViewComponent component) {
		if (arViewComponents.contains(component))
			return;
		// TODO XXX find a better solution...
		component.setVisualizationHandlerRef(checkedVisualizationHandler);
		arViewComponents.add(component);
	}

	public static final void removeARViewComponent(ARViewComponent component) {
		arViewComponents.remove(component);
	}

	public interface ARViewComponent {
		void onVisualizationHandlerAdded(DataSourceVisualizationHandler handler);

		void setVisualizationHandlerRef(
				List<DataSourceVisualizationHandler> handlers);
	}

	private OnCheckedChangedListener<DataSourceInstanceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceInstanceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceInstanceHolder item,
				boolean newState) {
			synchronized (checkedVisualizationHandler) {
				if (newState == true) {
					DataSourceVisualizationHandler handler = new DataSourceVisualizationHandler(
							augmentedView, item);

					checkedVisualizationHandler.add(handler);
					for (ARViewComponent arViewComponent : arViewComponents) {
						arViewComponent.onVisualizationHandlerAdded(handler);
					}
				} else {
					for (Iterator<DataSourceVisualizationHandler> it = checkedVisualizationHandler
							.iterator(); it.hasNext();) {
						DataSourceVisualizationHandler current = it.next();
						if (current.getDataSourceHolder() == item) {
							current.clear();
							it.remove();
							// break; maybe there are duplicates somehow
						}
					}
				}
			}
			// TODO

		}
	};

	private ARSurfaceView augmentedView;
	private ARCanvasSurfaceView canvasView;

	@Override
	public void onLocationChanged(Location location) {
		for (DataSourceVisualizationHandler handler : checkedVisualizationHandler)
			handler.onLocationChanged(location);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		FrameLayout layout = (FrameLayout) getView().findViewById(R.id.layout);

		final ActivityManager activityManager = (ActivityManager) getActivity()
				.getSystemService(Context.ACTIVITY_SERVICE);
		final ConfigurationInfo config = activityManager
				.getDeviceConfigurationInfo();

		canvasView = new ARCanvasSurfaceView(getActivity());
		layout.addView(canvasView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

		if (config.reqGlEsVersion >= 0x20000 || Build.PRODUCT.startsWith("sdk")) {
			augmentedView = new ARSurfaceView(getActivity());
			augmentedView.setZOrderMediaOverlay(true);
			layout.addView(augmentedView, 0, new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

			// final DisplayMetrics displayMetrics = new DisplayMetrics();
			// getActivity().getWindowManager().getDefaultDisplay()
			// .getMetrics(displayMetrics);
		}

		// XXX moved here from constructor
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			dataSource.getInstances().addOnCheckedChangeListener(
					dataSourceListener);
			// TODO unregister eventually!

			synchronized (checkedVisualizationHandler) {
				for (DataSourceInstanceHolder instance : dataSource
						.getInstances().getCheckedItems()) {
					// XXX Maybe filter for instances/data sources which really
					// support an ar visualization
					DataSourceVisualizationHandler handler = new DataSourceVisualizationHandler(
							augmentedView, instance);
					checkedVisualizationHandler.add(handler);
					for (ARViewComponent arViewComponent : arViewComponents) {
						arViewComponent.onVisualizationHandlerAdded(handler);
					}
				}
			}
		}

		// LocationHandler.addLocationUpdateListener(this); why?

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
		LocationHandler.removeLocationUpdateListener(this);
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
		// PluginLoader.getDataSources().removeOnCheckedChangeListener(
		// dataSourceListener);
		// TODO
		LocationHandler.addLocationUpdateListener(this);
		if (augmentedView != null)
			augmentedView.onResume();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		for (DataSourceHolder dataSource : PluginLoader.getDataSources()) {
			dataSource.getInstances().removeOnCheckedChangeListener(
					dataSourceListener);
		}
		if (augmentedView != null)
			augmentedView.destroyDrawingCache();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

}
