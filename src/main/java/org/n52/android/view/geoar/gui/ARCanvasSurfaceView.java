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
package org.n52.android.view.geoar.gui;

import java.util.List;

import org.n52.android.GeoARApplication;
import org.n52.android.R;
import org.n52.android.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.android.tracking.location.LocationHandler;
import org.n52.android.view.geoar.ARFragment2;
import org.n52.android.view.geoar.ARFragment2.ARViewComponent;
import org.n52.android.view.geoar.gl.ARObject;
import org.n52.android.view.geoar.gl.ARSurfaceViewRenderer;
import org.n52.android.view.geoar.gl.DataSourceVisualizationHandler;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class ARCanvasSurfaceView extends View implements CameraUpdateListener,
		ARViewComponent {

	private static final float SIZE = 40;
	private final Location lastLocation = LocationHandler
			.getLastKnownLocation();

	private final int width;
	private final int height;

	private Paint paint;
	private float rangeOfView;

	private RectF radarRect;
	private Paint radarCirclePaint;
	private Paint radarOvalPaint;

	private Paint poiRenderer;

	private boolean init;

	GUIDrawable drawable;
	private List<DataSourceVisualizationHandler> visualizationHandler;

	public ARCanvasSurfaceView(Context context) {
		super(context);
		this.width = getWidth();
		this.height = getHeight();
		init();
	}

	public ARCanvasSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.width = getWidth();
		this.height = getHeight();
		init();
	}

	private void init() {
		ARFragment2.addARViewComponent(this);
		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setTextSize(25);
		paint.setAntiAlias(true);
		
		this.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				if(MotionEvent.ACTION_DOWN == motionEvent.getAction()){
					for(DataSourceVisualizationHandler visHandler : visualizationHandler){
						for(ARObject object : visHandler.getARObjects()){
							if(object.thisObjectHitted(motionEvent.getX(), motionEvent.getY())){
								Toast.makeText(GeoARApplication.applicationContext, "omg yea", Toast.LENGTH_SHORT).show();
//								object.onItemClicked();
							}
						}
					}
//					Toast.makeText(GeoARApplication.applicationContext, "omg yea", Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});

		initDrawingTools();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (!init) {
			float scale = (float) getWidth() / 4;
			drawable = new Radar(new RectF(0, 0, scale, scale));
			drawable.initDrawingTools();
			init = true;
		}

//		drawCoordinates(canvas);

		// drawable.onRenderCanvas(canvas);
		// now the outer rim circle
		// canvas.drawOval(rimRect, rimCirclePaint);

		canvas.drawCircle(100, 100, 1, poiRenderer);

		if (ARSurfaceViewRenderer.test != null) {
			float[] screenCoord = ARSurfaceViewRenderer.test
					.onScreenCoordsUpdate();
			if (screenCoord != null)
				canvas.drawCircle(screenCoord[0], 690 - screenCoord[1], 10f,
						poiRenderer);
		}
		for (DataSourceVisualizationHandler handler : visualizationHandler) {
			for (ARObject arObject : handler.getARObjects()) {
				arObject.renderCanvas(poiRenderer, canvas);
			}
		}

		invalidate();
	}

	private void initDrawingTools() {
		radarRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);
		radarCirclePaint = new Paint();
		radarCirclePaint.setAntiAlias(true);
		radarCirclePaint.setStyle(Paint.Style.STROKE);
		radarCirclePaint.setStrokeWidth(0.05f);
		radarCirclePaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f,
				Color.rgb(0xf0, 0xf5, 0xf0), Color.rgb(0x30, 0x31, 0x30),
				Shader.TileMode.CLAMP));

		radarOvalPaint = new Paint();
		radarOvalPaint.setAntiAlias(true);
		radarOvalPaint.setStyle(Paint.Style.FILL);
		radarOvalPaint.setColor(Color.argb(100, 0, 0, 200));

		poiRenderer = new Paint();
		poiRenderer.setAntiAlias(true);
		poiRenderer.setStyle(Paint.Style.FILL);
		poiRenderer.setColor(Color.GREEN);
	}

	private void drawRadar(Canvas canvas) {
		/** push camera matrix */
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		/** set scale */
		float scale = (float) getWidth() / 4;
		canvas.scale(scale, scale);

		/** fills the circle */
		canvas.drawOval(radarRect, radarOvalPaint);
		/** draw boarder circle */
		canvas.drawOval(radarRect, radarCirclePaint);

		/** restore camera matrix to default */
		canvas.restore();
	}

	private void drawCoordinates(Canvas canvas) {
		final int rectLeft = getWidth() / 2;
		final int rectTop = 5;
		final int rectRight = getWidth() - 5;
		final int rectBottom = getHeight() / 10;

		// draw box
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(Color.argb(100, 0, 0, 200));
		canvas.drawRect(new Rect(rectLeft, rectTop, rectRight, rectBottom),
				paint);

		// draw text
		paint.setColor(Color.GREEN);
		paint.setTextSize(25);
		paint.setAntiAlias(true);
		int textLocationY = 25;
		int textLocationX = rectLeft + 5;
		canvas.drawText("Coordinates ", textLocationX, textLocationY, paint);
		paint.setTextSize(20);
		textLocationY += 20;
		canvas.drawText("Latitude  " + lastLocation.getLatitude(),
				textLocationX, textLocationY, paint);
		textLocationY += 20;
		canvas.drawText("Longitude " + lastLocation.getLongitude(),
				textLocationX, textLocationY, paint);
	}

	private void drawLocationDetails(Canvas canvas) {
		Location loc = LocationHandler.getLastKnownLocation();
		canvas.drawText("Hello World"
				+ LocationHandler.getLastKnownLocation().getAccuracy(), 5, 30,
				paint);
	}

	@Override
	public void onVisualizationHandlerAdded(
			DataSourceVisualizationHandler handler) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setVisualizationHandlerRef(
			List<DataSourceVisualizationHandler> handlers) {
		this.visualizationHandler = handlers;
	}

	@Override
	public void onCameraUpdate() {
		// TODO Auto-generated method stub

	}

}
