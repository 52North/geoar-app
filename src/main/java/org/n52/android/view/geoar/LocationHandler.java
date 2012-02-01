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
package org.n52.android.view.geoar;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.geoar.R;
import org.n52.android.view.InfoView;
import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;


/**
 * Helper class to allow an safe and common way to receive location updates from
 * GPS. Also allows to override with user generated location updates
 * 
 * @author Holger Hopmann
 * 
 */
public class LocationHandler implements LocationListener {

	public interface OnLocationUpdateListener {
		void onLocationChanged(Location location);
	}

	private InfoView infoHandler;
	private LocationManager locationManager;
	private Object gpsStatusInfo = new Object();
	private Object gpsProviderInfo = new Object();
	private List<OnLocationUpdateListener> listeners = new ArrayList<OnLocationUpdateListener>();

	private boolean manualLocationMode = true;
	private Location manualLocation;

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param infoHandler
	 *            The overall {@link InfoView} to generate status updates for
	 *            the user
	 */
	public LocationHandler(Context context, InfoView infoHandler) {
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
		this.infoHandler = infoHandler;

	}

	/**
	 * Override with a user generated location fix
	 * 
	 * @param location
	 */
	public void setManualLocation(Location location) {
		manualLocationMode = true;
		onPause();
		manualLocation = location;
		onLocationChanged(manualLocation);
		infoHandler.setStatus(R.string.manual_position, -1, manualLocation);
	}

	/**
	 * Override with a user generated location fix
	 * 
	 * @param geoPoint
	 */
	public void setManualLocation(GeoPoint geoPoint) {
		manualLocationMode = true;
		onPause();
		if (manualLocation == null) {
			manualLocation = new Location("manual");
		}
		manualLocation.setLatitude(geoPoint.getLatitudeE6() / 1E6f);
		manualLocation.setLongitude(geoPoint.getLongitudeE6() / 1E6f);
		onLocationChanged(manualLocation);

		infoHandler.setStatus(R.string.manual_position, -1, manualLocation);
	}

	/**
	 * Reregister location updates for real location provider
	 */
	public void disableManualLocation() {
		infoHandler.clearStatus(manualLocation);
		manualLocationMode = false;
		onResume();
	}

	/**
	 * Performs processes needed to enable location updates
	 */
	public void onResume() {
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			infoHandler.setStatus(R.string.gps_nicht_aktiviert, -1,
					gpsProviderInfo);
		}

		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				5000, 0, this);

	}

	/**
	 * Should be called if activity gets paused. Removes location updates
	 */
	public void onPause() {
		infoHandler.clearStatus(gpsProviderInfo);
		locationManager.removeUpdates(this);
	}

	public void onLocationChanged(Location location) {
		for (OnLocationUpdateListener listener : listeners) {
			listener.onLocationChanged(location);
		}
	}

	/**
	 * Returns the last known location. Takes user generated locations into
	 * account. Returns location fix from network provider if no GPS available
	 * 
	 * @return
	 */
	public Location getLastKnownLocation() {
		if (manualLocationMode) {
			return manualLocation;
		} else if (locationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER) != null) {
			return locationManager
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		} else {
			return locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
	}

	public void onProviderDisabled(String provider) {
		infoHandler
				.setStatus(R.string.gps_nicht_aktiviert, -1, gpsProviderInfo);
	}

	public void onProviderEnabled(String provider) {
		infoHandler.setStatus(R.string.gps_aktiviert, 2000, gpsProviderInfo);
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status != LocationProvider.AVAILABLE) {
			infoHandler.setStatus(R.string.warte_auf_gps_verf_gbarkeit, 5000,
					gpsStatusInfo);
		} else {
			infoHandler.clearStatus(gpsStatusInfo);
		}

	}

	/**
	 * Register listener for location updates. Gets immediately called with the
	 * last known location, if available
	 * 
	 * @param listener
	 */
	public void addLocationUpdateListener(OnLocationUpdateListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			if (getLastKnownLocation() != null) {
				listener.onLocationChanged(getLastKnownLocation());
			}
		}
	}

	/**
	 * Removes location update listener
	 * 
	 * @param listener
	 */
	public void removeLocationUpdateListener(OnLocationUpdateListener listener) {
		listeners.remove(listener);
	}

	public void onSaveInstanceState(Bundle outState) {
		if (manualLocationMode) {
			outState.putParcelable("manualLocation", manualLocation);
		}
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.get("manualLocation") != null) {
			setManualLocation((Location) savedInstanceState
					.getParcelable("manualLocation"));
		}
	}

}
