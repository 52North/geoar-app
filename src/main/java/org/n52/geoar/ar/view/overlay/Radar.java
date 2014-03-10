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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;

public class Radar extends GUIDrawable {

	private Paint radarCirclePaint;
	private Paint radarOvalPaint;

	public Radar(RectF area) {
		super(area);
	}

	@Override
	protected void initDrawingTools() {
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
	}

	@Override
	protected void onRender(Canvas canvas) {
		/** push camera matrix */
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		setScaleAndFocusToDrawingArea(canvas);
		/** fills the circle */
		canvas.drawOval(drawableArea, radarOvalPaint);
		/** draw boarder circle */
		canvas.drawOval(drawableArea, radarCirclePaint);

		/** restore camera matrix to default */
		canvas.restore();

	}

}
