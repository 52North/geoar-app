/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */
package org.n52.android.view.map.overlay;

import org.n52.android.alg.Interpolation;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.MeasurementManager.GetMeasurementBoundsCallback;
import org.n52.android.data.MeasurementManager.RequestHolder;
import org.n52.android.geoar.R;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;


/**
 * Map {@link Overlay} to show interpolation data in a {@link MapView}
 * 
 * @author Holger Hopmann
 * @author Arne de Wall
 * 
 */
public class InterpolationOverlay extends Overlay {

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
	private class UpdateInterpolationHolder implements Runnable {
		private boolean canceled;
		private MercatorRect bounds;
		private MapView mapView;
		private RequestHolder requestHolder;

		// Callback for finished interpolation
		private GetMeasurementBoundsCallback callback = new GetMeasurementBoundsCallback() {

			public void onReceiveInterpolation(MercatorRect bounds,
					Object dataStream) {

				if (!canceled) {
					synchronized (interpolationLock) {
						interpolationBuffer = (byte[]) dataStream;
						interpolationBmp = Interpolation.interpolationToBitmap(
								bounds, interpolationBuffer, interpolationBmp);
					}
					// interpolation received, now this object represents the
					// current interpolation
					currentInterpolationUpdate = UpdateInterpolationHolder.this;

					mapView.postInvalidate();
				}
			}
			

			public void onAbort(MercatorRect bounds, final int reason) {
				canceled = true;

				if (infoHandler != null) {
					// inform user of aborting reason
					infoHandler.clearProgress(UpdateInterpolationHolder.this);
					if (reason == MeasurementManager.ABORT_NO_CONNECTION) {
						infoHandler.setStatus(R.string.connection_error, 5000,
								UpdateInterpolationHolder.this);
					} else if (reason == MeasurementManager.ABORT_UNKOWN) {
						infoHandler.setStatus(R.string.unkown_error, 5000,
								UpdateInterpolationHolder.this);
					}
				}
			}

			public void onProgressUpdate(final int progress,
					final int maxProgress, final int step) {
				if (infoHandler != null) {

					String stepTitle = "";
					switch (step) {
					case Interpolation.STEP_CLUSTERING:
						stepTitle = "Clustering";
						break;
					case Interpolation.STEP_INTERPOLATION:
						stepTitle = "Interpolation";
						break;
					case MeasurementManager.STEP_REQUEST:
						stepTitle = "Messungsabfrage";
						break;
					}

					infoHandler.setProgressTitle(stepTitle,
							UpdateInterpolationHolder.this);
					infoHandler.setProgress(progress, maxProgress,
							UpdateInterpolationHolder.this);
				}
			}


		};

		public UpdateInterpolationHolder(MercatorRect bounds, MapView mapView) {
			this.bounds = bounds;
			this.mapView = mapView;
		}

		public void run() {
			if (!canceled) {
				if (bounds.zoom >= Settings.MIN_ZOOM_MAPINTERPOLATION) {
					// Just runs if zoom is in range
					synchronized (interpolationLock) {
						requestHolder = measureManager.getInterpolation(bounds,
								callback, false, interpolationBuffer);
					}
				} else {
					infoHandler.setStatus(R.string.not_zoomed_in, 5000, this);
				}
			}
		}

		public void cancel() {
			if (requestHolder != null) {
				requestHolder.cancel();
			}
			canceled = true;
		}

	}

	// stores current interpolation and the one which is performed at the moment
	// to make it possible to cancel it
	private UpdateInterpolationHolder currentInterpolationUpdate;
	private UpdateInterpolationHolder nextInterpolationUpdate;

	private Object interpolationLock = new Object();
	private byte[] interpolationBuffer;

	private Bitmap interpolationBmp;

	private MeasurementManager measureManager;

	private int cacheWidth;
	private int cacheHeight;
	private InfoView infoHandler;
	private Paint paint;

	/**
	 * Constructor. Needs {@link MeasurementManager} from which to receive data,
	 * and bounds which specify the inernal buffers size in pixel
	 * 
	 * @param measureManager
	 * @param cacheWidth
	 * @param cacheHeight
	 */
	public InterpolationOverlay(Context context, MeasurementManager measureManager,
			int cacheWidth, int cacheHeight) {
		super(context);
		this.measureManager = measureManager;
		this.cacheWidth = cacheWidth;
		this.cacheHeight = cacheHeight;
		paint = new Paint();
		paint.setAlpha(200);
	}

