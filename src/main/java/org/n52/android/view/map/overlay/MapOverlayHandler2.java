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
package org.n52.android.view.map.overlay;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.alg.InterpolationProvider;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.Measurement;
import org.n52.android.geoar.R;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataCache.RequestHolder;
import org.n52.android.newdata.Visualization;
import org.n52.android.newdata.Visualization.MapVisualization;
import org.n52.android.newdata.Visualization.MapVisualization.ItemVisualization;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.OverlayItem;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

/**
 * @author Holger Hopmann
 * @author Arne de Wall
 * 
 */
public class MapOverlayHandler2 {

	public interface OnProgressUpdateListener extends
			org.n52.android.alg.OnProgressUpdateListener {
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
		protected MapView mapView;
		protected RequestHolder requestHolder;

		protected GetDataBoundsCallback callback = new GetDataBoundsCallback() {

			@Override
			public void onProgressUpdate(int progress, int maxProgress, int step) {
				if (infoHandler != null) {
					String stepTitle = "";
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

					infoHandler.setProgressTitle(stepTitle, UpdateHolder.this);
					infoHandler.setProgress(progress, maxProgress,
							UpdateHolder.this);
				}

			}

			@Override
			public void onAbort(MercatorRect bounds, int reason) {
				canceled = true;

				if (infoHandler != null) {
					// // inform user of aborting reason
					// infoHandler.clearProgress(UpdateHolder.this);
					// if (reason == MeasurementManager.ABORT_NO_CONNECTION) {
					// infoHandler.setStatus(R.string.connection_error, 5000,
					// UpdateHolder.this);
					// } else if (reason == MeasurementManager.ABORT_UNKOWN) {
					// infoHandler.setStatus(R.string.unkown_error, 5000,
					// UpdateHolder.this);
					// }
				}
			}

			@Override
			public void onReceiveDataUpdate(MercatorRect bounds,
					List<SpatialEntity> data) {

				if (!canceled) {
					synchronized (interpolationLock) {
						List<OverlayItem> overlayerItems = new ArrayList<OverlayItem>();
						List<ItemVisualization> visualizations = dataSource
								.getVisualizations().getCheckedItems(
										ItemVisualization.class);

						for (SpatialEntity entity : data) {
							GeoPoint point = new GeoPoint(entity.getLatitude(),
									entity.getmLongitude());

							for (ItemVisualization visualization : visualizations) {
								OverlayItem overlayItem = new OverlayItem(
										visualization.getTitle(entity),
										visualization.getDescription(entity),
										point);
								overlayerItems.add(overlayItem);
							}
						}
						itemizedOverlay.setOverlayItems(overlayerItems);
					}
					// interpolation received, now this object represents the
					// current interpolation
					currentUpdate = UpdateHolder.this;
					mapView.postInvalidate();
				}

			}
		};

		public UpdateHolder(MercatorRect bounds, MapView mapView) {
			this.bounds = bounds;
			this.mapView = mapView;
		}

		public void cancel() {
			if (requestHolder != null) {
				requestHolder.cancel();
			}
			canceled = true;
		}

		@Override
		public void run() {
			if (!canceled) {
				if (bounds.zoom >= Settings.MIN_ZOOM_MAPINTERPOLATION) {
					// Just runs if zoom is in range
					synchronized (interpolationLock) {
						requestHolder = dataSource.getDataCache()
								.getDataByBBox(bounds, callback, false);
					}
				} else {
					infoHandler.setStatus(R.string.not_zoomed_in, 5000, this);
				}
			}

		}
	}

	protected UpdateHolder currentUpdate;
	protected UpdateHolder nextUpdate;

	protected Object interpolationLock = new Object();

	protected ItemizedDataOverlay<OverlayItem> itemizedOverlay;

	protected int cacheWidth;
	protected int cacheHeight;
	protected InfoView infoHandler;
	protected Paint paint;
	// protected Drawable itemizedDrawable;

	private MapView mapView;
	private Drawable drawable;
	private Bitmap interpolationBmp;

	private final int INTERPOLATION = 0;
	private final int POI = 1;
	private DataSourceHolder dataSource;

