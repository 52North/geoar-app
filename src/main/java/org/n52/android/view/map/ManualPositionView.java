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
package org.n52.android.view.map;

import org.n52.android.GeoARView;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.InfoView;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * A simple LinearLayout offering a user interface to change/set the manual
 * positioning
 * 
 * @author Holger Hopmann
 * 
 */
public class ManualPositionView extends LinearLayout implements GeoARView,
		OnClickListener {

	private GeoMapView mapView;
	private LocationHandler locationHandler;

	public ManualPositionView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.manual_position_views,
				this, true);

		// Find child views
		Button okButton = (Button) findViewById(R.id.buttonOk);
		Button removeButton = (Button) findViewById(R.id.buttonRemove);

		// Set click listeners
		removeButton.setOnClickListener(this);
		okButton.setOnClickListener(this);
	}

	public void setMapView(GeoMapView mapView) {
		this.mapView = mapView;
	}

	public void setLocationHandler(LocationHandler locationHandler) {
		this.locationHandler = locationHandler;
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		if (mapView != null) {
			if (isShown()) {
				// If view is really shown, set mapview into manual positioning
				// mode
				mapView.setManualPositioning(true);
			} else {
				mapView.setManualPositioning(false);
			}
		}
		super.onVisibilityChanged(changedView, visibility);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.buttonOk:
			setVisibility(GONE);
			break;
		case R.id.buttonRemove:
			locationHandler.disableManualLocation();
			setVisibility(GONE);
			break;

		}
	}

	public boolean isVisible() {
		return isShown();
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_set_location:
			setVisibility(VISIBLE);
			return true;
		}
		return false;
	}

	public Integer getMenuGroupId() {
		return null;
	}

	public void setInfoHandler(InfoView infoHandler) {
	}

	public void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub

	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMeasureManager(MeasurementManager measureManager) {
		// TODO Auto-generated method stub
		
	}

}
