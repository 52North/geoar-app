package org.n52.android.view.geoar;

import java.util.ArrayList;

import org.n52.android.GeoARView;
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

public class ARFragment extends GeoARFragment {
	
	private AugmentedView augmentedView;
	
	public ARFragment(){
		geoARViews = new ArrayList<GeoARView>();
	}
	
	public ARFragment(MeasurementManager measureManager, LocationHandler locationHandler, InfoView infoView){
		this();
		this.mMeasureManager 	= measureManager;
		this.mInfoHandler 		= infoView;
		this.mLocationHandler 	= locationHandler;
		this.setRetainInstance(true);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.fragment_ar, container, false);
		if(savedInstanceState == null){
			augmentedView = (AugmentedView) view.findViewById(R.id.glNoiseView);
			augmentedView.setInfoHandler(mInfoHandler);
			augmentedView.setLocationHandler(mLocationHandler);
			augmentedView.setMeasureManager(mMeasureManager);
			
			// Chart
//			NoiseChartView diagramView = (NoiseChartView) view.findViewById(R.id.noiseDiagramView);
//			diagramView.setNoiseGridValueProvider(augmentedView.getNoiseGridValueProvider());
//			geoARViews.add(diagramView);
			
			// Calibration View
			CalibrationControlView calibrationView = (CalibrationControlView) view.findViewById(R.id.calibrationView);
			geoARViews.add(calibrationView);
		}
		return view;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_ar, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
	}

}