	public MapOverlayHandler2(final MapView mapView, DataSourceHolder dataSource) {
		this.dataSource = dataSource;
		paint = new Paint();
		paint.setAlpha(200);

		this.mapView = mapView;

		itemizedOverlay = new ItemizedDataOverlay<OverlayItem>(
				new ArrayList<OverlayItem>(), mapView.getResources()
						.getDrawable(R.drawable.mapmarker),
				new ItemizedDataOverlay.OnItemGestureListener<OverlayItem>() {

					@Override
					public boolean onItemSingleTapUp(final int index,
							final OverlayItem item) {
						// Toast.makeText( mapView.getContext(),
						// "Description: \n" +
						// "value: " + item.mTitle + "\n" + "" +
						// item.mDescription, Toast.LENGTH_LONG).show();
						itemizedOverlay.setBubbleItem(mapView, item, index);
						return true;
					}

					@Override
					public boolean onItemLongPress(final int index,
							final OverlayItem item) {
						// Toast.makeText( mapView.getContext(),
						// "Overlay Titled: " +
						// item.mTitle + " Long pressed" + "\n" +
						// "Description: " + item.mDescription
						// ,Toast.LENGTH_LONG).show();
						itemizedOverlay.setBubbleItem(mapView, item, index);
						return false;
					}

				}, mapView.getResourceProxy());
		this.mapView.getOverlays().add(this.itemizedOverlay);
	}

	// public MapOverlayHandler2(final MapView mapView, int cacheWidth,
	// int cacheHeight, Drawable drawable) {
	// this.cacheHeight = cacheHeight;
	// this.cacheWidth = cacheWidth;
	// this.itemizedDrawable = drawable;
	// this.mapView = mapView;
	// paint = new Paint();
	// paint.setAlpha(200);
	//
	// interpolationOverlay = new InterpolationOverlay(mapView.getContext(),
	// cacheWidth, cacheHeight);
	// this.mapView.getOverlays().add(this.interpolationOverlay);
	//
	// itemizedOverlay = new ItemizedDataOverlay<OverlayItem>(
	// new ArrayList<OverlayItem>(), mapView.getResources()
	// .getDrawable(R.drawable.mapmarker),
	// new ItemizedDataOverlay.OnItemGestureListener<OverlayItem>() {
	//
	// @Override
	// public boolean onItemSingleTapUp(final int index,
	// final OverlayItem item) {
	// Toast.makeText(
	// mapView.getContext(),
	// "Overlay Titled: " + item.mTitle
	// + " Single Tapped" + "\n"
	// + "Description: " + item.mDescription,
	// Toast.LENGTH_LONG).show();
	// return true;
	// }
	//
	// @Override
	// public boolean onItemLongPress(final int index,
	// final OverlayItem item) {
	// Toast.makeText(
	// mapView.getContext(),
	// "Overlay Titled: " + item.mTitle
	// + " Long pressed" + "\n"
	// + "Description: " + item.mDescription,
	// Toast.LENGTH_LONG).show();
	// return false;
	// }
	//
	// }, mapView.getResourceProxy());
	// this.mapView.getOverlays().add(this.itemizedOverlay);
	// }

	public void onTouchEvent(android.view.MotionEvent e, MapView mapView) {
		if (e.getAction() == MotionEvent.ACTION_UP
				|| e.getAction() == MotionEvent.ACTION_CANCEL) {
			updateOverlay(mapView, false);
		}
	};

	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
	}

	public void abort() {
		if (currentUpdate != null) {
			currentUpdate.cancel();
		}
	}

	public void setDrawable(Drawable drawable) {
		this.drawable = drawable;
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
		byte zoom = (byte) (mapView.getZoomLevel() - 1);

		zoom = (byte) Math.min(zoom, Settings.MAX_ZOOM_MAPINTERPOLATION);
		Projection proj = mapView.getProjection();

		GeoPoint gPoint = (GeoPoint) proj.fromPixels(0, 0);

		int x = (int) MercatorProj.transformLonToPixelX(
				gPoint.getLongitudeE6() / 1E6f, zoom);
		int y = (int) MercatorProj.transformLatToPixelY(
				gPoint.getLatitudeE6() / 1E6f, zoom);

		synchronized (interpolationLock) {
			MercatorRect newBounds = new MercatorRect(x, y, x + cacheWidth, y
					+ cacheHeight, zoom);

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
				}
				nextUpdate = new UpdateHolder(newBounds, mapView);
				mapView.postDelayed(nextUpdate, Math.max(0, updateDelay));
				Log.i("GeoAR", "Overlay Update in " + updateDelay / 1000 + " s");
			}
		}
	}
}
