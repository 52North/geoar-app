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

import java.lang.reflect.Field;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.core.GeoPoint;
import org.n52.android.GeoARActivity3;
import org.n52.android.R;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

//import org.osmdroid.util.GeoPoint;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class GeoMapFragment extends SherlockFragment {

	private GeoMapView geoMapView;

	// "Hack", create DummyMapActivity which passes all relevant
	// Context calls to the implementations of the activity of this
	// fragment
	private class DummyMapActivity extends MapActivity {

		public DummyMapActivity() {
			try {
				// The private _final_ method getApplication has to return the
				// fragment's activity's application, so use reflection to get
				// the mApplication field and change it to that value
				Field applicationField = Activity.class
						.getDeclaredField("mApplication"); // Could change...
				applicationField.setAccessible(true);
				applicationField.set(this, GeoMapFragment.this.getActivity()
						.getApplication());

			} catch (NoSuchFieldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void destroy() {
			onDestroy();

		}

		public void pause() {
			onPause();
		}

		public void resume() {
			onResume();
		}

		@Override
		public Resources getResources() {
			return GeoMapFragment.this.getActivity().getResources();
		}

		@Override
		public ApplicationInfo getApplicationInfo() {
			return GeoMapFragment.this.getActivity().getApplicationInfo();
		}

		@Override
		public Theme getTheme() {
			return GeoMapFragment.this.getActivity().getTheme();
		}

		@Override
		public Context getBaseContext() {
			return GeoMapFragment.this.getActivity().getBaseContext();
		}

		@Override
		public Object getSystemService(String name) {
			return GeoMapFragment.this.getActivity().getSystemService(name);
		}

		@Override
		public SharedPreferences getSharedPreferences(String name, int mode) {
			return GeoMapFragment.this.getActivity().getSharedPreferences(
					name, mode);
		}
	};

	private DummyMapActivity mapActivity;

	// TODO
	// private ManualPositionView positionView;

	// public GeoMapFragment2(){
	// geoARViews = new ArrayList<GeoARView2>();
	// }

	// public GeoMapFragment2(LocationHandler locationHandler, InfoView
	// infoView) {
	public GeoMapFragment() {
		// this();
		// this.mInfoHandler = infoView;
		// this.mLocationHandler = locationHandler;
		setRetainInstance(true);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);

		return inflater.inflate(R.layout.map_fragment, container, false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// Get Layout root to add mapview programmatically
		FrameLayout layout = (FrameLayout) getView().findViewById(
				R.id.frameLayout);

		// Use special map activity as context since it is required by
		// mapsforge's mapview

		// Create Dummy here because it needs the underlying activity

		mapActivity = new DummyMapActivity();
		geoMapView = new GeoMapView(mapActivity);
		// geoMapView = (GeoMapView3) getView().findViewById(R.id.mapview);
		// geoMapView.setLocationHandler(mLocationHandler);
		// geoMapView.setInfoHandler(mInfoHandler);
		// geoARViews.add(geoMapView);
		layout.addView(geoMapView, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);

//		if (savedInstanceState != null) {
//			int lat = savedInstanceState.getInt("lat");
//			int lon = savedInstanceState.getInt("lon");
//			byte zoom = savedInstanceState.getByte("zoom");
//			geoMapView.setCenterAndZoom(new GeoPoint(lat, lon), zoom);
//		}

		// positionView = (ManualPositionView) getView().findViewById(
		// R.id.manual_position_view);
		// positionView.setLocationHandler(mLocationHandler);
		// positionView.setMapView(geoMapView);
		// geoARViews.add(positionView);
	}

	/**
	 * 
	 */
	@Override
	public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
			com.actionbarsherlock.view.MenuInflater inflater) {
		inflater.inflate(R.menu.menu_map, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}


	/**
	 * Cleares the tileprovider and indicates to the VM that it would be a good
	 * time to run the garbage collector.
	 */
	@Override
	public void onDestroy() {
		// TODO destroy mapview
		// geoMapView.getTileProvider().clearTileCache();
		//
		// System.gc();
		mapActivity.destroy();
		super.onDestroy();
	}

	@Override
	public void onPause() {
		mapActivity.pause();
		super.onPause();
	}

	@Override
	public void onResume() {
		mapActivity.resume();
		super.onResume();
	}
}
