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
package org.n52.android.view.geoar.gl;

import org.n52.android.newdata.SpatialEntity;
import org.n52.android.view.geoar.gl.mode.RenderFeature;

import android.location.Location;

public class ARObject {

	public interface Conditions {
		boolean nearVisualization();
		boolean farVisualization();
	}
	
	private float distanceTo;

	private RenderFeature renderFeature;
	private SpatialEntity entity;

	
	public ARObject(SpatialEntity entity){
		this.entity = entity;
	}
	
	public void onRender(float[] projectionMatrix, float[] viewMatrix, float[] parentMatrix){
		// TODO different visualizations
		if(renderFeature != null)
			renderFeature.onRender(projectionMatrix, viewMatrix, parentMatrix);
	}

	public void onLocationUpdate(Location location) {
		if (entity == null)
			return;

		final double longitude = entity.getLongitude();
		final double latitude = entity.getLatitude();
		int altitude = entity.getAltitude();
		
		/** calc the distance */
		final float[] x = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), latitude, longitude, x);
		distanceTo = x[0];
		
		// just want the distance -> length 1
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), location.getLatitude(), longitude, x);
		// just want the distance -> length 1
		final float[] z = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), latitude, location.getLongitude(), z);

		// correct the direction according to the poi location, because we just
		// get the distance in x and z direction
		if (location.getLongitude() < longitude)
			x[0] *= -1;
		if (location.getLatitude() < latitude)
			z[0] *= -1;
		if (altitude == 0)
			altitude = (int) location.getAltitude();
		// testen
		renderFeature
				.setPosition(new float[] { (float) x[0] / 10f,
						(float) (altitude - location.getAltitude()),
						(float) z[0] / 10f});
	}
}
