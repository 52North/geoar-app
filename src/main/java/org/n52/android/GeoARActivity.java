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

package org.n52.android;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.data.DataSourceAdapter;
import org.n52.android.data.MeasurementFilter;
import org.n52.android.data.MeasurementManager;
import org.n52.android.dialog.DataSourceDialog;
import org.n52.android.dialog.FilterDialog;
import org.n52.android.ext.actionbar.ActionBarActivity;
import org.n52.android.geoar.R;
import org.n52.android.tracking.camera.RealityCamera;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.GeoARFragment;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.ARFragment;
import org.n52.android.view.map.GeoMapFragment;
import org.osmdroid.views.MapView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;


/**
 * Core and only {@link Activity} in this application. Coordinates all its child
 * views, manager classes and inter-view communication. Derived from
 * {@link MapActivity} to utilize a {@link MapView} as child.
 * 
 * Uses an icon from www.androidicons.com
 * 
 * @author Arne de Wall
 * 
 */
public class GeoARActivity extends ActionBarActivity {

	private MeasurementManager measurementManager;
	private InfoView infoView;
	
	private LocationHandler locationHandler;
	private List<GeoARView> noiseARViews = new ArrayList<GeoARView>();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.main);
		
		if(savedInstanceState == null){
//			DataSourceAdapter.initFactoryLoader(getClassLoader(), this);
		}


		infoView = (InfoView) findViewById(R.id.infoView);
		locationHandler = new LocationHandler(this, infoView);
		
		// Get MeasurementManager from previous instance or create new one
		Object lastMeasureManager = getLastCustomNonConfigurationInstance();
		if (lastMeasureManager != null) { 
			measurementManager = (MeasurementManager) lastMeasureManager;
		} else {
			measurementManager = DataSourceAdapter.createMeasurementManager();
		}
		
		// First time init, create the UI.
		if(savedInstanceState == null){
			Fragment newFragment = UiFragment.newInstance(measurementManager, locationHandler);
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, 
					newFragment).commit();
		}


		// Reset camera height if set
		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		RealityCamera.setHeight(prefs.getFloat("cameraHeight", 1.6f));

		if (savedInstanceState != null) {

			// restore manual positioning
			locationHandler.onRestoreInstanceState(savedInstanceState);
		} else {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.info_use);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.ok, null);
			builder.setTitle(R.string.advice);
			builder.show();
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		// Lets measurementManager survive a screen orientation change, so that
		// no measurements need to get recached
		return measurementManager; 
	} 


	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.TRANSLUCENT);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save manual positioning
		locationHandler.onSaveInstanceState(outState);
	}

	@Override
	protected void onResume() {
		// delegate to locationHandler
		locationHandler.onResume();
		super.onResume();
	}

	@Override
	protected void onPause() {
		// delegate to locationHandler
//		locationHandler.onPause();
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();

		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("cameraHeight", RealityCamera.height);
		editor.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// inflate common general menu definition
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.general, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getGroupId() != Menu.NONE) {
			// Delegate selection event to all child views to allow them to
			// react.
			for (GeoARView view : noiseARViews) {
				if (view.onOptionsItemSelected(item)) {
					// Event consumed
					return true;
				}
			}
		} else {
			// Item dows not belong to any child view
			switch (item.getItemId()) {
			case R.id.item_filter:
				// Get current measurement filter
				MeasurementFilter filter = measurementManager
						.getMeasurementFilter();
				if (filter == null) {
					filter = new MeasurementFilter();
				}
				new FilterDialog(this, filter, measurementManager).show();
				break;
			case R.id.item_source:
				// show data sources dialog
				// TODO
//				new DataSourceDialog(this, dataSources, measurementManager)
				new DataSourceDialog(this, null, measurementManager)
						.show();
				break;
			}
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// Update visibility of menu items according to visiblity of the child
		// views
		for (GeoARView view : noiseARViews) {
			if (view.getMenuGroupId() != null) { 
				menu.setGroupVisible(view.getMenuGroupId(), view.isVisible());
			}

		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	private static class UiFragment extends Fragment {
		GeoARFragment mapFragment;
		GeoARFragment arFragment;

		private static UiFragment instance;
		
		private MeasurementManager measureManager;
		private LocationHandler locationHandler;
		private InfoView infoView;
		
		private ImageButton mapARSwitcherButton;
		
		private boolean showMap = true;
		
		static UiFragment newInstance(MeasurementManager measurementManager, 
				 LocationHandler locationHandler){
			instance = new UiFragment();
			instance.setRetainInstance(true);
			instance.measureManager = measurementManager;
			instance.locationHandler = locationHandler;

			return instance;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater.inflate(R.layout.main, container, false);
			
			// AR / Map switcher Button
			mapARSwitcherButton = (ImageButton) v.findViewById(R.id.imageButtonMapARSwitcher);
			mapARSwitcherButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showMap = (showMap == true) ? false : true;
					updateButton();
				} 
			});
			
			return v; 
		}
		
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
						
			FragmentManager fm = getFragmentManager();
			
			// find fragments
			mapFragment = (GeoARFragment) fm.findFragmentById(R.id.fragment_view);
			arFragment = (GeoARFragment) fm.findFragmentById(R.id.fragment_view2);
			
			infoView = (InfoView) getView().findViewById(R.id.infoView);
			FragmentTransaction f = fm.beginTransaction();
			
			if(arFragment == null){
				// AugmentedReality Fragment
				arFragment = new ARFragment(measureManager, 
						locationHandler, infoView);
				
				arFragment.setMeasureManager(measureManager);
				arFragment.setLocationHandler(locationHandler);
				arFragment.setInfoHandler(infoView);
				
				f.add(R.id.fragment_view2, arFragment);
			}
			else{
				arFragment.setInfoHandler(infoView);
			}

			if(mapFragment == null){
				// Map Fragment
				mapFragment = new GeoMapFragment(measureManager, 						
						locationHandler, infoView);
				
				mapFragment.setMeasureManager(measureManager);
				mapFragment.setLocationHandler(locationHandler);
				mapFragment.setInfoHandler(infoView);
				
				f.add(R.id.fragment_view, mapFragment);
			}
			else{
				mapFragment.setInfoHandler(infoView);
			}
			
			f.commit();
			updateButton();
		}

		/**
		 * Sets correct drawable for map/AR switching button
		 */
		private void updateButton(){			
			FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
			if(showMap){
				mapARSwitcherButton.setImageResource(R.drawable.ic_menu_phone);
				fragmentTransaction.hide(arFragment);
				fragmentTransaction.show(mapFragment);
			} else{
				mapARSwitcherButton.setImageResource(R.drawable.ic_menu_mapmode);
				fragmentTransaction.hide(mapFragment);
				fragmentTransaction.show(arFragment);
			}	
			fragmentTransaction.commit();
		}
	}
}