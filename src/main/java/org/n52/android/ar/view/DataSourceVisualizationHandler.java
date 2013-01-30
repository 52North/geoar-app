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
package org.n52.android.ar.view;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.R;
import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.DataCache.Cancelable;
import org.n52.android.newdata.DataCache.DataSourceErrorType;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder.DataSourceSettingsChangedListener;
import org.n52.android.newdata.RenderFeatureFactory;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.ARVisualization;
import org.n52.android.newdata.Visualization.ARVisualization.ItemVisualization;
import org.n52.android.newdata.vis.DataSourceVisualization.DataSourceVisualizationGL;
import org.n52.android.utils.GeoLocation;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.n52.android.view.geoar.gl.mode.RenderFeature2;
import org.n52.android.view.geoar.gl.mode.features.CubeFeature2;
import org.n52.android.view.geoar.gl.mode.features.SphereFeature;

public class DataSourceVisualizationHandler implements RenderFeatureFactory,
		DataSourceSettingsChangedListener {

	private GetDataBoundsCallback callback = new GetDataBoundsCallback() {

		@Override
		public void onProgressUpdate(int progress, int maxProgress) {
			InfoView.setProgressTitle(R.string.requesting_data,
					DataSourceVisualizationHandler.this);
			InfoView.setProgress(progress, maxProgress,
					DataSourceVisualizationHandler.this);

		}

		@Override
		public void onAbort(MercatorRect bounds, DataSourceErrorType reason) {
			InfoView.clearProgress(DataSourceVisualizationHandler.this);
			if (reason == DataSourceErrorType.CONNECTION) {
				InfoView.setStatus(R.string.connection_error, 5000,
						DataSourceVisualizationHandler.this);
			} else if (reason == DataSourceErrorType.UNKNOWN) {
				InfoView.setStatus(R.string.unknown_error, 5000,
						DataSourceVisualizationHandler.this);
			}
		}

		@Override
		public void onReceiveDataUpdate(MercatorRect bounds,
				List<? extends SpatialEntity> data) {

			synchronized (mutex) {
				List<ARObject> arObjects = new ArrayList<ARObject>();
				List<ItemVisualization> visualizations = dataSourceInstance
						.getParent()
						.getVisualizations()
						.getCheckedItems(
								ARVisualization.ItemVisualization.class);

				for (SpatialEntity entity : data) {

					for (ItemVisualization visualization : visualizations) {

						List<RenderFeature2> features = new ArrayList<RenderFeature2>();
						for (DataSourceVisualizationGL feature : visualization
								.getEntityVisualization(entity,
										DataSourceVisualizationHandler.this)) {
							features.add((RenderFeature2) feature);
						}
						ARObject arObject = new ARObject(entity,
								visualization, features,
								visualization.getEntityVisualization(entity));
						// TODO maybe just use entity + visualization
						arObjects.add(arObject);
					}

				}

				arView.setARObjects(arObjects, dataSourceInstance);
				currentRect = bounds;
			}
		}
	};

	private DataSourceInstanceHolder dataSourceInstance;
	private Object mutex = new Object();

	// public List<RenderFeature> renderFeatures = new
	// ArrayList<RenderFeature>();
	private GeoLocation currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
	private MercatorRect currentRect;

	private Cancelable currentUpdate;

	private ARView arView;

	public DataSourceVisualizationHandler(ARView arView,
			DataSourceInstanceHolder dataSourceInstance) {
		this.arView = arView;
		this.dataSourceInstance = dataSourceInstance;

		// GeoLocation loc = new GeoLocation(LocationHandler
		// .getLastKnownLocation().getLatitude(), LocationHandler
		// .getLastKnownLocation().getLongitude());
		// setCenter(loc);
	}

	public void setCenter(GeoLocation gPoint) {
		currentCenterGPoint = gPoint;

		// Calculate thresholds for request of data
		double meterPerPixel = MercatorProj.getGroundResolution(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		int pixelRadius = (int) (Settings.SIZE_AR_INTERPOLATION / meterPerPixel);
		int pixelReloadDist = (int) (Settings.RELOAD_DIST_AR / meterPerPixel);

		// Calculate new center point in world coordinates
		int centerPixelX = (int) MercatorProj.transformLonToPixelX(
				gPoint.getLongitudeE6() / 1E6f, Settings.ZOOM_AR);
		int centerPixelY = (int) MercatorProj.transformLatToPixelY(
				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		currentCenterMercator = new MercatorPoint(centerPixelX, centerPixelY,
				Settings.ZOOM_AR);

		// currentGroundResolution = (float) MercatorProj.getGroundResolution(
		// currentCenterGPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
		// Needed for ground raster data

		// determination if data request is needed or if just a simple shift is
		// enough
		boolean requestInterpolation = false;
		if (currentRect == null) {
			// Get new data if there were none before
			requestInterpolation = true;
		} else {
			MercatorPoint interpolationCenter = currentRect.getCenter();
			if (currentCenterMercator.zoom != currentRect.zoom
					|| currentCenterMercator.distanceTo(interpolationCenter) > pixelReloadDist) {
				// request data if new center offsets more than
				// Settings.RELOAD_DIST_AR meters
				requestInterpolation = true;
			}
		}

		if (requestInterpolation) {
			// if new data is needed
			if (currentUpdate != null) {
				// cancel currently running data request
				currentUpdate.cancel();
			}
			// trigger data request
			currentUpdate = dataSourceInstance.getDataCache().getDataByBBox(
					new MercatorRect(currentCenterMercator.x - pixelRadius,
							currentCenterMercator.y - pixelRadius,
							currentCenterMercator.x + pixelRadius,
							currentCenterMercator.y + pixelRadius,
							Settings.ZOOM_AR), callback, false);
		}

	}

	public void clear() {
		synchronized (mutex) {
			// LOG.info(dataSourceInstance.getName() +
			// " clearing ar visualization");
			cancel();
			arView.clearARObjects(dataSourceInstance);
		}
	}

	public void cancel() {
		synchronized (mutex) {
			if (currentUpdate != null) {
				currentUpdate.cancel();
				currentUpdate = null;
			}

		}
	}

	public void destroy() {
		clear();
		dataSourceInstance.removeOnSettingsChangedListener(this);
	}

	@Override
	public void onDataSourceSettingsChanged() {
		if (currentUpdate != null) {
			currentUpdate.cancel();
			currentUpdate = null;
		}
		setCenter(currentCenterGPoint);
	}

	// TODO Move!
	@Override
	public DataSourceVisualizationGL createCube() {
		CubeFeature2 cube = new CubeFeature2();
		return cube;
	}

	@Override
	public DataSourceVisualizationGL createSphere() {
		SphereFeature sphere = new SphereFeature();
		return sphere;
	}

}
