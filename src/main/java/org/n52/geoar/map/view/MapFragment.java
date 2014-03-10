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
package org.n52.geoar.map.view;

import java.util.HashMap;
import java.util.Map;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.overlay.ArrayCircleOverlay;
import org.mapsforge.android.maps.overlay.OverlayCircle;
import org.mapsforge.core.GeoPoint;
import org.n52.geoar.R;
import org.n52.geoar.map.view.GeoARMapView.OnZoomChangeListener;
import org.n52.geoar.map.view.overlay.DataSourcesOverlay;
import org.n52.geoar.map.view.overlay.DataSourcesOverlay.OnOverlayItemTapListener;
import org.n52.geoar.map.view.overlay.OverlayType;
import org.n52.geoar.newdata.CheckList;
import org.n52.geoar.newdata.CheckList.OnCheckedChangedListener;
import org.n52.geoar.newdata.DataSourceHolder;
import org.n52.geoar.newdata.DataSourceInstanceHolder;
import org.n52.geoar.newdata.PluginActivityContext;
import org.n52.geoar.newdata.PluginLoader;
import org.n52.geoar.tracking.location.LocationHandler;
import org.n52.geoar.tracking.location.LocationHandler.OnLocationUpdateListener;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.FrameLayout.LayoutParams;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class MapFragment extends SherlockFragment {

	private GeoARMapView mapView;

	private MapActivityContext mapActivity; // Special context to use MapView
											// without MapActivity

	private LocationOverlay locationOverlay;

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

	@SuppressLint("NewApi")
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Add MapView programmatically, since it needs a special context
		// depending on a call to getActivity, so it happens here and not in
		// onCreateView.
		System.gc();
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
                            OverlayType<? extends Geometry> item) {
                        Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle(item.getTitle())
                                .setMessage(item.getDescription())
                                .setNeutralButton(R.string.cancel, null);

                        PluginActivityContext pluginActivityContext = new PluginActivityContext(item.getDataSourceInstance()
                                .getParent().getPluginHolder()
                                .getPluginContext(), getActivity());
                        // TODO use view caching with convertView parameter
                        View featureView = item.getVisualization()
                                .getFeatureView(item.getSpatialEntity(), null,
                                        null, pluginActivityContext);

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

		if (Build.VERSION.SDK_INT >= 11) {
			// use layout change listener to get notified when mapview is
			// layouted and has valid projection information
			mapView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
				@SuppressLint("NewApi")
                @Override
				public void onLayoutChange(View v, int left, int top,
						int right, int bottom, int oldLeft, int oldTop,
						int oldRight, int oldBottom) {
					if (oldRight != right || oldBottom != bottom
							|| oldTop != top || oldLeft != left) {
						// View layouted first time -> update overlays,
						// projection
						// will be valid
						updateOverlays();
						mapView.removeOnLayoutChangeListener(this);
					}
				}
			});
		} else {
			// Fallback for older Versions, update after timeout
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {

				}
			}, 2000);
		}
	}

	private void showOwnLocation() {
		if (locationOverlay == null) {
			locationOverlay = new LocationOverlay();
			mapView.getOverlays().add(locationOverlay);
		}

		OnLocationUpdateListener updateListener = new OnLocationUpdateListener() {
			@Override
			public void onLocationChanged(Location location) {
				GeoPoint center = new GeoPoint(location.getLatitude(),
						location.getLongitude());
				locationOverlay.setLocation(center,
						location.hasAccuracy() ? location.getAccuracy() : 50);
				mapView.getController().setZoom(16);
				mapView.getController().setCenter(center);
			}
		};

		LocationHandler.getSingleLocation(updateListener, 5000);
		// TODO lock while getting position

		Location lastKnownLocation = LocationHandler.getLastKnownLocation();
		if (lastKnownLocation != null) {
			updateListener.onLocationChanged(lastKnownLocation);
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
		if (!mapView.getMapPosition().isValid()) {
			MapController controller = mapView.getController();
			controller.setZoom(15);
			controller.setCenter(new GeoPoint(51.935008, 7.652111)); // 52N
		}

		super.onResume();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.item_ownlocation) {
			showOwnLocation();

			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class LocationOverlay extends ArrayCircleOverlay {

		private OverlayCircle locationCircle;

		public LocationOverlay() {
			super(null, null);

			Paint paintLocationFill = new Paint();
			paintLocationFill.setStyle(Style.FILL);
			paintLocationFill.setColor(Color.BLUE);
			paintLocationFill.setAlpha(120);
			paintLocationFill.setAntiAlias(true);
			Paint paintLocationOutline = new Paint();
			paintLocationOutline.setStyle(Style.STROKE);
			paintLocationOutline.setColor(Color.BLUE);
			paintLocationOutline.setAlpha(200);
			paintLocationOutline.setAntiAlias(true);

			locationCircle = new OverlayCircle(paintLocationFill,
					paintLocationOutline);
			addCircle(locationCircle);
		}

		public void setLocation(GeoPoint center, float radius) {
			locationCircle.setCircleData(center, radius);
			requestRedraw();
		}

	}
}
