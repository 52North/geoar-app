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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.n52.geoar.R;

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
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

/**
 * 
 * @author Arne de Wall
 * 
 */
public class IntroController {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface IntroTask {

	}

	private class Initializer implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			if (method.isAnnotationPresent(IntroTask.class)) {
				IntroTask annotation = method.getAnnotation(IntroTask.class);
				// dele
			}
			// Class c; c.get
			// TODO Auto-generated method stub
			return null;
		}

	}

	private static class TaskGraph {

		private class TaskNode {
			Task step;

			private List<TaskNode> dependencies = new LinkedList<TaskNode>();

			TaskNode(Task step) {
				this.step = step;
			}

			private void addStepNode(TaskNode step) {
				this.dependencies.add(step);
			}

			private Task getStepByView(View view) {
				if (step.view == view)
					return step;
				Task step;
				for (TaskNode child : dependencies) {
					step = child.getStepByView(view);
					if (step != null)
						return step;
				}
				return null;
			}

			private Task getStepByRId(int rId) {
				if (step.title == rId)
					return step;
				Task step;
				for (TaskNode child : dependencies) {
					step = child.getStepByRId(rId);
					if (step != null)
						return step;
				}
				return null;
			}
		}

		private TaskNode download;
		private TaskNode select;
		private TaskNode activate;

		private TaskGraph() {
			initSearchStructure();
		}

		private void initSearchStructure() {
			// build up search structure
			download = new TaskNode(stepMap.get(4));
			select = new TaskNode(stepMap.get(6));
			activate = new TaskNode(stepMap.get(9));

			// Download Task
			TaskNode step1 = new TaskNode(stepMap.get(1));
			TaskNode step2 = new TaskNode(stepMap.get(2));
			step2.addStepNode(step1);

			TaskNode step3 = new TaskNode(stepMap.get(3));
			step3.addStepNode(step2);

			download.addStepNode(step3);

			// Select Task
			TaskNode step5 = new TaskNode(stepMap.get(5));
			select.addStepNode(step5);
			select.addStepNode(step2);

			// Activate Task
			TaskNode step7 = new TaskNode(stepMap.get(7));
			TaskNode step8 = new TaskNode(stepMap.get(8));

			activate.addStepNode(step7);
			activate.addStepNode(step8);
		}

		public Task getCorrespondingTask(View view) {
			if (!download.step.completed)
				return download.getStepByView(view);
			if (!select.step.completed)
				return select.getStepByView(view);
			if (!activate.step.completed)
				return activate.getStepByView(view);
			return null;
		}

		public Task getCorrespondingTask(int id) {
			if (!download.step.completed)
				return download.getStepByRId(id);
			if (!select.step.completed)
				return select.getStepByRId(id);
			if (!activate.step.completed)
				return activate.getStepByRId(id);
			return null;
		}

	}

	private static SparseArray<Task> stepMap;

	// todo init somewhere
	static {
		stepMap = new SparseArray<Task>();
		//@formatter:off
		stepMap.append(1, new Task(1,
				R.string.intro_title_1, R.string.intro_desc_1, 0, false, true){});
		stepMap.append(2, new Task(2,
				R.string.intro_title_2, R.string.intro_desc_2, 0, true, false){});
		stepMap.append(3, new Task(3,
				R.string.intro_title_3, R.string.intro_desc_3, 0, false, false){});
		stepMap.append(4, new Task(4,
				R.string.intro_title_4, R.string.intro_desc_4, R.drawable.intro_step_download, false, true){});
		stepMap.append(5, new Task(5,
				R.string.intro_title_5, R.string.intro_desc_5, 0, false, false){});
		stepMap.append(6, new Task(6,
				R.string.intro_title_6, R.string.intro_desc_6, 0, false, true){});
		stepMap.append(7, new Task(7,
				R.string.intro_title_7, R.string.intro_desc_7, 0, false, false){});
		stepMap.append(8, new Task(8,
				R.string.intro_title_8, R.string.intro_desc_8, 0, false, false){});
		stepMap.append(9, new Task(9,
				R.string.intro_title_9, R.string.intro_desc_9, 0, false, true){});
		stepMap.append(10, new Task(10,
				R.string.intro_title_10, R.string.intro_desc_10, 0, false, true){});
		//@formatter:on
		isActivated = true;
	}

	private static class Task implements Comparable<Task> {
		protected int index;

		protected boolean completed = false;
		protected boolean refreshPopupOnShow = false;
		protected boolean isAnchorStep = false;

		protected int title;
		protected int description;

		protected int bitmapResource;
		protected View view;

		public Task(int index, int title, int description, int bitmapResource,
				boolean refreshOnPopup, boolean isAnchorStep) {
			this.index = index;
			this.title = title;
			this.description = description;
			this.bitmapResource = bitmapResource;
			this.refreshPopupOnShow = refreshOnPopup;
			this.isAnchorStep = isAnchorStep;
		}

		@Override
		public int compareTo(Task another) {
			return this.index > another.index ? 1 : -1;
		}
	}

	private static boolean isActivated = false;
	private static PopupWindow popup;
	private static IntroViewer introViewer;
	private static Activity activity;

	private static Task currentStep;
	private static Task nextStep;

	private static View viewIndicator;
	private static Integer idIndicator;
	private static TaskGraph graph;

	private static Object mutex = new Object();

	private static Thread handler = new Thread(new Runnable() {

		private void waitForEvent() throws InterruptedException {
			synchronized (mutex) {
				while (idIndicator == null && viewIndicator == null)
					mutex.wait();
				if (viewIndicator == null) {
					final Task task = graph.getCorrespondingTask(idIndicator);
					if (task != null)
						introViewer.updateView(task);
					idIndicator = null;
				} else {
					final Task task = graph.getCorrespondingTask(viewIndicator);
					if (task != null)
						introViewer.updateView(task);
					viewIndicator = null;
				}
			}
		}

		@Override
		public void run() {
			synchronized (mutex) {
				if (currentStep == null) {
					currentStep = stepMap.get(1);
					introViewer.updateView(currentStep);
				}
			}

			// todo use isActivated
			while (true) {
				try {
					waitForEvent();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	});

	public static void initPopupShow(final Activity activity) {
		IntroController.activity = activity;
		introViewer = new IntroViewer(activity, activity.getResources()
				.getString(R.string.intro_start_title), activity.getResources()
				.getString(R.string.intro_start_desc));

		graph = new TaskGraph();

		popup = new PopupWindow(introViewer, LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		popup.setTouchable(false);
		popup.setFocusable(true);
		popup.setOutsideTouchable(true);
		popup.setTouchInterceptor(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return false;
			}

		});

		activity.getWindow().getDecorView().post(new Runnable() {
			@Override
			public void run() {
				popup.showAtLocation(activity.getWindow().getDecorView()
						.getRootView(), Gravity.TOP, 0, 0);
			}
		});
	}

	public static void addViewToStep(int i, View view) {
		synchronized (mutex) {
			stepMap.get(i).view = view;
		}
	}

	public static void notify(View view) {
		synchronized (mutex) {
			Task task = graph.getCorrespondingTask(view);
			viewIndicator = view;
			mutex.notifyAll();
		}
	}

	public static void notify(int rId) {
		synchronized (mutex) {
			Task task = graph.getCorrespondingTask(rId);
			idIndicator = rId;
			mutex.notifyAll();
		}
	}

	public static void skipIntro() {
		isActivated = false;
		introViewer.setVisibility(View.GONE);
		introViewer = null;
		popup.dismiss();
		popup = null;
	}

	public static void taskCompleted(int i) {
		synchronized (stepMap) {
			Task step = stepMap.get(i);
			step.completed = true;
		}
	}

	private static class IntroViewer extends RelativeLayout implements
			View.OnTouchListener {

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

		private float pointerRectScaleX;
		private float pointerRectScaleY;

		public IntroViewer(final Activity context, final String title,
				final String description) {
			super(context);
			descriptionView = (RelativeLayout) LayoutInflater.from(context)
					.inflate(R.layout.intro_description, this);
			setDescriptionView(title, description, null);

			skipButton = descriptionView.findViewById(R.id.intro_skipButton);
			skipButton.setOnTouchListener(this);
			startButton = descriptionView.findViewById(R.id.intro_startButton);
			startButton.setOnTouchListener(this);

			// Init painting Stuff
			transparentPainter = new Paint();
			// transparentPainter.setColor(0xFFFFFF);
			transparentPainter.setAlpha(255);
			transparentPainter
					.setXfermode(new PorterDuffXfermode(Mode.MULTIPLY));

			pointerSpot = context.getResources().getDrawable(
					R.drawable.pointer_spot_hover);
			viewIndicator = BitmapFactory.decodeResource(getResources(),
					R.drawable.hand);

			setOnTouchListener(this);
		}

		private void updateView(final Task step) {
			// update description
			currentStep = step;
			final String t = getResources().getString(step.title);
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
						if (step.refreshPopupOnShow)
							refreshPopup();
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

			transparentRect = new RectF(screenPos[0], screenPos[1],
					screenPos[0] + viewWidth, screenPos[1] + viewHeight);

			pointerRectScaleX = ((float) viewWidth) / pointerWidth;
			pointerRectScaleY = ((float) viewHeight) / pointerHeight;
		}

		private void refreshPopup() {
			popup.dismiss();
			activity.getWindow().getDecorView().post(new Runnable() {
				@Override
				public void run() {
					popup.showAtLocation(activity.getWindow().getDecorView()
							.getRootView(), Gravity.TOP, 0, 0);
				}
			});
		}

		@Override
		protected void dispatchDraw(Canvas canvas) {
			if (currentStep != null && currentStep.view != null) {
				if (viewCenterX < 0 && viewCenterY < 0) {
					super.dispatchDraw(canvas);
					return;
				}

				int width = getMeasuredWidth();
				int height = getMeasuredHeight();
				transparentPainter.setShader(new RadialGradient(viewCenterX,
						viewCenterY, 200f, 0xFFFFFF, Color.argb(255, 255, 255,
								255), Shader.TileMode.CLAMP));

				Bitmap b = Bitmap.createBitmap(width, height,
						Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(b);
				c.drawColor(backgroundColor);
				// c.drawRect(transparentRect, transparentPainter);
				c.drawCircle(viewCenterX, viewCenterY, 200f, transparentPainter);

				c.save();
				// c.scale(pointerRectScaleX, pointerRectScaleY, viewCenterX,
				// viewCenterY);
				// c.drawOval(pointerSpotRectF, transparentPainter);
				// c.drawCircle(screenCoordX, screenCoordY, spotRadius,
				// transparentPainter);
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
				skipIntro();
				return true;
			}

			// check startButton;
			startButton.getLocationOnScreen(pos);

			top = pos[1];
			bottom = pos[1] + startButton.getHeight();
			left = pos[0];
			right = pos[0] + startButton.getWidth();

			if (!isActivated && top < eventCoordY && bottom > eventCoordY
					&& left < eventCoordX && right > eventCoordX) {
				isActivated = true;
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
					this.setVisibility(View.GONE);
					// introModel.
				}
			return false;
		}
	}
}
