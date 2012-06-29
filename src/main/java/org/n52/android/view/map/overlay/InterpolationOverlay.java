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

import org.n52.android.alg.InterpolationProvider;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.MeasurementManager;
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
 * @author Arne de Wall
 * 
 */
public class InterpolationOverlay extends Overlay {

	private byte[] interpolationBuffer;
	private MercatorRect bounds;
	private Bitmap interpolationBmp;

	private int cacheWidth;
	private int cacheHeight;

	private Paint paint;

	/**
	 * Constructor. Needs {@link MeasurementManager} from which to receive data,
	 * and bounds which specify the inernal buffers size in pixel
	 * 
	 * @param measureManager
	 * @param cacheWidth
	 * @param cacheHeight
	 */
	public InterpolationOverlay(Context context, int cacheWidth, int cacheHeight) {
		super(context);
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

		if (interpolationBmp != null) {
			interpolationBmp.recycle();
			interpolationBmp = null;
		}
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


	
	/**
	 * Updates the interpolation Overlay bitmap and the bounds
	 * @param bounds
	 * 				Bounds of the interpolation rectangle
	 * @param overlay
	 * 				Bitmap of interpolation results
	 */
	public void setOverlayBitmap(MercatorRect bounds, Bitmap overlay){
		this.interpolationBmp = overlay;
		this.bounds = bounds;
	}
	
	/**
	 * Updates the interpolation Overlay bitmap and the bounds
	 * 			not needed anymore i think
	 * @param bounds
	 * 				Bounds of the interpolation rectangle
	 * @param overlay
	 * 				byte array of interpolation results
	 */
	public void setOverlayData(MercatorRect bounds, byte[] interpolationOverlay){
		this.bounds = bounds;
		this.interpolationBuffer = interpolationOverlay;
		this.interpolationBmp = InterpolationProvider.interpolationToBitmap(
				bounds, interpolationBuffer, interpolationBmp);
	}


}