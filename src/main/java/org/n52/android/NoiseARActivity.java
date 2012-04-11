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
package org.n52.android;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.data.DataSource;
import org.n52.android.data.DataSourceAdapter;
import org.n52.android.data.MeasurementFilter;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.noise.NoiseDroidLocalSource;
import org.n52.android.data.noise.NoiseDroidServerSource;
import org.n52.android.dialog.DataSourceDialog;
import org.n52.android.dialog.FilterDialog;
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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
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
 * @author Holger Hopmann
 * 
 */
public class NoiseARActivity extends Activity {

	private DataSource[] dataSources = new DataSource[] {
			new NoiseDroidServerSource(), new NoiseDroidLocalSource() };
	
	private DataSourceAdapter adapter = DataSourceAdapter.getInstance();
	
	private MeasurementManager measurementManager;
	private ImageButton mapARSwitcherButton;

	private InfoView infoView;
	
	private LocationHandler locationHandler;
	// List of NoiseARViews
	private List<NoiseARView> noiseARViews = new ArrayList<NoiseARView>();
	private List<GeoARFragment> geoARFragments = new ArrayList<GeoARFragment>();
	
	private GeoMapFragment mapFragment;
	private ARFragment arFragment;
	
	private boolean showMap;
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		// Lets measurementManager survive a screen orientation change, so that
		// no measurements need to get recached
		return measurementManager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		infoView = (InfoView) findViewById(R.id.infoView);
		locationHandler = new LocationHandler(this, infoView);

		// Get MeasurementManager from previous instance or create new one
		Object lastMeasureManager = getLastNonConfigurationInstance();
		if (lastMeasureManager != null) {
			measurementManager = (MeasurementManager) lastMeasureManager;
		} else {
			measurementManager = adapter.CreateMeasurementManager();
		}

		// Buttons
		mapARSwitcherButton = (ImageButton) findViewById(R.id.imageButtonMapARSwitcher);
		mapARSwitcherButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showMap = (showMap == true) ? false : true;
				updateButton();
			}
		});

		// Reset camera height if set
		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		RealityCamera.setHeight(prefs.getFloat("cameraHeight", 1.6f));
		
		
		FragmentManager fragmentManager = getFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		
		// Check to see if we have retained the fragments.
		mapFragment = (GeoMapFragment) fragmentManager.findFragmentById(R.id.fragment_view);
		arFragment = (ARFragment) fragmentManager.findFragmentById(R.id.fragment_view2);

		// If not retained (or first time running), we need to create it.
		if(mapFragment == null || arFragment == null){
			// MapFragment
			mapFragment = new GeoMapFragment(measurementManager, 
					locationHandler, infoView);
			geoARFragments.add(mapFragment);
			
			// AugmentedReality Fragment
			arFragment = new ARFragment(measurementManager, 
					locationHandler, infoView);
			geoARFragments.add(arFragment);
			
			fragmentTransaction.add(R.id.fragment_view, mapFragment );
			fragmentTransaction.add(R.id.fragment_view2, arFragment );
			
		} else {
			mapFragment.setMeasureManager(measurementManager);
			mapFragment.setLocationHandler(locationHandler);
			mapFragment.setInfoHandler(infoView);
			geoARFragments.add(mapFragment);
			
			arFragment.setMeasureManager(measurementManager);
			arFragment.setLocationHandler(locationHandler);
			arFragment.setInfoHandler(infoView);
			geoARFragments.add(arFragment);
		}

		
		
		if (savedInstanceState != null) {
			showMap = savedInstanceState.getBoolean("showMap", showMap);

			for (GeoARFragment f : geoARFragments)
				f.onRestoreInstanceState(savedInstanceState);

			// restore manual positioning
			locationHandler.onRestoreInstanceState(savedInstanceState);
		} else {
			showMap = true;
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.info_use);
			builder.setCancelable(true);
			builder.setPositiveButton(R.string.ok, null);
			builder.setTitle(R.string.advice);
			builder.show();
		}
		Log.d("oh yea", "" + showMap);
		fragmentTransaction.commit();

		updateButton();

	}

	/**
	 * Sets correct drawable for map/AR switching button
	 */
	private void updateButton() {
		FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
		fragmentTransaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
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

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.TRANSLUCENT);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save state, whether map or AR fragment is visible
		outState.putBoolean("showMap", showMap);
		for (GeoARFragment f : geoARFragments)
			f.onSaveInstanceState(outState);
		
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
		locationHandler.onPause();
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
			for (NoiseARView view : noiseARViews) {
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
					filter = adapter.CreateMeasurementFilter();// new MeasurementFilter();
				}
				new FilterDialog(this, filter, measurementManager).show();
				break;
			case R.id.item_source:
				// show data sources dialog
				new DataSourceDialog(this, dataSources, measurementManager)
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
		for (NoiseARView view : noiseARViews) {
			if (view.getMenuGroupId() != null) {
				menu.setGroupVisible(view.getMenuGroupId(), view.isVisible());
			}

		}
		return super.onPrepareOptionsMenu(menu);
	}
	
	private static class UiFragment extends Fragment {

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onActivityCreated(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			return super.onCreateView(inflater, container, savedInstanceState);
		}
		
	}
}