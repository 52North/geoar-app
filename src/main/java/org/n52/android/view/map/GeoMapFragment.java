package org.n52.android.view.map;

import java.util.ArrayList;

import org.n52.android.NoiseARView;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.GeoARFragment;
import org.n52.android.view.InfoView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class GeoMapFragment extends GeoARFragment {

	private GeoMapView 			geoMapView;
	private ManualPositionView 	positionView;

	public GeoMapFragment(){
		geoARViews = new ArrayList<NoiseARView>();
	}
	
	public GeoMapFragment(MeasurementManager measureManager, LocationHandler locationHandler, InfoView infoView){
		this();
		this.mMeasureManager 	= measureManager;
		this.mInfoHandler 		= infoView;
		this.mLocationHandler 	= locationHandler;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		geoMapView = (GeoMapView) getView().findViewById(R.id.mapview);
		geoMapView.setMeasureManager(mMeasureManager);
		geoMapView.setLocationHandler(mLocationHandler);
		geoMapView.setInfoHandler(mInfoHandler);
		geoARViews.add(geoMapView);
		
		positionView = (ManualPositionView) getView().findViewById(R.id.manual_position_view);
		positionView.setLocationHandler(mLocationHandler);
		positionView.setMapView(geoMapView);
		geoARViews.add(positionView);
		
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		final View view = inflater.inflate(R.layout.map_fragment, container, false);
		return view;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}


	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	
}
