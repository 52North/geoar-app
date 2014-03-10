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
package org.n52.geoar.ar.view.overlay;

import java.util.ArrayList;
import java.util.List;

import org.n52.geoar.R;
import org.n52.geoar.ar.view.ARObject;
import org.n52.geoar.ar.view.ARView;
import org.n52.geoar.newdata.PluginActivityContext;
import org.n52.geoar.tracking.camera.RealityCamera.CameraUpdateListener;
import org.n52.geoar.tracking.location.LocationHandler;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

// TODO XXX
public class ARCanvasSurfaceView extends View implements CameraUpdateListener {

	private static final float SIZE = 40;
	private final Location lastLocation = LocationHandler
			.getLastKnownLocation();

	private Paint paint;
	private float rangeOfView;

	private RectF radarRect;
	private Paint radarCirclePaint;
	private Paint radarOvalPaint;

	private Paint poiRenderer;

	private boolean init;

	GUIDrawable drawable;

	private List<ARObject> mARObjects = new ArrayList<ARObject>(0);
	private boolean mARObjectsChanged;
	private ARView mARView;

	public ARCanvasSurfaceView(ARView arView) {
		super(arView.getContext());
		this.mARView = arView;
		init();
	}

	private void init() {
		paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setTextSize(25);
		paint.setAntiAlias(true);

		this.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				if (MotionEvent.ACTION_DOWN == motionEvent.getAction()) {
					synchronized (mARObjects) {
						for (ARObject object : mARObjects) {
							if (object.thisObjectHitted(motionEvent.getX(),
									motionEvent.getY())) {
								onItemClicked(object);
							}
						}
					}
				}
				return false;
			}
		});

		initDrawingTools();
	}

	private void onItemClicked(ARObject item) {
		PluginActivityContext pluginActivityContext = new PluginActivityContext(
				item.getDataSourceInstance().getParent().getPluginHolder()
						.getPluginContext(), getContext());
		View featureView = item.getVisualization().getFeatureView(
				item.getEntity(), null, null, pluginActivityContext);
		if (featureView != null) {
			String title = item.getVisualization().getTitle(item.getEntity());
			if (title == null || title.isEmpty()) {
				title = "";
			}
			String message = item.getVisualization().getDescription(
					item.getEntity());
			if (message == null || message.isEmpty()) {
				message = "";
			}
			Builder builder = new AlertDialog.Builder(getContext());
			builder.setTitle(title).setMessage(message)
					.setNeutralButton(R.string.cancel, null)
					.setView(featureView);
			builder.create().show();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mARObjectsChanged) {
			// XXX will be the same instance anyway...
			mARObjects = mARView.getARObjects();
			mARObjectsChanged = false;
		}

		if (!init) {
			// TODO move initialization to the correct event handlers
			float scale = (float) getWidth() / 4;
			drawable = new Radar(new RectF(0, 0, scale, scale));
			drawable.initDrawingTools();
			init = true;
		}

		synchronized (mARObjects) {
			for (ARObject arObject : mARObjects) {
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

		if (lastLocation != null) {
			textLocationY += 20;
			canvas.drawText("Latitude  " + lastLocation.getLatitude(),
					textLocationX, textLocationY, paint);
			textLocationY += 20;
			canvas.drawText("Longitude " + lastLocation.getLongitude(),
					textLocationX, textLocationY, paint);
		}
	}

	private void drawLocationDetails(Canvas canvas) {
		Location loc = LocationHandler.getLastKnownLocation();
		if (loc != null) {
			canvas.drawText("Hello World"
					+ LocationHandler.getLastKnownLocation().getAccuracy(), 5,
					30, paint);
		}
	}

	@Override
	public void onCameraUpdate() {
		// TODO Auto-generated method stub

	}

	public void notifyARObjectsChanged() {
		mARObjectsChanged = true;
	}

}
