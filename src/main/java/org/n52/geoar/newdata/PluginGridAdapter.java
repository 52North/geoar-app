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
package org.n52.geoar.newdata;

import java.util.List;

import org.n52.geoar.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

public class PluginGridAdapter<T extends PluginHolder> extends BaseAdapter {

	interface OnItemCheckedListener {
		void onItemChecked(boolean newState, int position);
	}

	private class ViewHolder {
		ImageView imageView;
		TextView textViewTitle;
		CheckBox checkBox;
		OnGridCheckedChangeListener checkedListener;
		ImageTask imageTask;
		public TextView textViewSubTitle;
		public TextView textViewStatus;
	}

	private class ImageTask extends AsyncTask<Void, Void, Bitmap> {

		private ImageView imageView;
		private T plugin;

		public ImageTask(ImageView imageView, T plugin) {
			this.imageView = imageView;
			this.plugin = plugin;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			return plugin.getPluginIcon();
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			imageView.setImageBitmap(result);
		}

	}

	private boolean showCheckBox = false;

	private class OnGridCheckedChangeListener implements
			OnCheckedChangeListener {

		private int position;

		public void setPosition(int position) {
			this.position = position;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (itemCheckedListener != null) {
				itemCheckedListener.onItemChecked(isChecked, position);
			}
		}

	}

	protected List<T> plugins;
	private LayoutInflater inflater;
	private Context mContext;

	public PluginGridAdapter(Context context) {
		mContext = context;
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private OnItemCheckedListener itemCheckedListener;

	public void setOnItemCheckedListener(OnItemCheckedListener listener) {
		this.itemCheckedListener = listener;
	}

	@Override
	public int getCount() {
		if (plugins != null)
			return plugins.size();
		return 0;
	}

	@Override
	public T getItem(int position) {
		if (plugins != null && position < getCount() && position >= 0)
			return plugins.get(position);
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewHolder viewHolder;

		if (view == null) {
			view = inflater.inflate(R.layout.cb_grid_item, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.imageView = (ImageView) view
					.findViewById(R.id.imageView);
			viewHolder.textViewTitle = (TextView) view
					.findViewById(R.id.textViewTitle);
			viewHolder.textViewSubTitle = (TextView) view
					.findViewById(R.id.textViewSubTitle);
			viewHolder.textViewStatus = (TextView) view
					.findViewById(R.id.textViewStatus);
			viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkBox);
			if (showCheckBox) {
				viewHolder.checkedListener = new OnGridCheckedChangeListener();
				viewHolder.checkBox
						.setOnCheckedChangeListener(viewHolder.checkedListener);
				viewHolder.checkBox.setVisibility(View.VISIBLE);
			} else {
				viewHolder.checkBox.setVisibility(View.GONE);
			}
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		if (viewHolder.checkedListener != null) {
			viewHolder.checkedListener.setPosition(position);
		}

		final T plugin = plugins.get(position);
		// load image via imageCache
		// ImageLoader.getInstance().displayImage("", viewHolder.imageView); //
		// TODO?

		if (viewHolder.imageTask != null) {
			viewHolder.imageView.setImageBitmap(null);
			viewHolder.imageTask.cancel(true);
		}
		viewHolder.imageTask = new ImageTask(viewHolder.imageView, plugin);
		viewHolder.imageTask.execute((Void) null);

		viewHolder.textViewTitle.setText(plugin.getName());
		viewHolder.textViewSubTitle
				.setText(plugin.getPublisher() != null ? plugin.getPublisher()
						: mContext.getString(R.string.unknown_publisher));
		viewHolder.textViewStatus.setText(getPluginStatus(plugin));
		viewHolder.checkBox.setChecked(getItemChecked(position));

		return view;
	}

	protected String getPluginStatus(T plugin) {
		return "";
	}

	protected boolean getItemChecked(int position) {
		return false;
	}

	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

}