/**
 * Copyright 2012 52�North Initiative for Geospatial Open Source Software GmbH
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

import java.util.ArrayList;
import java.util.List;

import org.n52.android.R;
import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.DataCache;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataCache.RequestHolder;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.RenderFeatureFactory;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.ARVisualization.ItemVisualization;
import org.n52.android.newdata.gl.primitives.DataSourceRenderable;
import org.n52.android.newdata.gl.primitives.RenderLoader;
import org.n52.android.utils.GeoLocation;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer.OpenGLCallable;
import org.n52.android.view.geoar.gl.mode.RenderFeature;
import org.n52.android.view.geoar.gl.mode.features.CubeFeature;
import org.osmdroid.util.GeoPoint;

import android.opengl.GLSurfaceView;

public class DataSourceVisualizationHandler implements RenderFeatureFactory {

	public interface OnProgressUpdateListener extends
			org.n52.android.newdata.DataCache.OnProgressUpdateListener {
		void onAbort(int reason);
	}

	protected GetDataBoundsCallback callback = new GetDataBoundsCallback() {

		@Override
		public void onProgressUpdate(int progress, int maxProgress, int step) {
			String stepTitle = "";
			switch (step) {
			// case NoiseInterpolation.STEP_CLUSTERING:
			// stepTitle = mContext.getString(R.string.clustering);
			// break;
			// case NoiseInterpolation.STEP_INTERPOLATION:
			// stepTitle =
			// mContext.getString(R.string.interpolation);
			// break;
			case DataCache.STEP_REQUEST:
				stepTitle = "Request Data";
				break;
			}

			InfoView.setProgressTitle(stepTitle, this);
			InfoView.setProgress(progress, maxProgress, this);

		}

		@Override
		public void onAbort(MercatorRect bounds, int reason) {
			InfoView.clearProgress(DataSourceVisualizationHandler.class);
			if (reason == DataCache.ABORT_NO_CONNECTION) {
				InfoView.setStatus(R.string.connection_error, 5000, this);
			} else if (reason == DataCache.ABORT_UNKOWN) {
				InfoView.setStatus(R.string.unkown_error, 5000, this);
			}
		}

		@Override
		public void onReceiveDataUpdate(MercatorRect bounds,
				List<? extends SpatialEntity> data) {
			synchronized (mutex) {

				List<RenderFeature> renderFeatures = new ArrayList<RenderFeature>();
				List<ItemVisualization> visualizations = dataSourceHolder
						.getVisualizations().getCheckedItems(
								ItemVisualization.class);

				for (SpatialEntity entity : data) {
					for (ItemVisualization visualization : visualizations) {
						RenderFeature feature = (RenderFeature) visualization
								.getEntityVisualization(entity,
										DataSourceVisualizationHandler.this);
						feature.setEntity(entity);
						enqueueRenderable(feature);
						renderFeatures.add(feature);
					}
				}

				DataSourceVisualizationHandler.this.renderFeatures = renderFeatures;
			}
		}
	};

	private DataSourceHolder dataSourceHolder;
	protected Object mutex = new Object();
	protected ARVisualizationFactory visFactory;

	protected final GLSurfaceView glSurfaceView;

	public List<RenderFeature> renderFeatures = new ArrayList<RenderFeature>();
	private GeoLocation currentCenterGPoint;
	private MercatorPoint currentCenterMercator;
	private float currentGroundResolution;
	private MercatorRect currentRect;

	private RequestHolder currentUpdate;

	public DataSourceVisualizationHandler(final GLSurfaceView glSurfaceView,
			DataSourceHolder dataSource,
			ARVisualizationFactory visualizationFactory) {
		this.glSurfaceView = glSurfaceView;
		this.visFactory = visualizationFactory;
		this.dataSourceHolder = dataSource;
	}

	public void clear() {
		synchronized (mutex) {
			renderFeatures.clear();
		}
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

		currentGroundResolution = (float) MercatorProj.getGroundResolution(
				currentCenterGPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);

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
			currentUpdate = dataSourceHolder.getDataCache().getDataByBBox(
					new MercatorRect(currentCenterMercator.x - pixelRadius,
							currentCenterMercator.y - pixelRadius,
							currentCenterMercator.x + pixelRadius,
							currentCenterMercator.y + pixelRadius,
							Settings.ZOOM_AR), callback, false);
		}

		// for (GeoLocationUpdateListener r : geoLocationUpdateListener) {
		// r.onGeoLocationUpdate(currentCenterGPoint);
		// }

	}

	private void enqueueRenderable(final OpenGLCallable renderNode) {
		this.glSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				renderNode.onCreateInGLESThread();
			}
		});
	}

	public void reload() {

	}

	public DataSourceHolder getDataSourceHolder() {
		return dataSourceHolder;
	}

	@Override
	public DataSourceRenderable createCube() {
		CubeFeature cube = new CubeFeature();
		return cube;
	}

	@Override
	public DataSourceRenderable createSphere() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataSourceRenderable createRenderable(RenderLoader renderLoader) {
		// TODO Auto-generated method stub
		return null;
	}
}
