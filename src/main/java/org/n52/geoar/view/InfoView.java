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
package org.n52.geoar.view;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.n52.geoar.GeoARApplication;
import org.n52.geoar.R;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Core information bar. Takes records of objects which currently try to show
 * information and presents them as possible
 * 
 * @author Holger Hopmann, Arne de Wall
 * 
 */
public class InfoView extends LinearLayout {

	private static final int MESSAGE_REFRESH = 1;

	private static class ProgressHolder {
		private int progress, maxProgress;
	}

	private static class StatusHolder {
		private long clearTime;
		private String status = "";
		private Object id;
	}

	private interface OnChangeListener {
		void onChange();
	}

	private static Handler updatehandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == MESSAGE_REFRESH) {
				notifyStatusChangeListeners();
			}
		}
	};

	private OnChangeListener statusChangeListener = new OnChangeListener() {
		@Override
		public void onChange() {
			statusUpdate();
		}
	};

	private OnChangeListener progressChangeListener = new OnChangeListener() {
		@Override
		public void onChange() {
			progressUpdate();
		}
	};

	private ProgressBar infoProgressBar;

	// HashMaps of all identifiers and their information to show
	private static Map<Object, ProgressHolder> progressHolderMap = new HashMap<Object, ProgressHolder>();
	private static LinkedHashMap<Object, StatusHolder> statusHolderMap = new LinkedHashMap<Object, StatusHolder>(
			0, 1, true);
	private static Set<OnChangeListener> statusChangeListeners = new HashSet<InfoView.OnChangeListener>();
	private static Set<OnChangeListener> progressChangeListeners = new HashSet<InfoView.OnChangeListener>();

	private StatusHolder currentStatus;
	private ProgressHolder currentProgress = new ProgressHolder();

	private TextView infoStatusTextView;
	private Runnable updateViewsRunnable = new Runnable() {
		public void run() {
			updateRunnableQueued = false;
			updateViews();
		}
	};
	private boolean updateRunnableQueued;

	public InfoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);

		// inflate layout
		LayoutInflater.from(context).inflate(R.layout.infoviews, this, true);

		infoStatusTextView = (TextView) findViewById(R.id.textViewStatusInfo);
		infoProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		statusChangeListeners.add(statusChangeListener);
		progressChangeListeners.add(progressChangeListener);

		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				statusUpdate();
			}
		}, 0, 3000);

		statusUpdate();
		updateViews();
	}

	private void statusUpdate() {
		synchronized (statusHolderMap) {
			if (statusHolderMap.size() != 0) {
				// get next status to show
				Iterator<StatusHolder> iterator = statusHolderMap.values()
						.iterator();
				long now = SystemClock.uptimeMillis();
				currentStatus = null;
				// try to get the very first one which is not outdated
				while (iterator.hasNext()) {
					StatusHolder holder = iterator.next();
					if (holder.clearTime <= now) {
						iterator.remove();
					} else {
						currentStatus = holder;
						break;
					}
				}
				if (currentStatus != null) {
					// just get the object which we already have, to
					// update the internal access order so that that
					// element gets biased against the others.
					statusHolderMap.get(currentStatus.id);
				}
				refresh();
			} else if (currentStatus != null) {
				currentStatus = null;
			}
		}
	}

	private void progressUpdate() {
		synchronized (progressHolderMap) {
			currentProgress.progress = 0;
			currentProgress.maxProgress = 0;
			for (ProgressHolder holder : progressHolderMap.values()) {
				currentProgress.progress += holder.progress;
				currentProgress.maxProgress += holder.maxProgress;
			}

			if (currentProgress.progress >= currentProgress.maxProgress) {
				for (ProgressHolder holder : progressHolderMap.values()) {
					clearStatus(holder);
				}
				progressHolderMap.clear();
			}
			refresh();
		}
	}

	private static void notifyStatusChangeListeners() {
		for (OnChangeListener listener : statusChangeListeners)
			listener.onChange();
	}

	private static void notifyProgressChangeListeners() {
		for (OnChangeListener listener : progressChangeListeners)
			listener.onChange();
	}

	/**
	 * Allows to update views state from any thread
	 */
	private void refresh() {
		if (!updateRunnableQueued) {
			updateRunnableQueued = true;
			post(updateViewsRunnable);
		}
	}

	/**
	 * Set the specific status for the identification object
	 * 
	 * @param progress
	 * @param maxProgress
	 * @param id
	 */
	public static void setProgress(int progress, int maxProgress, Object id) {
		synchronized (progressHolderMap) {

			ProgressHolder holder = progressHolderMap.get(id);
			if (holder == null) {
				holder = new ProgressHolder();
				progressHolderMap.put(id, holder);
			}
			holder.maxProgress = maxProgress;
			holder.progress = progress;
			if (holder.progress >= holder.maxProgress) {
				clearStatus(holder);
			}
		}
		notifyProgressChangeListeners();
	}

	/**
	 * sets a progress title for this id
	 * 
	 * @param title
	 * @param id
	 */
	public static void setProgressTitle(String title, Object id) {
		synchronized (progressHolderMap) {
			ProgressHolder holder = progressHolderMap.get(id);
			if (holder == null) {
				holder = new ProgressHolder();
				progressHolderMap.put(id, holder);
			}
			setStatus(title, -1, holder);
		}
	}

	public static void setProgressTitle(int stringId, Object id) {
		setProgressTitle(
				GeoARApplication.applicationContext.getString(stringId), id);
	}

	/**
	 * Sets status text for id
	 * 
	 * @param stringId
	 * @param maxDuration
	 * @param id
	 */
	public static void setStatus(int stringId, int maxDuration, Object id) {
		setStatus(GeoARApplication.applicationContext.getString(stringId),
				maxDuration, id);
	}

	/**
	 * Sets status text for id
	 * 
	 * @param status
	 * @param maxDuration
	 * @param id
	 */
	public static void setStatus(String status, int maxDuration, Object id) {
		boolean update = false;
		synchronized (statusHolderMap) {
			StatusHolder holder = statusHolderMap.get(id);
			if (holder == null) {
				holder = new StatusHolder();
				holder.id = id;
				statusHolderMap.put(id, holder);
				update = true;
			}
			if (holder.status != null && !holder.status.equals(status)) {
				update = true;
			}
			holder.status = status;
			if (maxDuration != -1) {
				holder.clearTime = SystemClock.uptimeMillis() + maxDuration;
			} else {
				holder.clearTime = Long.MAX_VALUE;
			}
			updatehandler.sendMessageAtTime(
					updatehandler.obtainMessage(MESSAGE_REFRESH),
					holder.clearTime);
		}

		if (update) {
			notifyStatusChangeListeners();
		}
	}

	/**
	 * Clears status text for id
	 * 
	 * @param id
	 */
	public static void clearStatus(Object id) {
		synchronized (statusHolderMap) {
			statusHolderMap.remove(id);
		}
		notifyStatusChangeListeners();
	}

	/**
	 * Updates views to reflect current status and progress
	 */
	private void updateViews() {
		boolean isVisible = false;
		synchronized (progressHolderMap) {
			if (currentProgress.progress < currentProgress.maxProgress) {
				infoProgressBar.setMax(currentProgress.maxProgress);
				infoProgressBar.setProgress(currentProgress.progress);

				infoProgressBar.setVisibility(View.VISIBLE);
				isVisible = true;
			} else {
				infoProgressBar.setVisibility(View.GONE);
			}
		}
		synchronized (statusHolderMap) {
			if (currentStatus != null) {
				infoStatusTextView.setText(currentStatus.status);
				infoStatusTextView.setVisibility(View.VISIBLE);
				isVisible = true;
			} else {
				infoStatusTextView.setVisibility(View.GONE);
			}
		}

		if (isVisible) {
			setVisibility(View.VISIBLE);
		} else {
			setVisibility(View.GONE);
		}
	}

	/**
	 * Clears progress by id
	 * 
	 * @param id
	 */
	public static void clearProgress(Object id) {
		setProgress(0, 0, id);
	}

}