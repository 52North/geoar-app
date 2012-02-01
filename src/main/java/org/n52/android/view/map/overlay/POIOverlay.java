package org.n52.android.view.map.overlay;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.Measurement;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.MeasurementManager.GetMeasurementBoundsCallback;
import org.n52.android.geoar.R;
import org.n52.android.view.geoar.Settings;
import org.n52.android.view.map.MapOverlay;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;
import org.osmdroid.views.overlay.OverlayItem;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

public class POIOverlay extends MapOverlay {

	public POIOverlay(Context context, MeasurementManager measureManager,
			int cacheWidth, int cacheHeight) {
		super(context, measureManager, cacheWidth, cacheHeight);
		// TODO Auto-generated constructor stub
	}
	
//	@SuppressWarnings("unused")
//	private class UpdatePOIHolder extends UpdateHolder{
//		
//		public UpdatePOIHolder(MercatorRect bounds, final MapView mapView) {
//			super(bounds, mapView);
//			
//			callback = new GetMeasurementBoundsCallback(){
//				
//				@SuppressWarnings("unchecked")
//				public void onReceiveInterpolation(MercatorRect bounds,
//						Object dataStream) {
//					
//					if (!canceled) {
//						synchronized (interpolationLock){
//							measurementsListBuffer = (List<Measurement>) dataStream;
//							overlayBmp = null; // TODO to bitmap geschichte
//						}
//						// Measurements received, now this object represents the current
//						// measurements
//						currentUpdate = UpdatePOIHolder.this;
//						
//						mapView.postInvalidate();
//					}
//				}
//
//				@Override
//				public void onProgressUpdate(int progress, int maxProgress, int step) {
//					if(infoHandler != null){
//						String stepTitle = "step";
//						// TODO new steps to show
////						switch (step) {
////						case Interpolation.STEP_CLUSTERING:
////							stepTitle = "Clustering";
////							break;
////						case Interpolation.STEP_INTERPOLATION:
////							stepTitle = "Interpolation";
////							break;
////						case MeasurementManager.STEP_REQUEST:
////							stepTitle = "Messungsabfrage";
////							break;
////						}
//
//						infoHandler.setProgressTitle(stepTitle, UpdatePOIHolder.this);
//						infoHandler.setProgress(progress, maxProgress, UpdatePOIHolder.this);
//					}
//				}
//
//				@Override
//				public void onAbort(MercatorRect bounds, int reason) {
//					// TODO Auto-generated method stub
//					
//				}
//			};
//					
//		}
//		
//
//		@Override
//		public void run() {
//			if(!canceled){
//				if(bounds.zoom >= Settings.MIN_ZOOM_MAPINTERPOLATION){
//					// just runs if zoom is in range
//					synchronized(interpolationLock){
//						requestHolder = measureManager.getMeasurementsAsPOIs(bounds, 
//								callback, false, measurementsListBuffer);
//					}
//				} else {
//					infoHandler.setStatus(R.string.not_zoomed_in, 5000, this);
//				}
//			}	
//		}
//		
//		public void cancel(){
//			if(requestHolder != null){
//				requestHolder.cancel();
//			}
//			canceled = true;
//		}
//	}
//	
//	public class POIOverlayItem extends OverlayItem{
//
//		public POIOverlayItem(GeoPoint point, String title, String snippet) {
//			super(point, title, snippet);
//			
//			ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
//			drawable.getPaint().setColor(100);
//			drawable.setBounds(-5, -5, 10, 10);
//			
//			setMarker(drawable);
//		}
//		
//	
//
//	}
//
//	private List<Measurement> measurementsListBuffer = new ArrayList<Measurement>();
//	
//	/**
//	 * Constructor. Needs {@link MeasurementManager} from which to receive data,
//	 * and bounds which specify the inernal buffers size in pixel
//	 * 
//	 * @param measureManager
//	 * @param cacheWidth
//	 * @param cacheHeight
//	 */
//	public POIOverlay(MeasurementManager measureManager, int cacheWidth,
//			int cacheHeight) {
//		super(measureManager, cacheWidth, cacheHeight);
//		this.measureManager = measureManager;
//		this.cacheWidth = cacheWidth;
//		this.cacheHeight = cacheHeight;
//		paint = new Paint();
//		paint.setAlpha(200);
//	}
//
//	/**
//	 * changes the interpolation buffer size after construction
//	 * 
//	 * @param width
//	 * @param height
//	 */
//	@Override
//	public void setInterpolationPixelSize(int width, int height) {
//		synchronized (interpolationLock){
//			if(nextUpdate != null){
//				nextUpdate.cancel();
//			}
//			
//			this.cacheHeight = height;
//			this.cacheWidth = width;
//			measurementsListBuffer = null;
//			if(overlayBmp != null){
//				overlayBmp.recycle();
//				overlayBmp = null;
//			}
//		}
//		
//	}
//	
//	@Override 
//	public void draw(Canvas canvas, MapView mapView, boolean shadow){
//		super.draw(canvas, mapView, shadow);
//		if(shadow || currentUpdate == null)
//			return;
//		
//		// only draw if not shadow processing and if interpolation really available
//		Projection proj = mapView.getProjection();
//		
////		Point tileTopLeft = proj
////				.toPixels(
////						new GeoPoint(
////								(int) (MercatorProj.transformPixelYToLat(
////										currentUpdate.bounds.top,
////										currentUpdate.bounds.zoom) * 1E6),
////								(int) (MercatorProj.transformPixelXToLon(
////										currentUpdate.bounds.left,
////										currentUpdate.bounds.zoom) * 1E6)),
////												null);
////		
////		Point tileBottomRight = proj
////				.toPixels(
////						new GeoPoint(
////								(int) (MercatorProj
////										.transformPixelYToLat(
////												currentUpdate.bounds.bottom,
////												currentUpdate.bounds.zoom) * 1E6),
////								(int) (MercatorProj
////										.transformPixelXToLon(
////												currentUpdate.bounds.right,
////												currentUpdate.bounds.zoom) * 1E6)),
////												null);
////		
////		Rect dstRect = new Rect(tileTopLeft.x, tileTopLeft.y, tileBottomRight.x, tileBottomRight.y);
////		// draw interpolation Bitmap
////		canvas.drawBitmap(overlayBmp, null, dstRect, paint);
//		
//		
////        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
//        
//
//	}
//
//	@Override
//	public void updateStatus(MapView mapView, boolean force) {
//		// TODO Auto-generated method stub
//		
//	}
//	
	
}
