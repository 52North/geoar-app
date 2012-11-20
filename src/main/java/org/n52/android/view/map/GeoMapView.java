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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mapsforge.android.maps.MapController;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.core.GeoPoint;
import org.n52.android.R;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceLoader;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.android.view.InfoView;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * The overall {@link MapView}. Maintains {@link InterpolationOverlay} and
 * allows setting of custom locations to override {@link LocationHandler}
 * 
 * @author Holger Hopmann
 * 
 */
public class GeoMapView extends org.mapsforge.android.maps.MapView implements
		OnLocationUpdateListener {

	/**
	 * Dialog to allow users to choose overlays. Nested class makes modification
	 * of map view easier and it is only used here.
	 * 
	 * @author Holger Hopmann
	 */
	// private class MapOverlayDialog extends AlertDialog implements
	// OnCheckedChangeListener {
	//
	// private ToggleButton buttonInterpolationOverlay;
	// private ToggleButton buttonItemizedOverlay;
	//
	// public MapOverlayDialog() {
	// super(GeoMapView3.this.getContext());
	// // Inflate Layout
	// View layout = LayoutInflater.from(getContext()).inflate(
	// R.layout.map_overlay_dialog, null);
	//
	// // // Find Button Views
	// buttonInterpolationOverlay = (ToggleButton) layout
	// .findViewById(R.id.ToggleButtonInterpolation);
	// buttonInterpolationOverlay.setChecked(showInterpolationOverlay);
	//
	// buttonItemizedOverlay = (ToggleButton) layout
	// .findViewById(R.id.ToggleButtonItemizedOverlay);
	// buttonItemizedOverlay.setChecked(showItemizedOverlay);
	//
	// buttonInterpolationOverlay.setOnCheckedChangeListener(this);
	// buttonItemizedOverlay.setOnCheckedChangeListener(this);
	//
	// // Set Dialog Options
	// setView(layout);
	// setCancelable(true);
	//
	// setTitle(R.string.select_map);
	// setButton(BUTTON_NEUTRAL, getContext().getString(R.string.ok),
	// (Message) null);
	// }
	//
	// @Override
	// public void onCheckedChanged(CompoundButton buttonView,
	// boolean isChecked) {
	// // switch (buttonView.getId()){
	// // case R.id.ToggleButtonInterpolation:
	// // mapOverlayHandler.showInterpolationOverlay(isChecked);
	// // showInterpolationOverlay = isChecked;
	// // invalidate();
	// // break;
	// // case R.id.ToggleButtonItemizedOverlay:
	// // mapOverlayHandler.showItemizedOverlay(isChecked);
	// // showItemizedOverlay = isChecked;
	// // invalidate();
	// // break;
	// // }
	// }
	// }

	private InfoView infoHandler;
	private List<DataSourceOverlayHandler> overlayHandlers = new ArrayList<DataSourceOverlayHandler>();
	// private List<Overlay> mapOverlays;
	private ImageView locationIndicator;

	private LocationHandler locationHandler;
	// private GestureDetector gesture;
	private boolean manualPositionMode;

	private boolean showItemizedOverlay;
	private boolean showInterpolationOverlay;
	private DataSourcesOverlay dataSourcesOverlay;

	private OnCheckedChangedListener<DataSourceHolder> dataSourceListener = new OnCheckedChangedListener<DataSourceHolder>() {

		@Override
		public void onCheckedChanged(DataSourceHolder item, boolean newState) {
			if (newState == true) {
				// new data source selected -> add new overlay handler
				DataSourceOverlayHandler overlayHandler = new DataSourceOverlayHandler(
						dataSourcesOverlay, item);
				overlayHandler.updateOverlay(GeoMapView.this, true);
				overlayHandlers.add(overlayHandler);
			} else {
				// data source disabled -> find overlay holder and remove it
				for (Iterator<DataSourceOverlayHandler> it = overlayHandlers
						.iterator(); it.hasNext();) {
					DataSourceOverlayHandler current = it.next();
					if (current.getDataSource() == item) {
						current.clear();
						it.remove();
						break;
					}
				}
			}
		}
	};

	public GeoMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public GeoMapView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		if (isInEditMode())
			return;

		// TODO own location
		locationIndicator = new ImageView(context);
		locationIndicator
				.setImageResource(R.drawable.ic_maps_indicator_current_position_anim);
		locationIndicator.setVisibility(View.GONE);
		this.addView(locationIndicator);

		// Offline rendering here
		// setMapFile(new File(Environment.getExternalStorageDirectory()
		// + "/GeoAR/map.map"));
		setClickable(true);
		// setRenderTheme(DEFAULT_RENDER_THEME);

		setMapGenerator(new MapnikTileDownloader());
		setBuiltInZoomControls(true);

		// Center and zoom TODO move to fragment
		MapController controller = this.getController();
		controller.setZoom(15);
		controller.setCenter(new org.mapsforge.core.GeoPoint(51.965344,
				7.600003));

		// Data source handling
		dataSourcesOverlay = new DataSourcesOverlay();
		getOverlays().add(dataSourcesOverlay);

		// add overlay handler for each enabled data source
		for (DataSourceHolder dataSource : DataSourceLoader
				.getSelectedDataSources().getCheckedItems()) {
			DataSourceOverlayHandler overlayHandler = new DataSourceOverlayHandler(
					dataSourcesOverlay, dataSource);
			overlayHandler.updateOverlay(this, true);
			overlayHandlers.add(overlayHandler);
		}

		// register for update events
		DataSourceLoader.getSelectedDataSources().addOnCheckedChangeListener(
				dataSourceListener);
	}

	@Override
	public boolean onTouchEvent(MotionEvent motionEvent) {
		// Use motion event to inform overlay handlers that they should update
		// their data if needed
		for (DataSourceOverlayHandler handler : overlayHandlers) {
			handler.onTouchEvent(motionEvent, this);
		}

		return super.onTouchEvent(motionEvent);
	}

	@Deprecated
	public void setLocationHandler(LocationHandler locationHandler) {
		this.locationHandler = locationHandler;
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (!isShown()) {
			// Cancel overlay updates if view gets invisible
			for (DataSourceOverlayHandler handler : overlayHandlers) {
				handler.cancel();
			}
		}
	}

	/**
	 * Shows most recent position on map
	 * 
	 * @param location
	 */
	public void showLocation(Location location) {
		if (location != null) {
			GeoPoint geoPoint = new GeoPoint(
					(int) (location.getLatitude() * 1E6),
					(int) (location.getLongitude() * 1E6));
			// TODO extend mapsforge or create new overlay
			// http://code.google.com/p/mapsforge/issues/detail?id=267

			// locationIndicator.setLayoutParams(new MapView.LayoutParams(
			// LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
			// geoPoint, LayoutParams.CENTER, 0, 0));
			locationIndicator.setVisibility(View.VISIBLE);
		}
	}

	public void hideLocation() {
		locationIndicator.setVisibility(View.GONE);
	}

	public boolean showsLocation() {
		return locationIndicator.getVisibility() == View.VISIBLE;
	}

	public void showOwnLocation(boolean show, boolean pan) {
		if (show) {
			Location lastKnownLocation = null;
			if (locationHandler != null) {
				locationHandler.addLocationUpdateListener(this);
				lastKnownLocation = locationHandler.getLastKnownLocation();
			}
			if (lastKnownLocation == null) {
				Toast.makeText(getContext(),
						R.string.akteulle_position_unbekannt, Toast.LENGTH_LONG)
						.show();
			} else if (pan) {
				GeoPoint geoPoint = new GeoPoint(
						(int) (lastKnownLocation.getLatitude() * 1E6),
						(int) (lastKnownLocation.getLongitude() * 1E6));
				// TODO
				// getController().animateTo(geoPoint);
			}
		} else {
			hideLocation();
			locationHandler.removeLocationUpdateListener(this);
		}
	}

	// /**
	// * Saves the current center of the map so that it can be restored.
	// */
	// @Override
	// protected Parcelable onSaveInstanceState() {
	// Bundle outState = new Bundle();
	// outState.putParcelable("instanceState", super.onSaveInstanceState());
	// outState.putDouble("lon", getMapPosition().getMapCenter()
	// .getLongitude());
	// outState.putDouble("lat", getMapPosition().getMapCenter().getLatitude());
	// outState.putByte("zoom", getMapPosition().getZoomLevel());
	// return outState;
	// }
	//
	// @Override
	// protected void onRestoreInstanceState(Parcelable state) {
	// if (state instanceof Bundle) {
	// Bundle bundle = (Bundle) state;
	// getController().setCenter(
	// new GeoPoint(bundle.getDouble("lat"), bundle
	// .getDouble("lon")));
	//
	// getController().setZoom(bundle.getByte("zoom"));
	//
	// super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
	// } else {
	// super.onRestoreInstanceState(state);
	// }
	// }

	public void setManualPositioning(boolean enabled) {
		if (enabled) {
			// activate manual positioning
			showOwnLocation(true, false);
		}
		manualPositionMode = enabled;
	}

	public void setCenterAndZoom(GeoPoint g, byte zoomLevel) {

		MapController controller = getController();
		controller.setCenter(g);
		controller.setZoom(zoomLevel);
	}

	// public boolean onSingleTapUp(MotionEvent e) {
	// if (manualPositionMode) {
	// // Set manual position in tap
	// GeoPoint geoPoint = (GeoPoint) getProjection().fromPixels(
	// (int) e.getX(), (int) e.getY());
	//
	// // locationHandler.setManualLocation(geoPoint);
	// }
	// return false;
	// }

	public void onLocationChanged(Location location) {
		showLocation(location);
	}

}
