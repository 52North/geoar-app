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
import org.n52.android.view.InfoView;
import org.n52.android.view.camera.NoiseCamera;
import org.n52.android.view.geoar.ARNoiseView;
import org.n52.android.view.geoar.CalibrationControlView;
import org.n52.android.view.geoar.LocationHandler;
import org.n52.android.view.geoar.NoiseARView;
import org.n52.android.view.geoar.NoiseChartView;
import org.n52.android.view.map.GeoMapView;
import org.n52.android.view.map.ManualPositionView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ViewAnimator;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;


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
public class NoiseARActivity extends MapActivity {

	private DataSource[] dataSources = new DataSource[] {
			new NoiseDroidServerSource(), new NoiseDroidLocalSource() };

	private DataSourceAdapter adapter = DataSourceAdapter.getInstance();
	
	private MeasurementManager measurementManager;
	private ViewAnimator viewAnimator;
	private ImageButton mapARSwitcherButton;

	private InfoView infoView;
	private ARNoiseView noiseView;

	private LocationHandler locationHandler;
	// List of NoiseARViews
	private List<NoiseARView> noiseARViews = new ArrayList<NoiseARView>();
	private List<Overlay> mapOverlays;

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
				viewAnimator
						.setDisplayedChild(viewAnimator.getDisplayedChild() == 0 ? 1
								: 0);
				updateButton();
			}
		});

		// Reset camera height if set
		SharedPreferences prefs = getSharedPreferences("NoiseAR", MODE_PRIVATE);
		NoiseCamera.setHeight(prefs.getFloat("cameraHeight", 1.6f));
		
		
		// Find child views, set all common object references and add to
		// noiseARViews list

		// NoiseView
		noiseView = (ARNoiseView) findViewById(R.id.glNoiseView);
		noiseView.setInfoHandler(infoView);
		noiseView.setMeasureManager(measurementManager);
		noiseView.setLocationHandler(locationHandler);
		noiseARViews.add(noiseView);

		// Chart
		NoiseChartView diagramView = (NoiseChartView) findViewById(R.id.noiseDiagramView);
		diagramView.setNoiseGridValueProvider(noiseView
				.getNoiseGridValueProvider());
		noiseARViews.add(diagramView);

		// MapView
		GeoMapView noiseMapView = (GeoMapView) findViewById(R.id.noiseMapView);
		noiseMapView.setInfoHandler(infoView);
		noiseMapView.setMeasureManager(measurementManager);
		noiseMapView.setLocationHandler(locationHandler);
		noiseARViews.add(noiseMapView);

		// Manual Position View
		ManualPositionView positionView = (ManualPositionView) findViewById(R.id.manualPositionView);
		positionView.setLocationHandler(locationHandler);
		positionView.setMapView(noiseMapView);
		noiseARViews.add(positionView);

		// Calibration View
		CalibrationControlView calibrationView = (CalibrationControlView) findViewById(R.id.calibrationView);
		noiseARViews.add(calibrationView);

		// Reset previous state of application when it was hidden for another.
		viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
		if (savedInstanceState != null) {
			viewAnimator.setDisplayedChild(savedInstanceState.getInt(
					"viewAnimatorState", 0));
			for (NoiseARView arView : noiseARViews) {
				arView.onRestoreInstanceState(savedInstanceState);
				if (arView instanceof View) {
					View view = (View) arView;
					String key = arView.getClass().getName() + "visibility";
					if (savedInstanceState.get(key) != null) {
						view.setVisibility(savedInstanceState.getInt(key));
					}
				}
			}
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

		updateButton();

	}

	/**
	 * Sets correct drawable for map/AR switching button
	 */
	private void updateButton() {
		if (viewAnimator.getDisplayedChild() == 0) {
			mapARSwitcherButton.setImageResource(R.drawable.ic_menu_phone);
		} else {
			mapARSwitcherButton.setImageResource(R.drawable.ic_menu_mapmode);
		}
	}

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();
		Window window = getWindow();
		window.setFormat(PixelFormat.TRANSLUCENT);
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		// save state, whether map or AR view is visible
		outState.putInt("viewAnimatorState", viewAnimator.getDisplayedChild());
		for (NoiseARView arView : noiseARViews) {
			arView.onSaveInstanceState(outState);
			if (arView instanceof View) {
				// if there is a real View, save its visibility
				View view = (View) arView;
				outState.putInt(arView.getClass().getName() + "visibility",
						view.getVisibility());
			}
		}

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
		editor.putFloat("cameraHeight", NoiseCamera.height);
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
}