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
package org.n52.android.view;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.n52.android.geoar.R;

import android.content.Context;
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
 * @author Holger Hopmann
 * 
 */
public class InfoView extends LinearLayout {

	private class ProgressHolder {
		private int progress, maxProgress;
		private String title = "";
	}

	private class StatusHolder {
		private long clearTime;
		private String status = "";
		private Object id;
	}

	private ProgressBar infoProgressBar;

	// HashMaps of all identifiers and their information to show
	LinkedHashMap<Object, ProgressHolder> progressHolderMap = new LinkedHashMap<Object, ProgressHolder>();
	LinkedHashMap<Object, StatusHolder> statusHolderMap = new LinkedHashMap<Object, StatusHolder>(
			0, 1, true);
	StatusHolder currentStatus;

	private TextView infoProgressTextView;
	private TextView infoStatusTextView;
	Runnable updateViewsRunnable = new Runnable() {
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
		LayoutInflater.from(context).inflate(R.layout.info_views, this, true);

		infoStatusTextView = (TextView) findViewById(R.id.textViewStatusInfo);
		infoProgressTextView = (TextView) findViewById(R.id.textViewProgressInfo);
		infoProgressBar = (ProgressBar) findViewById(R.id.progressBar);

		// Task continuously updates the status text
		TimerTask statusUpdateTask = new TimerTask() {
			@Override
			public void run() {
				synchronized (statusHolderMap) {
					if (statusHolderMap.size() != 0) {
						// get next status to show
						Iterator<StatusHolder> iterator = statusHolderMap
								.values().iterator();
						long now = System.currentTimeMillis();
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
		};

		Timer t = new Timer(true);
		// Schedule status update thread
		t.scheduleAtFixedRate(statusUpdateTask, 0, 3000);

		updateViews();
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
	public void setProgress(int progress, int maxProgress, Object id) {
		synchronized (progressHolderMap) {
			if (progress < maxProgress) {
				ProgressHolder holder = progressHolderMap.get(id);
				if (holder == null) {
					holder = new ProgressHolder();
					progressHolderMap.put(id, holder);
				}
				holder.maxProgress = maxProgress;
				holder.progress = progress;
			} else {
				progressHolderMap.remove(id);
			}
		}
		refresh();
	}

	/**
	 * sets a progress title for this id
	 * 
	 * @param title
	 * @param id
	 */
	public void setProgressTitle(String title, Object id) {
		synchronized (progressHolderMap) {
			ProgressHolder holder = progressHolderMap.get(id);
			if (holder == null) {
				holder = new ProgressHolder();
				progressHolderMap.put(id, holder);
			}
			holder.title = title;
		}
		refresh();
	}

	/**
	 * Sets status text for id
	 * 
	 * @param stringId
	 * @param maxDuration
	 * @param id
	 */
	public void setStatus(int stringId, int maxDuration, Object id) {
		setStatus(getResources().getString(stringId), maxDuration, id);
	}

	/**
	 * Sets status text for id
	 * 
	 * @param status
	 * @param maxDuration
	 * @param id
	 */
	public void setStatus(String status, int maxDuration, Object id) {
		synchronized (statusHolderMap) {
			StatusHolder holder = statusHolderMap.get(id);
			if (holder == null) {
				holder = new StatusHolder();
				holder.id = id;
				statusHolderMap.put(id, holder);
			}
			holder.status = status;
			if (maxDuration != -1) {
				holder.clearTime = System.currentTimeMillis() + maxDuration;
			} else {
				holder.clearTime = Long.MAX_VALUE;
			}
		}
		refresh();
	}

	/**
	 * Clears status text for id
	 * @param id
	 */
	public void clearStatus(Object id) {
		synchronized (statusHolderMap) {
			statusHolderMap.remove(id);
		}
		refresh();
	}

	/**
	 * Updates views to reflect current status and progress
	 */
	private void updateViews() {
		boolean isVisible = false;
		synchronized (progressHolderMap) {
			if (progressHolderMap.size() != 0) {
				ProgressHolder progressHolder = progressHolderMap.values()
						.iterator().next();
				infoProgressBar.setMax(progressHolder.maxProgress);
				infoProgressBar.setProgress(progressHolder.progress);
				infoProgressTextView.setText(progressHolder.title);

				infoProgressTextView.setVisibility(View.VISIBLE);
				infoProgressBar.setVisibility(View.VISIBLE);
				isVisible = true;
			} else {
				infoProgressTextView.setVisibility(View.GONE);
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
	public void clearProgress(Object id) {
		setProgress(0, 0, id);
	}

}