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

import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.GeoARFragment2;
import org.n52.android.view.InfoView;
import org.osmdroid.util.GeoPoint;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class GeoMapFragment2 extends GeoARFragment2 {

	private GeoMapView2 geoMapView;

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
		super.onCreate(savedInstanceState);
		final View view = inflater.inflate(R.layout.map_fragment2, container,
				false);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);

		geoMapView = (GeoMapView2) getView().findViewById(R.id.mapview);
		geoMapView.setLocationHandler(mLocationHandler);
		geoMapView.setInfoHandler(mInfoHandler);
		geoARViews.add(geoMapView);

		if (savedInstanceState != null) {
			int lat = savedInstanceState.getInt("lat");
			int lon = savedInstanceState.getInt("lon");
			int zoom = savedInstanceState.getInt("zoom");
			geoMapView.setCenterAndZoom(new GeoPoint(lat, lon), zoom);
		}

		// positionView = (ManualPositionView) getView().findViewById(
		// R.id.manual_position_view);
		// positionView.setLocationHandler(mLocationHandler);
		// positionView.setMapView(geoMapView);
		// geoARViews.add(positionView);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
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
		geoMapView.getTileProvider().clearTileCache();
		//
		System.gc();
		super.onDestroy();
	}

}
