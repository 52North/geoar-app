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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.n52.geoar.R;

import android.app.Activity;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.PopupWindow;
import android.widget.RelativeLayout.LayoutParams;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class IntroController {

	private static class TaskGraph {
		private class TaskNode {
			private Task step;
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
				if (step.description == rId)
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

		private Queue<TaskNode> tasksQueue = new LinkedList<TaskNode>();
		private TaskNode currentTask;

		private TaskGraph() {
			initSearchStructure();
		}

		private void initSearchStructure() {
			// build up search structure
			TaskNode download = new TaskNode(stepMap.get(4));
			TaskNode select = new TaskNode(stepMap.get(6));
			TaskNode activate = new TaskNode(stepMap.get(9));
			tasksQueue.offer(download);
			tasksQueue.offer(select);
			tasksQueue.offer(activate);

			// Download Task
			TaskNode step1 = new TaskNode(stepMap.get(1));
			TaskNode step2 = new TaskNode(stepMap.get(2));
			TaskNode step3 = new TaskNode(stepMap.get(3));
			step2.addStepNode(step1);
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

			currentTask = tasksQueue.poll();
		}

		public Task getCorrespondingTask(View view) {
			return currentTask.getStepByView(view);
		}

		public Task getCorrespondingTask(int id) {
			return currentTask.getStepByRId(id);
		}

		public void checkCondition(int id) {
			if (currentTask == null)
				return;
			if (currentTask.step.id == id) {
				currentTask = tasksQueue.poll();
				if (currentTask == null)
					skipIntro();
			}
		}

	}

	private static SparseArray<Task> stepMap;

	// todo init somewhere
	static {
		stepMap = new SparseArray<Task>();
		//@formatter:off
		stepMap.append(1, new Task(1,
				R.string.intro_task_1, R.string.intro_desc_1_1, 0, false){});
		stepMap.append(2, new Task(2,
				R.string.intro_task_1, R.string.intro_desc_1_2, 0, true){});
		stepMap.append(3, new Task(3,
				R.string.intro_task_1, R.string.intro_desc_1_3, 0, false){});
		stepMap.append(4, new Task(4,
				R.string.intro_task_1, R.string.intro_desc_1_4, R.drawable.intro_step_download, false){});
		stepMap.append(5, new Task(5,
				R.string.intro_task_2, R.string.intro_desc_2_1, 0, false){});
		stepMap.append(6, new Task(6,
				R.string.intro_task_2, R.string.intro_desc_2_2, 0, false){});
		stepMap.append(7, new Task(7,
				R.string.intro_task_3, R.string.intro_desc_3_1, 0, false){});
		stepMap.append(8, new Task(8,
				R.string.intro_task_3, R.string.intro_desc_3_2, 0, false){});
		stepMap.append(9, new Task(9,
				R.string.intro_task_3, R.string.intro_desc_3_3, 0, false){});
		stepMap.append(10, new Task(10,
				R.string.intro_task_3, R.string.intro_desc_3_4, 0, false){});
		//@formatter:on
		isActivated = true;
	}

	public static class Task implements Comparable<Task> {
		protected int index;

		protected boolean completed = false;
		protected boolean refreshPopupOnShow = false;

		protected int id;
		protected int description;

		protected int bitmapResource;
		protected View view;

		public Task(int index, int title, int description, int bitmapResource,
				boolean refreshOnPopup) {
			this.index = index;
			this.id = title;
			this.description = description;
			this.bitmapResource = bitmapResource;
			this.refreshPopupOnShow = refreshOnPopup;
		}

		@Override
		public int compareTo(Task another) {
			return this.index > another.index ? 1 : -1;
		}
	}

	static boolean isActivated = false;
	private static boolean started = false;
	private static PopupWindow popup;
	private static IntroViewer introViewer;
	private static Activity activity;

	private static TaskGraph graph;
	private static Task currentStep;

	private static View viewIndicator;
	private static Integer idIndicator;

	private static Object mutex = new Object();

	static Thread handler = new Thread(new Runnable() {

		private void waitForEvent() throws InterruptedException {
			synchronized (mutex) {
				while (idIndicator == null && viewIndicator == null)
					mutex.wait();
				if (viewIndicator == null) {
					final Task task = graph.getCorrespondingTask(idIndicator);
					if (task != null) {
						introViewer.updateView(task);
						if (task.refreshPopupOnShow)
							refreshPopup();
					}
					idIndicator = null;
				} else {
					final Task task = graph.getCorrespondingTask(viewIndicator);
					if (task != null) {
						introViewer.updateView(task);
						if (task.refreshPopupOnShow)
							refreshPopup();
					}
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
	
	public static void initPopupShow(final Activity activity){
		IntroController.activity = activity;
	}
	
	private static void initPopupShow() {
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
				if(popup!=null)
				popup.showAtLocation(activity.getWindow().getDecorView()
						.getRootView(), Gravity.TOP, 0, 0);
			}
		});
	}

	public static void addViewToStep(int i, View view) {
		if (stepMap != null)
			synchronized (mutex) {
				stepMap.get(i).view = view;
			}
	}

	public static void notify(View view) {
		if (isActivated)
			synchronized (mutex) {
				viewIndicator = view;
				mutex.notifyAll();
			}
	}

	public static void notify(int rId) {
		if (isActivated)
			synchronized (mutex) {
				idIndicator = rId;
				mutex.notifyAll();
			}
	}
	
	public static void startIntro(boolean hasDataSources) {
		if(!started && !hasDataSources ){
			started = true;
			initPopupShow();
			return;
		}
		hasDataSources = false;
	}

	public static void skipIntro() {
		graph = null;
		currentStep = null;
		handler = null;
		stepMap = null;
		isActivated = false;
		introViewer.setVisibility(View.GONE);
		introViewer = null;
		popup.dismiss();
		popup = null;
	}

	public static void finishTaskIfActive(int i) {
		if (isActivated)
			synchronized (stepMap) {
				graph.checkCondition(i);
			}
	}

	private static void refreshPopup() {
		if (!isActivated)
			return;
		activity.getWindow().getDecorView().post(new Runnable() {
			@Override
			public void run() {
				popup.dismiss();
				popup.showAtLocation(activity.getWindow().getDecorView()
						.getRootView(), Gravity.TOP, 0, 0);
			}
		});
	}

}
