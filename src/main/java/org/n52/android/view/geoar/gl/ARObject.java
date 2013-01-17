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

import javax.microedition.khronos.opengles.GL10;

import org.n52.android.GeoARApplication;
import org.n52.android.R;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.ARVisualization.ItemVisualization;
import org.n52.android.newdata.vis.DataSourceVisualization.DataSourceVisualizationCanvas;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OpenGLCallable;
import org.n52.android.view.geoar.gl.mode.RenderFeature2;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.view.View;

public class ARObject implements OpenGLCallable {

	protected class VisualizationLayer {
		final Class<? extends ItemVisualization> clazz;
		final ItemVisualization itemVisualization;
		private Set<RenderFeature2> renderFeatureList = new HashSet<RenderFeature2>();
		private DataSourceVisualizationCanvas canvasFeature;

		VisualizationLayer(ItemVisualization itemVisualization) {
			this.itemVisualization = itemVisualization;
			this.clazz = itemVisualization.getClass();
		}

		void addRenderFeatures(Collection<RenderFeature2> renderFeatures) {
			renderFeatureList.addAll(renderFeatures);
		}
	}

	/** Model Matrix of this feature */
	private final float[] modelMatrix2 = new float[16];
	/** Model view Matrix of this feature */
	private final float[] modelViewMatrix = new float[16];
	/** Model-View-Projection Matrix of our feature */
	private final float[] mvpMatrix = new float[16];
	/** temporary Matrix for caching */
	private final float[] tmpMatrix = new float[16];

	private float distanceTo;
	private final float[] newPosition = new float[4];
	private final float[] screenCoordinates = new float[3];

	// XXX Why mapping by Class? Compatible with multiinstancedatasources?
	private final Map<Class<? extends ItemVisualization>, VisualizationLayer> visualizationLayers = new HashMap<Class<? extends ItemVisualization>, VisualizationLayer>();
	private final SpatialEntity entity;

	// TODO FIXME XXX task: ARObject gains most functionalities of RenderFeature
	// (-> RenderFeature to be more optional)
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

		/** set the matrices to identity matrix */
		Matrix.setIdentityM(modelMatrix2, 0);
		Matrix.setIdentityM(modelViewMatrix, 0);
		Matrix.setIdentityM(mvpMatrix, 0);
		Matrix.setIdentityM(tmpMatrix, 0);

		// TODO i think position[0] must be translated negatively -> Check
		Matrix.translateM(modelMatrix2, 0, -newPosition[0], newPosition[1],
				newPosition[2]);

		if (parentMatrix != null) {
			Matrix.multiplyMM(tmpMatrix, 0, parentMatrix, 0, modelMatrix2, 0);
			System.arraycopy(tmpMatrix, 0, modelMatrix2, 0, 16);
			Matrix.setIdentityM(tmpMatrix, 0);
		}

		Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix2, 0);
		Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix2, 0);

		// TODO XXX FIXME frustum test
		if (newPosition != null) {
			float[] vec = new float[] {0,0,0,1};
			Matrix.multiplyMV(vec, 0, modelMatrix2, 0, vec, 0);
			if (!GLESCamera.frustumCulling(vec))
				return;
		}
		
		updateScreenCoordinates();

		// TODO XXX FIXME are just active visualizations called !? -> check
		for (VisualizationLayer layer : visualizationLayers.values()) {
			for (RenderFeature2 feature : layer.renderFeatureList) {
				feature.onRender(mvpMatrix);
			}
		}
	}

	public void onItemClicked(Context context) {
		Builder builder = new AlertDialog.Builder(context);
		builder.setTitle("entity").setMessage("entitysnippet")
				.setNeutralButton(R.string.cancel, null);

		// TODO use view caching with convertView parameter
		// FIXME NoSuchElementException
		View featureView = visualizationLayers.values().iterator().next().itemVisualization
				.getFeatureView(entity, null, null, context);

		if (featureView != null) {
			builder.setView(featureView);
		}
		builder.create().show();

	}

	public void updateScreenCoordinates() {
		float[] screenPos = new float[3];
		// TODO FIXME XXX i think newPosition[2] has to be negative
		int result = GLU.gluProject(-newPosition[0], newPosition[1],
				newPosition[2], modelMatrix2, 0, GLESCamera.projectionMatrix,
				0, GLESCamera.viewPortMatrix, 0, screenPos, 0);

		if (result == GL10.GL_TRUE) {
			screenCoordinates[0] = screenPos[0];
			screenCoordinates[1] = GLESCamera.glViewportHeight - screenPos[1];
		}
	}

	public boolean thisObjectHitted(float cx, float cy) {
		float dx = screenCoordinates[0] - cx;
		float dy = screenCoordinates[1] - cy;
		float length = (float) Math.sqrt(dx * dx - dy * dy);

		if (length <= 20) {
			return true;
		} else {
			return false;
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

		newPosition[0] = x[0] / 10f;
		newPosition[1] = (float) (altitude - location.getAltitude());
		// FIXME XXX TODO and here the third position has to be negative i think
		newPosition[2] = z[0] / 10f;

		for (VisualizationLayer layer : visualizationLayers.values()) {
			for (RenderFeature2 renderFeature : layer.renderFeatureList)
				renderFeature.setRelativePosition(newPosition);
		}

		this.newPosition[0] = newPosition[0] - GLESCamera.cameraPosition[0];
		this.newPosition[1] = newPosition[1] - GLESCamera.cameraPosition[1];
		this.newPosition[2] = newPosition[2] - GLESCamera.cameraPosition[2];
	}

	public void addRenderFeature(ItemVisualization itemVisualization,
			Collection<RenderFeature2> features) {
		if (visualizationLayers.containsKey(itemVisualization.getClass())) {
			visualizationLayers.get(itemVisualization.getClass())
					.addRenderFeatures(features);
		} else {
			VisualizationLayer layer = new VisualizationLayer(itemVisualization);
			layer.addRenderFeatures(features);
			// FIXME XXX TODO brauchen wir nicht
			for (RenderFeature2 feature : features) {
				feature.setRelativePosition(newPosition);
			}
			visualizationLayers.put(itemVisualization.getClass(), layer);
		}
	}

	public void addCanvasFeature(ItemVisualization itemVisualization,
			DataSourceVisualizationCanvas canvasFeature) {
		if (canvasFeature == null)
			return;

		if (visualizationLayers.containsKey(itemVisualization.getClass())) {
			visualizationLayers.get(itemVisualization.getClass()).canvasFeature = canvasFeature;
		} else {
			VisualizationLayer layer = new VisualizationLayer(itemVisualization);
			layer.canvasFeature = canvasFeature;
			// for (RenderFeature2 feature : features) {
			// feature.setRelativePosition(newPosition);
			// }
			visualizationLayers.put(itemVisualization.getClass(), layer);
		}
	}

	public void renderCanvas(Paint poiRenderer, Canvas canvas) {
		for (VisualizationLayer layer : visualizationLayers.values()) {
			// FIXME TODO XXX distanceTo has to be in the Settings
			if (distanceTo < 1000)
				for (RenderFeature2 renderFeature : layer.renderFeatureList) {
					layer.canvasFeature.onRender(screenCoordinates[0],
							screenCoordinates[1], canvas);
				}
		}
	}

}
