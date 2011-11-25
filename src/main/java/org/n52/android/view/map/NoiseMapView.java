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

import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.LocationHandler;
import org.n52.android.view.geoar.LocationHandler.OnLocationUpdateListener;
import org.n52.android.view.geoar.NoiseARView;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Location;
import android.os.Message;
import android.util.AttributeSet;
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

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;


/**
 * The overall {@link MapView}. Maintains {@link InterpolationOverlay} and
 * allows setting of custom locations to override {@link LocationHandler}
 * 
 * @author Holger Hopmann
 * 
 */
public class NoiseMapView extends MapView implements NoiseARView,
		OnGestureListener, OnLocationUpdateListener {

	/**
	 * Dialog to allow users to choose overlays. Nested class makes modification
	 * of map view easier and it is only used here.
	 * 
	 * @author Holger Hopmann
	 */
	private class MapOverlayDialog extends AlertDialog implements
			OnCheckedChangeListener {

		private ToggleButton buttonSatellite;
		private ToggleButton buttonTraffic;
		private ToggleButton buttonStreets;

		public MapOverlayDialog() {
			super(NoiseMapView.this.getContext());
			// Inflate Layout
			View layout = LayoutInflater.from(getContext()).inflate(
					R.layout.map_overlay_dialog, null);

			// Find Button Views
			buttonStreets = (ToggleButton) layout
					.findViewById(R.id.toggleButtonStreet);
			buttonStreets.setChecked(!isSatellite());

			buttonSatellite = (ToggleButton) layout
					.findViewById(R.id.ToggleButtonSatellite);
			buttonSatellite.setChecked(isSatellite());

			buttonTraffic = (ToggleButton) layout
					.findViewById(R.id.toggleButtonTraffic);
			buttonTraffic.setChecked(isTraffic());

			// Bind Check Listeners
			buttonStreets.setOnCheckedChangeListener(this);
			buttonSatellite.setOnCheckedChangeListener(this);
			buttonTraffic.setOnCheckedChangeListener(this);

			// Set Dialog Options
			setView(layout);
			setCancelable(true);
			setTitle(R.string.select_map);
			setButton(BUTTON_NEUTRAL, getContext().getString(R.string.ok),
					(Message) null);
		}

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// Change renderer state based on user check input
			switch (buttonView.getId()) {
			case R.id.toggleButtonStreet:
				if (buttonSatellite.isChecked() == isChecked) {
					buttonSatellite.setChecked(!isChecked);
				}
				break;
			case R.id.ToggleButtonSatellite:
				setSatellite(isChecked);
				if (buttonStreets.isChecked() == isChecked) {
					buttonStreets.setChecked(!isChecked);
				}
				break;
			case R.id.toggleButtonTraffic:
				setTraffic(isChecked);
				break;
			}
		}
	}

	private InfoView infoHandler;
	private InterpolationOverlay interpolationOverlay;
	private ImageView locationIndicator;

	private LocationHandler locationHandler;
	private GestureDetector gesture;
	private boolean manualPositionMode;

	public NoiseMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public NoiseMapView(Context context, String apiKey) {
		super(context, apiKey);
		init(context);
	}

	private void init(Context context) {
		if (isInEditMode()) {
			return;
		}

		this.gesture = new GestureDetector(getContext(), this);

		locationIndicator = new ImageView(context);
		locationIndicator
				.setImageResource(R.drawable.ic_maps_indicator_current_position_anim);
		locationIndicator.setVisibility(View.GONE);
		this.addView(locationIndicator);

		setBuiltInZoomControls(true);
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
	public void setMeasureManager(MeasurementManager measureManager) {
		if (interpolationOverlay == null) {
			interpolationOverlay = new InterpolationOverlay(measureManager,
					getWidth(), getHeight());
			if (infoHandler != null) {
				interpolationOverlay.setInfoHandler(infoHandler);
			}
			getOverlays().add(0, interpolationOverlay);
			interpolationOverlay.updateInterpolation(this, true);
		} else {
			interpolationOverlay.setMeasureManager(measureManager);
			interpolationOverlay.updateInterpolation(this, true);
		}
	}

	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
		if (interpolationOverlay != null) {
			interpolationOverlay.setInfoHandler(infoHandler);
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		if (interpolationOverlay != null) {
			interpolationOverlay.setInterpolationPixelSize(w, h);
		}
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (!isShown()) {
			// Cancel interpolation overlay updates if view gets invisible
			if (interpolationOverlay != null) {
				interpolationOverlay.abort();
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
					geoPoint, LayoutParams.CENTER));
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
			if (interpolationOverlay != null) {
				interpolationOverlay.updateInterpolation(this, true);
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

	public void setManualPositioning(boolean enabled) {
		if (enabled) {
			// activate manual positioning
			showOwnLocation(true, false);
		}
		manualPositionMode = enabled;
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
			GeoPoint geoPoint = getProjection().fromPixels((int) e.getX(),
					(int) e.getY());

			locationHandler.setManualLocation(geoPoint);
		}
		return false;
	}

	public void onLocationChanged(Location location) {
		showLocation(location);
	}
}
