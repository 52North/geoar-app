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

import java.util.ArrayList;
import java.util.List;

import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.Projection;
import org.mapsforge.android.maps.overlay.OverlayItem;
import org.mapsforge.core.GeoPoint;
import org.metalev.multitouch.controller.MultiTouchController.PositionAndScale;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataCache.RequestHolder;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.MapVisualization.ItemVisualization;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;

import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

/**
 * @author Holger Hopmann
 * @author Arne de Wall
 * 
 */
public class DataSourceOverlayHandler {

	public interface OnProgressUpdateListener extends
			org.n52.android.newdata.DataCache.OnProgressUpdateListener {
		void onAbort(int reason);
	}

	/**
	 * An instance of this class describes a measurement request order which
	 * will be performed in the future, just to make sure that the user will
	 * stop navigating through the map. It ensures that it wont alter this
	 * overlays data if it got canceled before
	 * 
	 */
	protected class UpdateHolder implements Runnable {
		protected boolean canceled;
		protected MercatorRect bounds;
		// protected MapView mapView;
		protected RequestHolder requestHolder;

		protected GetDataBoundsCallback callback = new GetDataBoundsCallback() {

			@Override
			public void onProgressUpdate(int progress, int maxProgress, int step) {

				InfoView.setProgressTitle("Requesting " + dataSource.getName(),
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
			public void onAbort(MercatorRect bounds, int reason) {
				canceled = true;

				InfoView.clearProgress(UpdateHolder.this);

				// if (infoHandler != null) {
				// // inform user of aborting reason
				// infoHandler.clearProgress(UpdateHolder.this);
				// if (reason == MeasurementManager.ABORT_NO_CONNECTION) {
				// infoHandler.setStatus(R.string.connection_error, 5000,
				// UpdateHolder.this);
				// } else if (reason == MeasurementManager.ABORT_UNKOWN) {
				// infoHandler.setStatus(R.string.unkown_error, 5000,
				// UpdateHolder.this);
				// }
				// }
			}

			@Override
			public void onReceiveDataUpdate(MercatorRect bounds,
					List<? extends SpatialEntity> data) {
				synchronized (updateLock) {
					if (!canceled) {
						List<OverlayItem> overlayItems = new ArrayList<OverlayItem>();
						List<ItemVisualization> visualizations = dataSource
								.getVisualizations().getCheckedItems(
										ItemVisualization.class);

						for (SpatialEntity entity : data) {
							GeoPoint point = new GeoPoint(entity.getLatitude(),
									entity.getmLongitude());

							for (ItemVisualization visualization : visualizations) {
								OverlayItem overlayItem = new OverlayItem(
										point, visualization.getTitle(entity),
										visualization.getDescription(entity),
										visualization
												.getDrawableForEntity(entity));
								overlayItems.add(overlayItem);
							}
						}
						overlay.setOverlayItems(overlayItems, dataSource);

						// interpolation received, now this object represents
						// the current interpolation
						currentUpdate = UpdateHolder.this;
						nextUpdate = null;
					}
				}

			}
		};

		public UpdateHolder(MercatorRect bounds) {
			this.bounds = bounds;
			// this.mapView = mapView;
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
					if (bounds.zoom >= Settings.MIN_ZOOM_MAPINTERPOLATION) {
						// Just runs if zoom is in range
						requestHolder = dataSource.getDataCache()
								.getDataByBBox(bounds, callback, false);
					} else {
						InfoView.setStatus("Daten erfordern höhere Zoomstufe",
								5000, this);
						// infoHandler.setStatus(R.string.not_zoomed_in, 5000,
						// this);
					}
				}
			}

		}
	}

	protected UpdateHolder currentUpdate;
	protected UpdateHolder nextUpdate;
	protected Handler updateHandler = new Handler();

	protected Object updateLock = new Object();

	protected DataSourcesOverlay overlay;
	// protected ItemizedDataOverlay<OverlayItem> itemizedOverlay;

	// protected int cacheWidth;
	// protected int cacheHeight;
	// protected InfoView infoHandler;
	// protected Paint paint;
	// protected Drawable itemizedDrawable;

	// private MapView mapView;
	private DataSourceHolder dataSource;

	public DataSourceOverlayHandler(DataSourcesOverlay overlay,
			DataSourceHolder dataSource) {
		this.overlay = overlay;
		this.dataSource = dataSource;
	}

	public DataSourceHolder getDataSource() {
		return dataSource;
	}

	public void onTouchEvent(android.view.MotionEvent e, MapView mapView) {
		if (e.getAction() == MotionEvent.ACTION_UP
				|| e.getAction() == MotionEvent.ACTION_CANCEL) {
			updateOverlay(mapView, false);
		}
	};

	public void clear() {
		synchronized (updateLock) {
			Log.d("GeoAR", dataSource.getName() + " clearing map overlay");
			cancel();
			overlay.clear(dataSource);
		}
	}

	public void cancel() {
		synchronized (updateLock) {
			if (nextUpdate != null) {
				updateHandler.removeCallbacks(nextUpdate);
				nextUpdate.cancel();
				Log.d("GeoAR", dataSource.getName() + " map overlay canceled");
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
		byte zoom = (byte) (mapView.getMapPosition().getZoomLevel() - 1);

		zoom = (byte) Math.min(zoom, Settings.MAX_ZOOM_MAPINTERPOLATION);
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
				updateDelay = 2500;
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
				Log.i("GeoAR", "Overlay Update in " + updateDelay / 1000 + " s");
			}
		}
	}
}
