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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.ARVisualization.ItemVisualization;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OpenGLCallable;
import org.n52.android.view.geoar.gl.mode.RenderFeature2;

import android.location.Location;

public class ARObject implements OpenGLCallable {

	public interface Conditions {
		boolean nearVisualization();

		boolean farVisualization();
	}

	protected class VisualizationLayer {
		final Class<? extends ItemVisualization> clazz;
		private Set<RenderFeature2> renderFeatureList = new HashSet<RenderFeature2>();

		VisualizationLayer(final Class<? extends ItemVisualization> clazz) {
			this.clazz = clazz;
		}

		void addRenderFeatures(Collection<RenderFeature2> renderFeatures) {
			renderFeatureList.addAll(renderFeatures);
		}
	}

	private float distanceTo;
	
	private final Map<Class<? extends ItemVisualization>, VisualizationLayer> 
		visualizationLayers = new HashMap<Class<? extends ItemVisualization>, VisualizationLayer>();
	private final SpatialEntity entity;
	

	public ARObject(SpatialEntity entity) {
		this.entity = entity;
		onLocationUpdate(LocationHandler.getLastKnownLocation());
	}

	@Override
	public void onPreRender() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRender(float[] projectionMatrix, float[] viewMatrix,
			float[] parentMatrix) {
		for(VisualizationLayer layer : visualizationLayers.values()){
			for(RenderFeature2 renderFeature : layer.renderFeatureList){
				renderFeature.onRender(projectionMatrix, viewMatrix, parentMatrix);
			}
		}
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

		/** just the distance -> length 1 */
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), location.getLatitude(), longitude, x);
		
		/** just the distance -> length 1 */
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

		float[] newPosition = new float[] { (float) x[0] / 10f,
				(float) (altitude - location.getAltitude()), (float) z[0] / 10f };
		
		for(VisualizationLayer layer : visualizationLayers.values()){
			for(RenderFeature2 renderFeature : layer.renderFeatureList)
				renderFeature.setPosition(newPosition);
		}
		
	}

	public void add(Class<? extends ItemVisualization> class1,
			Collection<RenderFeature2> features) {
		if (visualizationLayers.containsKey(class1)) {
			visualizationLayers.get(class1).addRenderFeatures(features);
		} else {
			VisualizationLayer layer = new VisualizationLayer(class1);
			layer.addRenderFeatures(features);
		}
	}

}
