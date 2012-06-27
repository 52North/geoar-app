package org.n52.android.view.map;

import java.util.ArrayList;

import org.n52.android.GeoARView;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.GeoARFragment;
import org.n52.android.view.InfoView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;

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
public class GeoMapFragment extends GeoARFragment {

	private GeoMapView 			geoMapView;
	private ManualPositionView 	positionView;

	public GeoMapFragment(){
		geoARViews = new ArrayList<GeoARView>();
	}
	
	public GeoMapFragment(MeasurementManager measureManager, LocationHandler locationHandler, InfoView infoView){
		this();
		this.mMeasureManager 	= measureManager;
		this.mInfoHandler 		= infoView;
		this.mLocationHandler 	= locationHandler;
		setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		final View view = inflater.inflate(R.layout.map_fragment, container, false);
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {

		super.onActivityCreated(savedInstanceState);
		
		geoMapView = (GeoMapView) getView().findViewById(R.id.mapview);
		geoMapView.setMeasureManager(mMeasureManager);
		geoMapView.setLocationHandler(mLocationHandler);
		geoMapView.setInfoHandler(mInfoHandler);
		geoARViews.add(geoMapView);
		
		if(savedInstanceState != null){
			int lat = savedInstanceState.getInt("lat");
			int lon = savedInstanceState.getInt("lon");
			int zoom = savedInstanceState.getInt("zoom");
			geoMapView.setCenterAndZoom(new GeoPoint(lat, lon), zoom);
		}
		
		positionView = (ManualPositionView) getView().findViewById(R.id.manual_position_view);
		positionView.setLocationHandler(mLocationHandler);
		positionView.setMapView(geoMapView);
		geoARViews.add(positionView);
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
	 * Cleares the tileprovider and indicates to the VM 
	 * that it would be a good time to run the garbage collector.
	 */
	@Override
	public void onDestroy() {
		geoMapView.getTileProvider().clearTileCache();
		// 
		System.gc();
		super.onDestroy();
	}


	
}
