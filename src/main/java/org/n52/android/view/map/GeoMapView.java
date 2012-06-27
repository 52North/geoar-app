/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */
package org.n52.android.view.map;

import java.util.List;

import org.n52.android.GeoARView;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.tracking.location.LocationHandler.OnLocationUpdateListener;
import org.n52.android.view.InfoView;
import org.n52.android.view.map.overlay.InterpolationOverlay;
import org.n52.android.view.map.overlay.MapOverlayHandler;
import org.osmdroid.ResourceProxy;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;



/**
 * The overall {@link MapView}. Maintains {@link InterpolationOverlay} and
 * allows setting of custom locations to override {@link LocationHandler}
 * 
 * @author Holger Hopmann
 * 
 */
public class GeoMapView extends MapView implements GeoARView,
		OnGestureListener, OnLocationUpdateListener {

	/**
	 * Dialog to allow users to choose overlays. Nested class makes modification
	 * of map view easier and it is only used here.
	 * 
	 * @author Holger Hopmann
	 */
	private class MapOverlayDialog extends AlertDialog implements
			OnCheckedChangeListener {
		
		private ToggleButton buttonInterpolationOverlay;
		private ToggleButton buttonItemizedOverlay;

		public MapOverlayDialog() {
			super(GeoMapView.this.getContext());
			// Inflate Layout
			View layout = LayoutInflater.from(getContext()).inflate(
					R.layout.map_overlay_dialog, null);

//			// Find Button Views
			buttonInterpolationOverlay = (ToggleButton) layout
					.findViewById(R.id.ToggleButtonInterpolation);
			buttonInterpolationOverlay.setChecked(showInterpolationOverlay);
			
			buttonItemizedOverlay = (ToggleButton) layout
					.findViewById(R.id.ToggleButtonItemizedOverlay);
			buttonItemizedOverlay.setChecked(showItemizedOverlay);
			
			buttonInterpolationOverlay.setOnCheckedChangeListener(this);
			buttonItemizedOverlay.setOnCheckedChangeListener(this);
			
			// Set Dialog Options
			setView(layout);
			setCancelable(true);
			setTitle(R.string.select_map);
			setButton(BUTTON_NEUTRAL, getContext().getString(R.string.ok),
					(Message) null);
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			switch (buttonView.getId()){
			case R.id.ToggleButtonInterpolation:
				mapOverlayHandler.showInterpolationOverlay(isChecked);
				showInterpolationOverlay = isChecked;
				invalidate();
				break;
			case R.id.ToggleButtonItemizedOverlay:
				mapOverlayHandler.showItemizedOverlay(isChecked);
				showItemizedOverlay = isChecked;
				invalidate();
				break;
			}
		}
	}

	private InfoView infoHandler;
	private MapOverlayHandler mapOverlayHandler;
	private List<Overlay> mapOverlays;
	private ImageView locationIndicator;

	private LocationHandler locationHandler;
	private GestureDetector gesture;
	private boolean manualPositionMode;
	private MapController mapController;
	
	private ResourceProxy mResourceProxy;
	
	private boolean showItemizedOverlay;
	private boolean showInterpolationOverlay;
	
	public GeoMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}


	private void init(Context context) {
		if (isInEditMode()) 
			return;
		
		this.gesture = new GestureDetector(getContext(), this);
	
		locationIndicator = new ImageView(context);
		locationIndicator.setImageResource(R.drawable.ic_maps_indicator_current_position_anim);
		locationIndicator.setVisibility(View.GONE);
		this.addView(locationIndicator);
		
		mapController = this.getController();
		mapController.setZoom(15);
		// set first view to Mensa II 
		GeoPoint point2 = new GeoPoint(51965344, 7600003);
		mapController.setCenter(point2);
		
		mResourceProxy = getResourceProxy();
		
		setBuiltInZoomControls(true);
		setMultiTouchControls(true);
	}

	public void setLocationHandler(LocationHandler locationHandler) {
		this.locationHandler = locationHandler;
	}

	/**
	 * Sets {@link MeasurementManager} and initializes
	 * {@link InterpolationOverlay}
	 * 
	 * @param measureManager
	 */
	@Override
	public void setMeasureManager(MeasurementManager measureManager) {
		if (mapOverlayHandler == null || mapOverlays == null) {
			mapOverlayHandler = new MapOverlayHandler(this, measureManager,
					getWidth(), getHeight());
			mapOverlayHandler.setDrawable(this.getResources().getDrawable(R.drawable.icon));
			if (infoHandler != null) {
				mapOverlayHandler.setInfoHandler(infoHandler);
			}
//			getOverlays().clear();
//			getOverlays().addAll(mapOverlayHandler.getOverlays());
			
			mapOverlayHandler.updateOverlay(this, true);
		} else {
			mapOverlayHandler.setMeasureManager(measureManager);
			mapOverlayHandler.updateOverlay(this, true);
		}
	}
	
	@Override
	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
		if (mapOverlayHandler != null) {
			mapOverlayHandler.setInfoHandler(infoHandler);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (mapOverlayHandler != null) {
			mapOverlayHandler.setInterpolationPixelSize(w, h);
		}
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (!isShown()) {
			// Cancel interpolation overlay updates if view gets invisible
			if (mapOverlayHandler != null) {
				mapOverlayHandler.abort();
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
			locationIndicator.setLayoutParams(new MapView.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					geoPoint, LayoutParams.CENTER, 0 , 0));
			locationIndicator.setVisibility(View.VISIBLE);
		}
	}

	public void hideLocation() {
		locationIndicator.setVisibility(View.GONE);
	}

	public boolean showsLocation() {
		return locationIndicator.getVisibility() == View.VISIBLE;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		gesture.onTouchEvent(ev);
		Log.d("touch", "yea oh touch");
		mapOverlayHandler.onTouchEvent(ev, this);
		return super.onTouchEvent(ev);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_own_location:
			if (!manualPositionMode) {
				showOwnLocation(!showsLocation(), true);
			}
			// Event konsumieren
			return true;
		case R.id.item_select_map_overlay:
			new MapOverlayDialog().show();

			// Event konsumieren
			return true;
		case R.id.item_reload_map:
			if (mapOverlayHandler != null) {
				mapOverlayHandler.updateOverlay(this, true);
			}
			// Event konsumieren
			return true;
		}
		return false;
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
				getController().animateTo(geoPoint);
			}
		} else {
			hideLocation();
			locationHandler.removeLocationUpdateListener(this);
		}
	}


	/**
	 * 	Saves the current center of the map so that it can 
	 *  be restored.
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		// begin boilerplate code that allows parent classes to save state
		Bundle outState = new Bundle();
		outState.putParcelable("instanceState", super.onSaveInstanceState());
		outState.putInt("lat", getMapCenter().getLatitudeE6());
		outState.putInt("lon", getMapCenter().getLongitudeE6());
		outState.putInt("zoom", getZoomLevel());
		return outState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if(state instanceof Bundle){
			Bundle bundle = (Bundle) state;
			int lat = bundle.getInt("lat");
			int lon = bundle.getInt("lon");
			getController().setZoom(bundle.getInt("zoom"));
			getController().setCenter(new GeoPoint(lat, lon));
			super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
			return;
		}
		super.onRestoreInstanceState(state);
	}

	public void setManualPositioning(boolean enabled) {
		if (enabled) {
			// activate manual positioning
			showOwnLocation(true, false);
		}
		manualPositionMode = enabled;
	}
	
	public void setCenterAndZoom(GeoPoint g, int zoomLevel){
		MapController controller = getController();
		controller.setCenter(g);
		controller.setZoom(zoomLevel);
	}

	public Integer getMenuGroupId() {
		return R.id.group_mapview;
	}

	public boolean isVisible() {
		return isShown();
	}

	public boolean onDown(MotionEvent e) {
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		return false;
	}

	public void onLongPress(MotionEvent e) {

	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {

	}

	public boolean onSingleTapUp(MotionEvent e) {
		if (manualPositionMode) {
			// Set manual position in tap
			GeoPoint geoPoint = (GeoPoint) getProjection().fromPixels((int) e.getX(),
					(int) e.getY());

			locationHandler.setManualLocation(geoPoint);
		}
		return false;
	}

	public void onLocationChanged(Location location) {
		showLocation(location);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
	}

	

}
