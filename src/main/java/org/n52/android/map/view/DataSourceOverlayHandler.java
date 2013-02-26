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
package org.n52.android.map.view;

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.core.GeoPoint;
import org.n52.android.R;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.DataCache.DataSourceErrorType;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataCache.Cancelable;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder.DataSourceSettingsChangedListener;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.MapVisualization.ItemVisualization;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.graphics.Point;
import android.os.Handler;
import android.view.MotionEvent;

/**
 * @author Holger Hopmann
 * @author Arne de Wall
 * 
 */
public class DataSourceOverlayHandler implements
		DataSourceSettingsChangedListener {

	/**
	 * An instance of this class describes a measurement request order which
	 * will be performed in the future, just to make sure that the user will
	 * stop navigating through the map. It ensures that it wont alter this
	 * overlays data if it got canceled before
	 * 
	 */
	private class UpdateHolder implements Runnable {
		private boolean canceled;
		private MercatorRect bounds;
		// protected MapView mapView;
		private Cancelable requestHolder;

		private GetDataBoundsCallback callback = new GetDataBoundsCallback() {

			@Override
			public void onProgressUpdate(int progress, int maxProgress) {
				InfoView.setProgressTitle(R.string.requesting_data,
						UpdateHolder.this); // TODO
				InfoView.setProgress(progress, maxProgress, UpdateHolder.this);

				// if (infoHandler != null) {
				// String stepTitle = "";
				// switch(step){
				// case InfoView.STEP_CLUSTERING:
				// stepTitle = "Clustering";
				// break;
				// case InfoView.STEP_INTERPOLATION:
				// stepTitle = "Interpolation";
				// break;
				// case MeasurementManager.STEP_REQUEST:
				// stepTitle = "Messungsabfrage";
				// break;
				//
				// }

				// infoHandler.setProgressTitle(stepTitle, UpdateHolder.this);
				// infoHandler.setProgress(progress, maxProgress,
				// UpdateHolder.this);
				// }

			}

			@Override
			public void onAbort(MercatorRect bounds, DataSourceErrorType reason) {
				canceled = true;

				InfoView.clearProgress(UpdateHolder.this);
				if (reason == DataSourceErrorType.CONNECTION) {
					InfoView.setStatus(R.string.connection_error, 5000,
							UpdateHolder.this);
				} else if (reason == DataSourceErrorType.UNKNOWN) {
					InfoView.setStatus(R.string.unknown_error, 5000,
							UpdateHolder.this);
				}
			}

			@Override
			public void onReceiveDataUpdate(MercatorRect bounds,
					List<? extends SpatialEntity> data) {
				if (!canceled) {
					synchronized (updateLock) {
						List<VisualizationOverlayItem> overlayItems = new ArrayList<VisualizationOverlayItem>();
						List<ItemVisualization> visualizations = dataSourceInstance
								.getParent().getVisualizations()
								.getCheckedItems(ItemVisualization.class);

						for (SpatialEntity entity : data) {
							GeoPoint point = new GeoPoint(entity.getLatitude(),
									entity.getLongitude());

							for (ItemVisualization visualization : visualizations) {
								VisualizationOverlayItem overlayItem = new VisualizationOverlayItem(
										point, visualization.getTitle(entity),
										visualization.getDescription(entity),
										visualization
												.getDrawableForEntity(entity),
										entity, visualization);
								overlayItems.add(overlayItem);
							}
						}
						overlay.setOverlayItems(overlayItems,
								dataSourceInstance);

						// data received, now this object represents
						// the current data
						currentUpdate = UpdateHolder.this;
						nextUpdate = null;
					}
				}

			}
		};

		private UpdateHolder(MercatorRect bounds) {
			this.bounds = bounds;
		}

		public void cancel() {
			synchronized (updateLock) {
				if (requestHolder != null) {
					requestHolder.cancel();
				}
				canceled = true;
			}
		}

		@Override
		public void run() {
			synchronized (updateLock) {
				if (!canceled) {
					if (bounds.zoom >= dataSourceInstance.getParent()
							.getMinZoomLevel()) {
						// Just runs if zoom is in range
						requestHolder = dataSourceInstance.getDataCache()
								.getDataByBBox(bounds, callback, false);
					} else {
						InfoView.setStatus(R.string.not_zoomed_in, 5000, this);
					}
				}
			}

		}
	}

	private UpdateHolder currentUpdate;
	private UpdateHolder nextUpdate;
	private Handler updateHandler = new Handler();

	private Object updateLock = new Object();

	private DataSourcesOverlay overlay;
	private DataSourceInstanceHolder dataSourceInstance;

	private static final Logger LOG = LoggerFactory
			.getLogger(DataSourceOverlayHandler.class);

	public DataSourceOverlayHandler(DataSourcesOverlay overlay,
			DataSourceInstanceHolder dataSource) {
		this.overlay = overlay;
		this.dataSourceInstance = dataSource;
		dataSource.addOnSettingsChangedListener(this);
	}

	public DataSourceInstanceHolder getDataSource() {
		return dataSourceInstance;
	}

	public void clear() {
		synchronized (updateLock) {
			LOG.info(dataSourceInstance.getName() + " clearing map overlay");
			cancel();
			overlay.clear(dataSourceInstance);
		}
	}

	public void cancel() {
		synchronized (updateLock) {
			if (nextUpdate != null) {
				updateHandler.removeCallbacks(nextUpdate);
				nextUpdate.cancel();
				nextUpdate = null;
				LOG.info(dataSourceInstance.getName() + " map overlay canceled");
			}

		}
	}

	/**
	 * Updates the interpolation data of this overlay. Computes the current
	 * bounds and compares them to the existing interpolation data. Data will be
	 * requested in future if needed
	 * 
	 * @param mapView
	 * @param force
	 *            request data without verification
	 */
	public void updateOverlay(MapView mapView, boolean force) {
		byte zoom = mapView.getMapPosition().getZoomLevel();

		zoom = (byte) Math.min(zoom, dataSourceInstance.getParent()
				.getMaxZoomLevel());
		Projection proj = mapView.getProjection();

		GeoPoint gPoint = (GeoPoint) proj.fromPixels(0, 0);
		if (gPoint == null)
			return; // mapview not yet displayed

		Point point = proj.toPoint(gPoint, null, zoom);

		// int x = (int) MercatorProj.transformLonToPixelX(
		// gPoint.getLongitudeE6() / 1E6f, zoom);
		// int y = (int) MercatorProj.transformLatToPixelY(
		// gPoint.getLatitudeE6() / 1E6f, zoom);

		synchronized (updateLock) {
			MercatorRect newBounds = new MercatorRect(point.x, point.y, point.x
					+ mapView.getWidth(), point.y + mapView.getHeight(), zoom);

			int updateDelay = -1;

			if (currentUpdate == null
					|| !currentUpdate.bounds.contains(newBounds)) {
				// no data or data outside viewport
				updateDelay = 1000;
			}
			if (currentUpdate != null
					&& currentUpdate.bounds.contains(newBounds)
					&& zoom != currentUpdate.bounds.zoom) {
				// data inside viewport, but different zoom
				updateDelay = 5000;
			}

			if (updateDelay >= 0 || force) {
				// Queue next update
				newBounds.expand(Settings.BUFFER_MAPINTERPOLATION,
						Settings.BUFFER_MAPINTERPOLATION);
				if (nextUpdate != null) {
					nextUpdate.cancel();
					updateHandler.removeCallbacks(nextUpdate);
				}
				nextUpdate = new UpdateHolder(newBounds);
				updateHandler.postDelayed(nextUpdate, Math.max(0, updateDelay));
				LOG.debug("Overlay Update in " + updateDelay / 1000 + " s");
			}
		}
	}

	public void destroy() {
		clear();
		dataSourceInstance.removeOnSettingsChangedListener(this);
	}

	@Override
	public void onDataSourceSettingsChanged() {
		if (nextUpdate != null) {
			// There is already an update pending
			return;
		}
		if (currentUpdate != null) {
			// repeat last update with new settings
			updateHandler.removeCallbacks(currentUpdate);
			updateHandler.post(currentUpdate);
		}
	}
}
