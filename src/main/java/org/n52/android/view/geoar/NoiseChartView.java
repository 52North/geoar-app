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
package org.n52.android.view.geoar;

import java.text.DecimalFormat;

import org.n52.android.NoiseARView;
import org.n52.android.alg.NoiseView;
import org.n52.android.alg.NoiseView.NoiseGridValueProvider;
import org.n52.android.alg.NoiseView.NoiseViewChangedListener;
import org.n52.android.geoar.R;
import org.n52.android.view.InfoView;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

/**
 * View to overlay a bar chart reflecting the currently "visible" noise. Needs a
 * {@link NoiseGridValueProvider} which notifies this view of currently visible
 * bounds. It is in return queried for the relevant noise values via the
 * {@link NoiseView} algorithm.
 * 
 * @author Holger Hopmann
 */
public class NoiseChartView extends View implements NoiseViewChangedListener,
		NoiseARView {

	/**
	 * Dialog to allow users to change chart settings. Nested class makes
	 * modification of renderer easier and it is only used here.
	 * 
	 * @author Holger Hopmann
	 */
	private class ChartDialog extends AlertDialog implements
			OnCheckedChangeListener {

		private ToggleButton buttonChart;
		private ToggleButton buttonChartLabels;

		public ChartDialog(Context context) {
			super(context);
			// Inflate Layout
			View layout = LayoutInflater.from(context).inflate(
					R.layout.chart_dialog, null);

			// Find Button Views
			buttonChart = (ToggleButton) layout
					.findViewById(R.id.toggleButtonChart);
			buttonChart.setChecked(showBars);
			buttonChartLabels = (ToggleButton) layout
					.findViewById(R.id.toggleButtonChartLabels);
			buttonChartLabels.setChecked(showLabels);

			// Bind Check Listeners
			buttonChart.setOnCheckedChangeListener(this);
			buttonChartLabels.setOnCheckedChangeListener(this);

			// Set Dialog Options
			setView(layout);
			setCancelable(true);
			setTitle(R.string.chart_settings);
			setButton(BUTTON_NEUTRAL, context.getString(R.string.ok),
					(Message) null);
		}

		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			// Change state based on user check input
			switch (buttonView.getId()) {
			case R.id.toggleButtonChart:
				showBars = isChecked;
				break;
			case R.id.toggleButtonChartLabels:
				showLabels = isChecked;
				break;
			}
			if (!showBars && !showLabels) {
				setVisibility(GONE);
			} else {
				setVisibility(VISIBLE);
			}
		}
	}

	private boolean activated;
	private Paint columnPaint;
	private boolean hasValidValues = false;
	private Paint labelPaint;
	private Paint linePaint;
	private int lineY40dB;
	private int lineY80dB;

	private float[] noiseValues;
	private NoiseView noiseView;
	private NoiseGridValueProvider provider;
	private boolean showBars = true;
	private boolean showLabels = true;
	private DecimalFormat labelFormat;

	public NoiseChartView(Context context) {
		super(context);
		init();
	}

	public NoiseChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public void clearNoiseGridValueProvider() {
		setActivated(false);
		provider = null;
	}

	private void init() {
		noiseValues = new float[Settings.NUM_SLICES_DIAGRAM];

		columnPaint = new Paint();
		columnPaint.setColor(Color.argb(200, 130, 130, 130));
		columnPaint.setStrokeWidth(1);
		columnPaint.setStyle(Style.FILL_AND_STROKE);

		linePaint = new Paint();
		linePaint.setColor(Color.LTGRAY);
		linePaint.setStrokeWidth(2);
		linePaint.setAntiAlias(true);

		labelPaint = new Paint();
		labelPaint.setColor(Color.LTGRAY);
		labelPaint.setTextSize(12);
		labelPaint.setAntiAlias(true);
		labelPaint.setTextAlign(Align.CENTER);

		labelFormat = new DecimalFormat("0.00");

		setBackgroundColor(Color.argb(100, 0, 0, 0));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (showBars || showLabels) {
			super.onDraw(canvas);
			int width = getWidth();
			int height = getHeight();

			// Draw reference lines
			canvas.drawLine(0, lineY40dB, width, lineY40dB, linePaint);
			canvas.drawText("40 dB", 10, lineY40dB, linePaint);
			canvas.drawLine(0, lineY80dB, width, lineY80dB, linePaint);
			canvas.drawText("80 dB", 10, lineY80dB, linePaint);

			if (hasValidValues && activated) {
				synchronized (noiseValues) {

					float step = width / (float) noiseValues.length;
					float columnWidth = step * 0.8f;
					float heightPerNoise = height
							/ (Settings.MAX_NOISE_DIAGRAM - Settings.MIN_NOISE_DIAGRAM);
					for (int i = 0; i < noiseValues.length; i++) {
						float left = step * i;
						if (showBars) {
							float colHeight = ((noiseValues[i] - Settings.MIN_NOISE_DIAGRAM) * heightPerNoise);
							canvas.drawRect(left, height - colHeight, left
									+ columnWidth, height, columnPaint);
						}
						if (showLabels) {
							canvas.save();
							canvas.rotate(-90, left + columnWidth, height / 2);
							canvas.drawText(labelFormat.format(noiseValues[i]),
									left + columnWidth + 2, height / 2,
									labelPaint);
							canvas.restore();
						}
					}
				}
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		this.setMeasuredDimension(width, height);
	}

	public void onNoiseViewChanged(PointF bottomLeft, PointF bottomRight,
			PointF topLeft, PointF topRight, PointF viewerPos) {

		if (bottomLeft != null && bottomRight != null && topLeft != null
				&& topRight != null) {
			synchronized (noiseValues) {
				System.arraycopy(noiseView.getVisibleNoise(bottomLeft,
						bottomRight, topLeft, topRight), 0, noiseValues, 0,
						noiseValues.length);
				hasValidValues = true;
				postInvalidate();
			}
		} else {
			if (hasValidValues) {
				hasValidValues = false;
				postInvalidate();
			} else {
				hasValidValues = false;
			}
		}

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);

		float noiseRange = Settings.MAX_NOISE_DIAGRAM
				- Settings.MIN_NOISE_DIAGRAM;
		lineY40dB = (int) (h - (40 - Settings.MIN_NOISE_DIAGRAM)
				* (h / noiseRange));
		lineY80dB = (int) (h - (80 - Settings.MIN_NOISE_DIAGRAM)
				* (h / noiseRange));
	}

	@Override
	protected void onVisibilityChanged(View changedView, int visibility) {
		setActivated(isShown());
		super.onVisibilityChanged(changedView, visibility);
	}

	public void setActivated(boolean active) {
		if (provider != null) {
			if (active) {
				provider.addOnNoiseViewChangedListener(this);
			} else {
				provider.removeOnNoiseViewChangedListener(this);
				hasValidValues = false;
			}
		}
		this.activated = active;
	}

	public void setNoiseGridValueProvider(NoiseGridValueProvider provider) {
		setActivated(false);
		this.provider = provider;
		noiseView = new NoiseView(provider, noiseValues.length);
		setActivated(true);
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_select_chart:
			new ChartDialog(getContext()).show();
			setVisibility(VISIBLE);
			return true;
		case R.id.item_calibrate:
			setVisibility(GONE);
			// Allow other views to receive this event too
			break;
		}
		return false;
	}

	public Integer getMenuGroupId() {
		return null;
	}

	public boolean isVisible() {
		return isShown();
	}

	public void setInfoHandler(InfoView infoHandler) {

	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("ChartViewBars", showBars);
		outState.putBoolean("ChartViewLabels", showLabels);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		showBars = savedInstanceState.getBoolean("ChartViewBars", true);
		showLabels = savedInstanceState.getBoolean("ChartViewLabels", true);
	}

}