	/**
	 * changes the interpolation buffer size after construction
	 * 
	 * @param width
	 * @param height
	 */
	public void setInterpolationPixelSize(int width, int height) {
		synchronized (interpolationLock) {
			if (nextInterpolationUpdate != null) {
				nextInterpolationUpdate.cancel();
			}
			this.cacheHeight = height;
			this.cacheWidth = width;
			interpolationBuffer = null;
			if (interpolationBmp != null) {
				interpolationBmp.recycle();
				interpolationBmp = null;
			}
		}
	}

	@Override
	public boolean onTouchEvent(android.view.MotionEvent e, MapView mapView) {
		if (e.getAction() == MotionEvent.ACTION_UP
				|| e.getAction() == MotionEvent.ACTION_CANCEL) {
			updateInterpolation(mapView, false);
		}
		return super.onTouchEvent(e, mapView);
	};

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {

		if (shadow || currentInterpolationUpdate == null)
			return;
		// only draw if not shadow processing and if interpolation really
		// available
		Projection proj = mapView.getProjection();

		Point tileTopLeft = proj
				.toPixels(
						new GeoPoint(
								(int) (MercatorProj.transformPixelYToLat(
										currentInterpolationUpdate.bounds.top,
										currentInterpolationUpdate.bounds.zoom) * 1E6),
								(int) (MercatorProj.transformPixelXToLon(
										currentInterpolationUpdate.bounds.left,
										currentInterpolationUpdate.bounds.zoom) * 1E6)),
						null);
		Point tileBottomRight = proj
				.toPixels(
						new GeoPoint(
								(int) (MercatorProj
										.transformPixelYToLat(
												currentInterpolationUpdate.bounds.bottom,
												currentInterpolationUpdate.bounds.zoom) * 1E6),
								(int) (MercatorProj
										.transformPixelXToLon(
												currentInterpolationUpdate.bounds.right,
												currentInterpolationUpdate.bounds.zoom) * 1E6)),
						null);

		Rect dstRect = new Rect(tileTopLeft.x, tileTopLeft.y,
				tileBottomRight.x, tileBottomRight.y);
		// draw interpolation Bitmap
		canvas.drawBitmap(interpolationBmp, null, dstRect, paint);
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
	public void updateInterpolation(MapView mapView, boolean force) {
		byte zoom = (byte) (mapView.getZoomLevel() - 1);

		zoom = (byte) Math.min(zoom, Settings.MAX_ZOOM_MAPINTERPOLATION);
		Projection proj = mapView.getProjection();

		GeoPoint gPoint = (GeoPoint) proj.fromPixels(0, 0);

		int x = (int) MercatorProj.transformLonToPixelX(
				gPoint.getLongitudeE6() / 1E6f, zoom);
		int y = (int) MercatorProj.transformLatToPixelY(gPoint.getLatitudeE6() / 1E6f,
				zoom);

		synchronized (interpolationLock) {
			MercatorRect newBounds = new MercatorRect(x, y, x + cacheWidth, y
					+ cacheHeight, zoom);

			int updateDelay = -1;

			if (currentInterpolationUpdate == null
					|| !currentInterpolationUpdate.bounds.contains(newBounds)) {
				// no data or data outside viewport
				updateDelay = 2500;
			}
			if (currentInterpolationUpdate != null
					&& currentInterpolationUpdate.bounds.contains(newBounds)
					&& zoom != currentInterpolationUpdate.bounds.zoom) {
				// data inside viewport, but different zoom
				updateDelay = 5000;
			}

			if (updateDelay >= 0 || force) {
				newBounds.expand(Settings.BUFFER_MAPINTERPOLATION,
						Settings.BUFFER_MAPINTERPOLATION);
				if (nextInterpolationUpdate != null) {
					nextInterpolationUpdate.cancel();
				}
				nextInterpolationUpdate = new UpdateInterpolationHolder(
						newBounds, mapView);
				mapView.postDelayed(nextInterpolationUpdate,
						Math.max(0, updateDelay));
			}
		}
	}

	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
	}

	public void setMeasureManager(MeasurementManager measureManager) {
		this.measureManager = measureManager;
	}

	/**
	 * cancel current interpolation order
	 */
	public void abort() {
		if (nextInterpolationUpdate != null) {
			nextInterpolationUpdate.cancel();
		}
	}
}