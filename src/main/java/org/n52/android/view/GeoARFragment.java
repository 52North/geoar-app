package org.n52.android.view;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.GeoARView;
import org.n52.android.data.MeasurementManager;
import org.n52.android.tracking.location.LocationHandler;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

public abstract class GeoARFragment extends Fragment {
	
	protected MeasurementManager 	mMeasureManager;
	protected InfoView 				mInfoHandler;
	protected LocationHandler 		mLocationHandler;
	
	protected List<GeoARView> 	geoARViews = new ArrayList<GeoARView>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		
		if(savedInstanceState != null)
			for (GeoARView arView : geoARViews) {
				arView.onRestoreInstanceState(savedInstanceState);
				if (arView instanceof View) {
					View view = (View) arView;
					String key = arView.getClass().getName() + "visibility";
					if (savedInstanceState.get(key) != null) {
						view.setVisibility(savedInstanceState.getInt(key));
					}
				}
			}
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Delegate selection event to all child views to allow them to react.
		for(GeoARView v : geoARViews)
			if(v.onOptionsItemSelected(item))
				return true;
		
		return super.onOptionsItemSelected(item);
	}
	
	public void setMeasureManager(MeasurementManager measureManager){
		this.mMeasureManager = measureManager;
		for(GeoARView v : geoARViews){
			v.setMeasureManager(measureManager);
		}
	}
	
	public void setInfoHandler(InfoView infoHandler){
		this.mInfoHandler = infoHandler;
		for(GeoARView v : geoARViews){
			v.setInfoHandler(infoHandler);
		}
	}
	
	public void setLocationHandler(LocationHandler locationHandler){
		this.mLocationHandler = locationHandler;
		for(GeoARView v : geoARViews){
			v.setLocationHandler(locationHandler);
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		for (GeoARView arView : geoARViews) {
			arView.onSaveInstanceState(outState);
			
			if (arView instanceof View) {
				// if there is a real View, save its visibility
				View view = (View) arView;
				outState.putInt(arView.getClass().getName() + "visibility",
						view.getVisibility());
			}
		}
	}
}
