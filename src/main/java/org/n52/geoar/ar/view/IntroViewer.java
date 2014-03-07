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
package org.n52.geoar.ar.view;

import org.n52.geoar.R;
import org.n52.geoar.ar.view.IntroController.Task;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class IntroViewer extends RelativeLayout implements View.OnTouchListener {

	private Activity activity;
	private Task currentStep;

	private RelativeLayout descriptionView;
	private View skipButton;
	private View startButton;

	// painting stuff
	private final int backgroundColor = Color.argb(150, 15, 15, 60);
	private final Paint transparentPainter;
	private Drawable pointerSpot;
	private Bitmap viewIndicator;

	private Rect pointerSpotRect;
	private Rect viewRect;
	private RectF transparentRect;

	/** drawing stuff */
	private float viewCenterX = -1f;
	private float viewCenterY = -1f;
	
	public IntroViewer(final Activity activity, final String title,
			final String description) {
		super(activity);
		this.activity = activity;
		descriptionView = (RelativeLayout) LayoutInflater.from(activity)
				.inflate(R.layout.intro_description, this);
		setDescriptionView(title, description, null);

		skipButton = descriptionView.findViewById(R.id.intro_skipButton);
		skipButton.setOnTouchListener(this);
		startButton = descriptionView.findViewById(R.id.intro_startButton);
		startButton.setOnTouchListener(this);

		// Init painting Stuff
		transparentPainter = new Paint();
		transparentPainter.setAlpha(255);
		transparentPainter.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));

		pointerSpot = activity.getResources().getDrawable(
				R.drawable.pointer_spot_hover);
		viewIndicator = BitmapFactory.decodeResource(getResources(),
				R.drawable.hand);

		setOnTouchListener(this);
	}

	void updateView(final Task step) {
		// update description
		currentStep = step;
		final String t = getResources().getString(step.id);
		final String d = getResources().getString(step.description);
		final Bitmap b = step.bitmapResource == 0 ? null : BitmapFactory
				.decodeResource(getResources(), step.bitmapResource);

		this.postDelayed(new Runnable() {

			@Override
			public void run() {
				setDescriptionView(t, d, b);
				// update highlightedView
				if (step.view != null) {
					// activity.getWindow().getDecorView().postInvalidate();
					setEmphasizedView(step.view);
				}
				activity.getWindow().getDecorView().postInvalidate();
				setVisibility(View.VISIBLE);
				IntroViewer.this.invalidate();
			}
		}, 100);
	}

	private void setDescriptionView(String title, String description,
			Bitmap bitmap) {
		TextView titleTextView = (TextView) descriptionView
				.findViewById(R.id.intro_titleTextView);
		TextView descTextView = (TextView) descriptionView
				.findViewById(R.id.intro_descriptionTextView);
		ImageView descImage = (ImageView) descriptionView
				.findViewById(R.id.intro_imageView);

		titleTextView.setText(title);
		descTextView.setText(description);

		if (bitmap != null) {
			descImage.setImageBitmap(bitmap);
			descImage.setVisibility(View.VISIBLE);
		} else {
			descImage.setVisibility(View.GONE);
		}
	}

	private void setEmphasizedView(View view) {
		int[] screenPos = new int[2];
		view.getLocationOnScreen(screenPos);

		int viewWidth = view.getWidth();
		int viewHeight = view.getHeight();

		// calculate the BoundingBox of the View
		viewRect = new Rect(screenPos[0], screenPos[1], screenPos[0]
				+ viewWidth, screenPos[1] + viewHeight);

		viewCenterX = (float) (screenPos[0] + viewWidth / 2);
		viewCenterY = (float) (screenPos[1] + viewHeight / 2);

		int pointerWidth = pointerSpot.getIntrinsicWidth();
		int pointerHeight = pointerSpot.getIntrinsicHeight();

		pointerSpotRect = new Rect((int) (viewCenterX - pointerWidth / 2),
				(int) (viewCenterY - pointerHeight / 2),
				(int) (viewCenterX + pointerWidth / 2),
				(int) (viewCenterY + pointerHeight / 2));

		transparentRect = new RectF(screenPos[0], screenPos[1], screenPos[0]
				+ viewWidth, screenPos[1] + viewHeight);
	}




	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (currentStep != null && currentStep.view != null) {
			if (viewCenterX < 0 && viewCenterY < 0) {
				super.dispatchDraw(canvas);
				return;
			} else if (viewCenterX <= 0 || viewCenterY <= 0) {
				setEmphasizedView(currentStep.view);
				int[] screenPos = new int[2];
				currentStep.view.getLocationOnScreen(screenPos);

				currentStep.view.getLeft();
				currentStep.view.getLocationInWindow(screenPos);
				int x = 0;
			}

			int width = getMeasuredWidth();
			int height = getMeasuredHeight();
			transparentPainter.setShader(new RadialGradient(viewCenterX,
					viewCenterY, 200f, 0xFFFFFF,
					Color.argb(255, 255, 255, 255), Shader.TileMode.CLAMP));

			Bitmap b = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			Canvas c = new Canvas(b);
			c.drawColor(backgroundColor);
			c.drawCircle(viewCenterX, viewCenterY, 200f, transparentPainter);

			c.save();
			c.scale(2, 2, viewCenterX, viewCenterY);
			pointerSpot.setBounds(pointerSpotRect);
			pointerSpot.draw(c);
			c.restore();
			
			c.drawBitmap(viewIndicator, viewCenterX, viewCenterY, null);
			canvas.drawBitmap(b, 0, 0, null);
			c.setBitmap(null);
			b.recycle();
			b = null;
		}
		super.dispatchDraw(canvas);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		final float eventCoordX = event.getRawX();
		final float eventCoordY = event.getRawY();

		int[] pos = new int[2];

		// check skipButton;
		skipButton.getLocationOnScreen(pos);

		int top = pos[1], bottom = pos[1] + skipButton.getHeight();
		int left = pos[0], right = pos[0] + skipButton.getWidth();

		if (top < eventCoordY && bottom > eventCoordY && left < eventCoordX
				&& right > eventCoordX) {
			descriptionView.setVisibility(View.GONE);
			IntroViewer.this.setVisibility(View.GONE);
			IntroController.skipIntro();
			return true;
		}

		// check startButton;
		startButton.getLocationOnScreen(pos);

		top = pos[1];
		bottom = pos[1] + startButton.getHeight();
		left = pos[0];
		right = pos[0] + startButton.getWidth();

		if (!IntroController.isActivated && top < eventCoordY && bottom > eventCoordY
				&& left < eventCoordX && right > eventCoordX) {
			IntroController.isActivated = true;
			IntroController.handler.start();
			startButton.setVisibility(View.GONE);
			return true;
		}

		// check view
		if (viewRect != null)
			if ((eventCoordX >= viewRect.left)
					&& (eventCoordX <= viewRect.right)
					&& (eventCoordY >= viewRect.top)
					&& (eventCoordY <= viewRect.bottom)) {
				viewCenterX = 0;
				viewCenterY = 0;
			}
		return false;
	}
}