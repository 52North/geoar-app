package org.n52.android.view.map;

import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.MeasurementManager.GetMeasurementBoundsCallback;
import org.n52.android.data.MeasurementManager.RequestHolder;
import org.n52.android.view.InfoView;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;



public class MapOverlay extends Overlay {
	
	public interface OnProgressUpdateListener extends org.n52.android.alg.OnProgressUpdateListener {
		void onAbort(int reason);
	}
	
	protected abstract class UpdateHolder implements Runnable{
		protected boolean canceled;
		protected MercatorRect bounds; 
		protected MapView mapView;
		protected RequestHolder requestHolder;
		
		protected GetMeasurementBoundsCallback callback;
		
		public UpdateHolder(MercatorRect bounds, MapView mapView){
			this.bounds = bounds;
			this.mapView = mapView;
		}
		
		public void cancel(){
			if(requestHolder != null){
				requestHolder.cancel();
			}
			canceled = true;
		}
	}
	
	protected UpdateHolder currentUpdate;
	protected UpdateHolder nextUpdate;
	
	private final int INTERPOLATION = 0;
	private final int POI = 1;
	
	protected Object interpolationLock = new Object();
	
	protected MeasurementManager measureManager;
	
	protected int cacheWidth;
	protected int cacheHeight;
	protected InfoView infoHandler;
	protected Paint paint;
	
	protected Bitmap overlayBmp;
	protected GetMeasurementBoundsCallback callback;
	
	
	public MapOverlay(Context context, MeasurementManager measureManager, int cacheWidth, int cacheHeight){
		super(context);
		this.measureManager = measureManager;
		this.cacheHeight = cacheHeight;
		this.cacheWidth = cacheWidth;
		paint = new Paint();
		paint.setAlpha(200);
	}
	
//	@Override
//	public boolean onTouchEvent(android.view.MotionEvent e, MapView mapView){
//		if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL){
//			updateStatus(mapView, false);
//		}
//		return super.onTouchEvent(e, mapView);
//	}
	
//	public abstract void setInterpolationPixelSize(int width, int height);
//	public abstract void updateStatus(MapView mapView, boolean force);
	
	public void setInfoHandler(InfoView infoHandler){
		this.infoHandler = infoHandler;
	}
	
	public void setMeasureManager(MeasurementManager measureManager){
		this.measureManager = measureManager;
	}
		
	public void abort(){
		if(currentUpdate != null){
			currentUpdate.cancel();
		}
	}

	@Override
	protected void draw(Canvas arg0, MapView arg1, boolean arg2) {
		// TODO Auto-generated method stub
		
	}
}
