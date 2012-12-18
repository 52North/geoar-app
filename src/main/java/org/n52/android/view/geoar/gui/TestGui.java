package org.n52.android.view.geoar.gui;

import org.n52.android.tracking.location.LocationHandler;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.location.Location;
import android.util.AttributeSet;
import android.view.View;

public class TestGui extends View {
	
	private static final float SIZE = 40;
	private final Location lastLocation = LocationHandler.getLastKnownLocation();
	
	private final int width;
	private final int height;
	
	private Paint paint;
	private float rangeOfView;

	public TestGui(Context context) {
		super(context);
		this.width = getWidth();
		this.height = getHeight();
		init();
	}

	public TestGui(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.width = getWidth();
		this.height = getHeight();
		init();
	}

	private void init(){
		
		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setTextSize(25);
		paint.setAntiAlias(true);
	}


	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.argb(100, 0, 0, 200));
		canvas.drawCircle(40, 40, 40, paint);
		
		drawCoordinates(canvas);
		invalidate();
	}
	
	private void drawCoordinates(Canvas canvas){
		final int rectLeft = getWidth() / 2;
		final int rectTop = 5;
		final int rectRight = getWidth() - 5;
		final int rectBottom = getHeight() / 10;
		
		// draw box
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.argb(100, 0, 0, 200));
		canvas.drawRect(new Rect(rectLeft, rectTop, rectRight, rectBottom), paint);
		
		// draw text
		paint.setColor(Color.GREEN);
		paint.setTextSize(25);
		paint.setAntiAlias(true);	
		int textLocationY = 25;
		int textLocationX = rectLeft + 5;
		canvas.drawText("Coordinates ", textLocationX, textLocationY, paint);
		paint.setTextSize(20);
		textLocationY += 20;
		canvas.drawText("Latitude  " + lastLocation.getLatitude(), textLocationX, textLocationY, paint);
		textLocationY += 20;
		canvas.drawText("Longitude " + lastLocation.getLongitude(), textLocationX, textLocationY, paint);
	}
	
	
	private void drawLocationDetails(Canvas canvas){
		Location loc = LocationHandler.getLastKnownLocation();
		canvas.drawText("Hello World" + LocationHandler.getLastKnownLocation().getAccuracy(), 5, 30, paint);
	}
	
}
