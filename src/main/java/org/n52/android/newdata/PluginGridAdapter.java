package org.n52.android.newdata;

import java.util.List;

import org.n52.android.data.ImageLoader;
import org.n52.android.geoar.R;

import android.content.Context;
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
		void onItemChecked(boolean newState, int position, long id);
	}

	private boolean showCheckBox = false;

	private class ViewHolder {
		public ImageView imageView;
		public TextView textView;
		public CheckBox checkBox;
	}

	private class OnGridCheckedChangeListener implements
			OnCheckedChangeListener {

		private int position;
		private int id;

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (itemCheckedListener != null) {
				itemCheckedListener.onItemChecked(isChecked, position, id);
			}
		}

	}

	protected List<T> dataSources;
	private LayoutInflater inflater;

	public PluginGridAdapter(Context context) {
		inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	private OnItemCheckedListener itemCheckedListener;

	public void setOnItemCheckedListener(OnItemCheckedListener listener) {
		this.itemCheckedListener = listener;
	}

	@Override
	public int getCount() {
		if (dataSources != null)
			return dataSources.size();
		return 0;
	}

	@Override
	public T getItem(int position) {
		if (dataSources != null && position < getCount() && position >= 0)
			return dataSources.get(position);
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
					.findViewById(R.id.cb_grid_image);
			viewHolder.textView = (TextView) view
					.findViewById(R.id.cb_grid_label);
			viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkBox);
			if (showCheckBox) {
				viewHolder.checkBox
						.setOnCheckedChangeListener(new OnGridCheckedChangeListener());
				viewHolder.checkBox.setVisibility(View.VISIBLE);
			} else {
				viewHolder.checkBox.setVisibility(View.GONE);
			}
			view.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) view.getTag();
		}

		final T dataSource = dataSources.get(position);
		// load image via imageCache
		ImageLoader.getInstance().displayImage("", viewHolder.imageView); // TODO?
		viewHolder.textView.setText(dataSource.getName());
		viewHolder.checkBox.setChecked(getItemChecked(position));

		return view;
	}

	protected boolean getItemChecked(int position) {
		return false;
	}

	public void setShowCheckBox(boolean showCheckBox) {
		this.showCheckBox = showCheckBox;
	}

}