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
package org.n52.geoar.map.view;

import org.mapsforge.android.maps.MapView;

import android.content.Context;

/**
 * Extension of {@link MapView} by a zoom event.
 */
public class GeoARMapView extends MapView {

	interface OnZoomChangeListener {
		void onZoomChange();
	}

	private OnZoomChangeListener mZoomChangeListener;

	public GeoARMapView(Context context) {
		super(context);
	}

	public void setOnZoomChangeListener(OnZoomChangeListener zoomChangeListener) {
		this.mZoomChangeListener = zoomChangeListener;
	}

	@Override
	public boolean zoom(byte arg0, float arg1) {
		boolean returnValue = super.zoom(arg0, arg1);
		if (mZoomChangeListener != null) {
			post(new Runnable() {
				@Override
				public void run() {
					// XXX
					// Posting a Runnable ensures that event handlers will be
					// called after zoom got handled by mapsforge API
					if (mZoomChangeListener != null) {
						mZoomChangeListener.onZoomChange();
					}
				}
			});
		}
		return returnValue;
	}

}
