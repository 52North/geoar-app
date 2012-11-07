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
import java.util.List;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;
import org.n52.android.GeoARApplication;
import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.GeoARFragment2;
import org.n52.android.view.InfoView;
//import org.osmdroid.util.GeoPoint;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class GeoMapFragment2 extends GeoARFragment2 {

	private GeoMapView3 geoMapView;

	// TODO
	// private ManualPositionView positionView;

	// public GeoMapFragment2(){
	// geoARViews = new ArrayList<GeoARView2>();
	// }

	public GeoMapFragment2(LocationHandler locationHandler, InfoView infoView) {
		// this();
		this.mInfoHandler = infoView;
		this.mLocationHandler = locationHandler;
		setRetainInstance(true);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);

		return inflater.inflate(R.layout.map_fragment3, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);

		// Get Layout root to add mapview programmatically
		FrameLayout layout = (FrameLayout) getView().findViewById(
				R.id.frameLayout);

		// "Hack", create anonymous MapActivity which passes all relevant
		// Context calls to the implementations of the activity of this
		// fragment
		MapActivity mapActivity = new MapActivity() {
			@Override
			public Resources getResources() {
				return GeoMapFragment2.this.getActivity().getResources();
			}

			@Override
			public ApplicationInfo getApplicationInfo() {
				return GeoMapFragment2.this.getActivity().getApplicationInfo();
			}

			@Override
			public Theme getTheme() {
				return GeoMapFragment2.this.getActivity().getTheme();
			}

			@Override
			public Context getBaseContext() {
				return GeoMapFragment2.this.getActivity().getBaseContext();
			}

			@Override
			public Object getSystemService(String name) {
				return GeoMapFragment2.this.getActivity()
						.getSystemService(name);
			}

			@Override
			public SharedPreferences getSharedPreferences(String name, int mode) {
				return GeoMapFragment2.this.getActivity().getSharedPreferences(
						name, mode);
			}
		};
		// Use special map activity as context since it is required by
		// mapsforge's mapview
		geoMapView = new GeoMapView3(mapActivity);
		// geoMapView = (GeoMapView3) getView().findViewById(R.id.mapview);
		geoMapView.setLocationHandler(mLocationHandler);
		geoMapView.setInfoHandler(mInfoHandler);
		geoARViews.add(geoMapView);
		layout.addView(geoMapView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		
	
		if (savedInstanceState != null) {
			int lat = savedInstanceState.getInt("lat");
			int lon = savedInstanceState.getInt("lon");
			byte zoom = savedInstanceState.getByte("zoom");
			geoMapView.setCenterAndZoom(new GeoPoint(lat, lon), zoom);
		}

		// positionView = (ManualPositionView) getView().findViewById(
		// R.id.manual_position_view);
		// positionView.setLocationHandler(mLocationHandler);
		// positionView.setMapView(geoMapView);
		// geoARViews.add(positionView);
	}

	/**
	 * 
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	/**
	 * Cleares the tileprovider and indicates to the VM that it would be a good
	 * time to run the garbage collector.
	 */
	@Override
	public void onDestroy() {
		// TODO destroy mapview
		// geoMapView.getTileProvider().clearTileCache();
		//
		System.gc();
		super.onDestroy();
	}

}
