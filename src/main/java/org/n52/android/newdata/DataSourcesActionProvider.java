package org.n52.android.newdata;

import org.n52.android.R;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesChangeListener;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.actionbarsherlock.view.ActionProvider;

public class DataSourcesActionProvider extends ActionProvider {

	Context mContext;
	private PopupWindow mPopup;

	public DataSourcesActionProvider(Context context) {
		super(context);
		mContext = context;
	}

	@Override
	public View onCreateActionView() {
		// Inflate the action view to be shown on the action bar.

		ImageButton imageButton = new ImageButton(mContext);
		imageButton.setImageResource(R.drawable.ic_menu_chart);

		return imageButton;
	}

	private PopupWindow getPopup() {
		if (mPopup == null) {
			ExpandableListView listView = new ExpandableListView(mContext);
			DataSourceListAdapter sourceListAdapter = new DataSourceListAdapter();
			listView.setAdapter(sourceListAdapter);
			PopupWindow popup = new PopupWindow(listView);
			popup.setWindowLayoutMode(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
		}
		return mPopup;
	}

	private class DataSourceListAdapter extends BaseExpandableListAdapter
			implements OnDataSourcesChangeListener {

		private class OnDataSourceCheckedChangeListener implements
				OnCheckedChangeListener {

			private int position;

			public void setPosition(int position) {
				this.position = position;
			}

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {

			}

		}

		private class DataSourceViewHolder {
			public ImageView imageView;
			public TextView textView;
			public CheckBox checkBox;
			public OnDataSourceCheckedChangeListener checkListener;
		}

		private CheckList<DataSourceHolder> selectedDataSources;
		private LayoutInflater mInflater;

		public DataSourceListAdapter() {
			mInflater = LayoutInflater.from(mContext);

			selectedDataSources = DataSourceLoader.getSelectedDataSources();

			DataSourceLoader.addOnSelectedDataSourcesUpdateListener(this);
			// TODO remove listener somehow
		}

		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			return null;
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			

			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return selectedDataSources.get(groupPosition).getVisualizations()
					.size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return selectedDataSources.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return selectedDataSources.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View view, ViewGroup parent) {
			DataSourceViewHolder viewHolder;

			if (view == null) {
				view = mInflater.inflate(R.layout.datasource_list_item, parent,
						false);
				viewHolder = new DataSourceViewHolder();
				viewHolder.imageView = (ImageView) view
						.findViewById(R.id.imageView);
				viewHolder.textView = (TextView) view
						.findViewById(R.id.textView);
				viewHolder.checkBox = (CheckBox) view
						.findViewById(R.id.checkBox);

				viewHolder.checkListener = new OnDataSourceCheckedChangeListener();
				viewHolder.checkBox
						.setOnCheckedChangeListener(viewHolder.checkListener);
				viewHolder.checkBox.setVisibility(View.VISIBLE);

				view.setTag(viewHolder);
			} else {
				viewHolder = (DataSourceViewHolder) view.getTag();
			}

			viewHolder.checkListener.setPosition(groupPosition);
			DataSourceHolder dataSource = selectedDataSources
					.get(groupPosition);

			viewHolder.textView.setText(dataSource.getName());
			viewHolder.checkBox.setChecked(selectedDataSources
					.isChecked(dataSource));

			return view;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		@Override
		public boolean isEmpty() {
			return selectedDataSources.isEmpty();
		}

		@Override
		public void onDataSourcesChange() {
			notifyDataSetChanged();
		}

	}
}
