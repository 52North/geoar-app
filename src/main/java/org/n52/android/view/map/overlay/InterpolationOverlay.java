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
import org.n52.android.view.InfoView;
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
import android.util.Log;
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



	private Object interpolationLock = new Object();
	private byte[] interpolationBuffer;
	private MercatorRect bounds;
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

		this.cacheHeight = height;
		this.cacheWidth = width;

//		if (interpolationBmp != null) {
//			interpolationBmp.recycle();
//			interpolationBmp = null;
//		}
	}

	@Override
	public boolean onTouchEvent(android.view.MotionEvent e, MapView mapView) {
		if (e.getAction() == MotionEvent.ACTION_UP
				|| e.getAction() == MotionEvent.ACTION_CANCEL) {
		}
		return super.onTouchEvent(e, mapView);
	};

	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
		
		if (shadow || interpolationBmp == null)
			return;
		// only draw if not shadow processing and if interpolation really
		// available
		Projection proj = mapView.getProjection();

		Point tileTopLeft = proj
				.toPixels(
						new GeoPoint(
								(int) (MercatorProj.transformPixelYToLat(
										bounds.top,	bounds.zoom) * 1E6),
								(int) (MercatorProj.transformPixelXToLon(
										bounds.left, bounds.zoom) * 1E6)),
						null);
		Point tileBottomRight = proj
				.toPixels(
						new GeoPoint(
								(int) (MercatorProj
										.transformPixelYToLat(
												bounds.bottom, bounds.zoom) * 1E6),
								(int) (MercatorProj
										.transformPixelXToLon(
												bounds.right, bounds.zoom) * 1E6)),
						null);

		Rect dstRect = new Rect(tileTopLeft.x, tileTopLeft.y,
				tileBottomRight.x, tileBottomRight.y);
		// draw interpolation Bitmap
		canvas.drawBitmap(interpolationBmp, null, dstRect, paint);
	}




	public void setInfoHandler(InfoView infoHandler) {
		this.infoHandler = infoHandler;
	}

	public void setMeasureManager(MeasurementManager measureManager) {
		this.measureManager = measureManager;
	}
	
	public void setOverlayBitmap(MercatorRect bounds, Bitmap overlay){
		this.interpolationBmp = overlay;
		this.bounds = bounds;
	}
	
	public void setOverlayData(MercatorRect bounds, byte[] interpolationOverlay){
		this.bounds = bounds;
		this.interpolationBuffer = interpolationOverlay;
		Log.d("yea", "yeaaeae");
		this.interpolationBmp = Interpolation.interpolationToBitmap(bounds, interpolationBuffer, interpolationBmp);
	}


}