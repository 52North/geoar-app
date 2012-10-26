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
package org.n52.android.view;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.GeoARView;
import org.n52.android.GeoARView2;
import org.n52.android.data.MeasurementManager;
import org.n52.android.tracking.location.LocationHandler;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

public abstract class GeoARFragment2 extends Fragment {

    protected InfoView mInfoHandler;
    protected LocationHandler mLocationHandler;

    protected List<GeoARView2> geoARViews = new ArrayList<GeoARView2>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setHasOptionsMenu(true);

	if (savedInstanceState != null)
	    for (GeoARView2 arView : geoARViews) {
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
	for (GeoARView2 v : geoARViews)
	    if (v.onOptionsItemSelected(item))
		return true;

	return super.onOptionsItemSelected(item);
    }

    public void setInfoHandler(InfoView infoHandler) {
	this.mInfoHandler = infoHandler;
	for (GeoARView2 v : geoARViews) {
	    v.setInfoHandler(infoHandler);
	}
    }

    public void setLocationHandler(LocationHandler locationHandler) {
	this.mLocationHandler = locationHandler;
	for (GeoARView2 v : geoARViews) {
	    v.setLocationHandler(locationHandler);
	}
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
	super.onSaveInstanceState(outState);

	for (GeoARView2 arView : geoARViews) {
	    arView.onSaveInstanceState(outState);

	    if (arView instanceof View) {
		// if there is a real View, save its visibility
		View view = (View) arView;
		outState.putInt(arView.getClass().getName() + "visibility", view.getVisibility());
	    }
	}
    }
}
