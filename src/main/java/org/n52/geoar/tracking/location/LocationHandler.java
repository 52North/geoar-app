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
package org.n52.geoar.tracking.location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.n52.geoar.utils.GeoLocation;
import org.n52.geoar.GeoARApplication;
import org.n52.geoar.R;
import org.n52.geoar.view.InfoView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Helper class to allow an safe and common way to receive location updates from
 * GPS. Also allows to override with user generated location updates
 * 
 * @author Holger Hopmann
 * 
 */
public class LocationHandler implements Serializable {
	private static final long serialVersionUID = 6337877169906901138L;
	private static final int DISABLE_LOCATION_UPDATES_MESSAGE = 1;
	private static final long DISABLE_LOCATION_UPDATES_DELAY = 12000;
	private static final Logger LOG = LoggerFactory
			.getLogger(LocationHandler.class);

	public interface OnLocationUpdateListener {
		void onLocationChanged(Location location);
	}

	private static LocationManager locationManager;
	private static final Object gpsStatusInfo = new Object();
	private static final Object gpsProviderInfo = new Object();
	private static List<OnLocationUpdateListener> listeners = new ArrayList<OnLocationUpdateListener>();

	private static boolean manualLocationMode;
	private static Location manualLocation;

	private static Handler disableUpdateHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DISABLE_LOCATION_UPDATES_MESSAGE) {
				onPause();
			} else {
				super.handleMessage(msg);
			}
		}
	};

	static {
		locationManager = (LocationManager) GeoARApplication.applicationContext
				.getSystemService(Context.LOCATION_SERVICE);
	}

	private static LocationListener locationListener = new LocationListener() {

		public void onProviderDisabled(String provider) {
			InfoView.setStatus(R.string.gps_nicht_aktiviert, -1,
					gpsProviderInfo);
		}

		public void onProviderEnabled(String provider) {
			InfoView.setStatus(R.string.gps_aktiviert, 2000, gpsProviderInfo);
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			if (status != LocationProvider.AVAILABLE) {
				InfoView.setStatus(R.string.warte_auf_gps_verf_gbarkeit, 5000,
						gpsStatusInfo);
			} else {
				InfoView.clearStatus(gpsStatusInfo);
			}

		}

		@Override
		public void onLocationChanged(Location location) {
			for (OnLocationUpdateListener listener : listeners) {
				listener.onLocationChanged(location);
			}
		}
	};

	/**
	 * Override with a user generated location fix
	 * 
	 * @param location
	 */
	public static void setManualLocation(Location location) {
		manualLocationMode = true;
		onPause();
		manualLocation = location;
		locationListener.onLocationChanged(manualLocation);
		InfoView.setStatus(R.string.manual_position, -1, manualLocation);
	}

	/**
	 * Override with a user generated location fix
	 * 
	 * @param geoPoint
	 */
	public static void setManualLocation(GeoLocation geoPoint) {
		manualLocationMode = true;
		onPause();
		if (manualLocation == null) {
			manualLocation = new Location("manual");
		}
		manualLocation.setLatitude(geoPoint.getLatitudeE6() / 1E6f);
		manualLocation.setLongitude(geoPoint.getLongitudeE6() / 1E6f);
		locationListener.onLocationChanged(manualLocation);

		InfoView.setStatus(R.string.manual_position, -1, manualLocation);
	}

	/**
	 * Reregister location updates for real location provider
	 */
	public static void disableManualLocation() {
		InfoView.clearStatus(manualLocation);
		manualLocationMode = false;
		onResume();
	}

	/**
	 * Performs processes needed to enable location updates
	 */
	public static void onResume() {
		if (!listeners.isEmpty()) {
			if (!locationManager
					.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				InfoView.setStatus(R.string.gps_nicht_aktiviert, -1,
						gpsProviderInfo);
			}
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
			LOG.debug("Requesting Location Updates");
		}
	}

	/**
	 * Should be called if activity gets paused. Removes location updates
	 */
	public static void onPause() {
		locationManager.removeUpdates(locationListener);
		InfoView.clearStatus(gpsProviderInfo);
		LOG.debug("Removed Location Updates");
	}

	/**
	 * Returns the last known location. Takes user generated locations into
	 * account. Returns location fix from network provider if no GPS available
	 * 
	 * @return
	 */
	public static Location getLastKnownLocation() {
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

	/**
	 * Register listener for location updates. Gets immediately called with the
	 * last known location, if available
	 * 
	 * @param listener
	 */
	public static void addLocationUpdateListener(
			OnLocationUpdateListener listener) {
		boolean shouldResume = listeners.isEmpty();
		if (!listeners.contains(listener)) {
			listeners.add(listener);
			if (getLastKnownLocation() != null) {
				listener.onLocationChanged(getLastKnownLocation());
			}
			disableUpdateHandler
					.removeMessages(DISABLE_LOCATION_UPDATES_MESSAGE);
		}

		if (shouldResume) {
			onResume();
		}
	}

	/**
	 * Removes location update listener
	 * 
	 * @param listener
	 */
	public static void removeLocationUpdateListener(
			OnLocationUpdateListener listener) {
		listeners.remove(listener);
		if (listeners.isEmpty()) {
			disableUpdateHandler.sendMessageDelayed(disableUpdateHandler
					.obtainMessage(DISABLE_LOCATION_UPDATES_MESSAGE),
					DISABLE_LOCATION_UPDATES_DELAY);
		}
	}

	public static void onSaveInstanceState(Bundle outState) {
		if (manualLocationMode) {
			outState.putParcelable("manualLocation", manualLocation);
		}
	}

	public static void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.get("manualLocation") != null) {
			setManualLocation((Location) savedInstanceState
					.getParcelable("manualLocation"));
		}
	}

	/**
	 * Registers location updates to actively receive a new single location fix.
	 * The provided listener will be called once or never, depending on the
	 * specified timeout.
	 * 
	 * @param listener
	 * @param timeoutMillis
	 *            Timeout in milliseconds
	 */
	public static void getSingleLocation(
			final OnLocationUpdateListener listener, int timeoutMillis) {

		/**
		 * Location update listener removing itself after first fix and
		 * canceling scheduled runnable
		 */
		class SingleLocationUpdateListener implements OnLocationUpdateListener {

			Runnable cancelSingleUpdateRunnable;

			@Override
			public void onLocationChanged(Location location) {
				// Clear updates
				removeLocationUpdateListener(this);
				disableUpdateHandler
						.removeCallbacks(this.cancelSingleUpdateRunnable);

				// Call actual listener
				listener.onLocationChanged(location);
			}
		}

		final SingleLocationUpdateListener singleUpdateListener = new SingleLocationUpdateListener();

		// Runnable to be called delayed by timeoutMillis to cancel location
		// update
		final Runnable cancelSingleUpdateRunnable = new Runnable() {
			@Override
			public void run() {
				removeLocationUpdateListener(singleUpdateListener);
			}
		};
		singleUpdateListener.cancelSingleUpdateRunnable = cancelSingleUpdateRunnable;

		// init updates
		addLocationUpdateListener(singleUpdateListener);
		disableUpdateHandler.postDelayed(cancelSingleUpdateRunnable,
				timeoutMillis);
	}
}
