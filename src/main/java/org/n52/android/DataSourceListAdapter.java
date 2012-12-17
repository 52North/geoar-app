package org.n52.android;

import java.util.List;

import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.InstalledPluginHolder;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.newdata.Visualization;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

public class DataSourceListAdapter extends BaseExpandableListAdapter {

	private ExpandableListView listView;
	private int VIEW_TYPE_DATASOURCE = 0;
	private int VIEW_TYPE_DATASOURCEINSTANCE = 1;

	/**
	 * Holder for group items
	 * 
	 */
	private class DataSourceViewHolder {
		int groupPosition;
		DataSourceHolder dataSource;
		ImageView imageViewSettings;
		TextView textView;
		CheckBox checkBox;
		OnCheckedChangeListener checkListener = new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (dataSource.areAllChecked() != isChecked) {
					dataSource.setChecked(isChecked);
					notifyDataSetChanged();
				}
			}
		};
		OnClickListener clickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!listView.isGroupExpanded(groupPosition))
					listView.expandGroup(groupPosition);
				else
					listView.collapseGroup(groupPosition);
			}
		};
		OnClickListener settingsClickListener = new OnClickListener() {

			@Override
			public void onClick(View v) {
				dataSource.createFilterDialog(mContext);
			}
		};
	}

	/**
	 * Holder for child items
	 * 
	 */
	private class DataSourceInstanceViewHolder {
		DataSourceInstanceHolder dataSourceInstance;
		ImageView imageViewSettings;
		TextView textView;
		CheckBox checkBox;
		OnCheckedChangeListener checkListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (dataSourceInstance.isChecked() != isChecked) {
					dataSourceInstance.setChecked(isChecked);
					notifyDataSetChanged();
				}
			}
		};
		OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO
			}
		};
		OnClickListener settingsClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dataSourceInstance.createFilterDialog(mContext);
			}
		};
	}

	private Context mContext;
	private LayoutInflater mInflater;
	private List<DataSourceHolder> mDataSources;
	private OnCheckedChangedListener<InstalledPluginHolder> mPluginChangedListener = new OnCheckedChangedListener<InstalledPluginHolder>() {

		@Override
		public void onCheckedChanged(InstalledPluginHolder item,
				boolean newState) {
			mDataSources = PluginLoader.getDataSources();
			notifyDataSetChanged();
		}
	};

	public <E extends Visualization> DataSourceListAdapter(Context context,
			ExpandableListView listView) {
		this.listView = listView;
		this.mContext = context;
		mInflater = LayoutInflater.from(context);
		mDataSources = PluginLoader.getDataSources();
		PluginLoader.getInstalledPlugins().addOnCheckedChangeListener(
				mPluginChangedListener);
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
			boolean isLastChild, View view, ViewGroup parent) {

		DataSourceInstanceHolder dataSourceInstance = mDataSources
				.get(groupPosition).getInstances().get(childPosition);
		return getDataSourceInstanceView(dataSourceInstance, view, parent);
	}

	private View getDataSourceInstanceView(
			DataSourceInstanceHolder dataSourceInstance, View view,
			ViewGroup parent) {
		DataSourceInstanceViewHolder viewHolder;

		if (view == null) {
			view = mInflater.inflate(
					R.layout.datasource_list_datasourceinstance_item, parent,
					false);
			viewHolder = new DataSourceInstanceViewHolder();
			viewHolder.imageViewSettings = (ImageView) view
					.findViewById(R.id.imageViewSettings);
			viewHolder.imageViewSettings
					.setOnClickListener(viewHolder.settingsClickListener);

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textView.setOnClickListener(viewHolder.clickListener);

			viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkBox);

			viewHolder.checkBox
					.setOnCheckedChangeListener(viewHolder.checkListener);

			view.setTag(viewHolder);
		} else {
			viewHolder = (DataSourceInstanceViewHolder) view.getTag();
		}

		viewHolder.dataSourceInstance = dataSourceInstance;
		viewHolder.textView.setText(dataSourceInstance.getName());
		viewHolder.checkBox.setChecked(viewHolder.dataSourceInstance
				.isChecked());

		return view;
	}

	private View getDataSourceView(DataSourceHolder dataSource,
			int groupPosition, View view, ViewGroup parent) {
		DataSourceViewHolder viewHolder;

		if (view == null) {
			view = mInflater.inflate(R.layout.datasource_list_datasource_item,
					parent, false);
			viewHolder = new DataSourceViewHolder();
			viewHolder.imageViewSettings = (ImageView) view
					.findViewById(R.id.imageViewSettings);
			viewHolder.imageViewSettings
					.setOnClickListener(viewHolder.settingsClickListener);

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textView.setOnClickListener(viewHolder.clickListener);

			viewHolder.checkBox = (CheckBox) view.findViewById(R.id.checkBox);

			viewHolder.checkBox
					.setOnCheckedChangeListener(viewHolder.checkListener);

			view.setTag(viewHolder);
		} else {
			viewHolder = (DataSourceViewHolder) view.getTag();
		}

		viewHolder.groupPosition = groupPosition;
		viewHolder.dataSource = dataSource;

		viewHolder.textView.setText(viewHolder.dataSource.getName());
		viewHolder.checkBox.setChecked(viewHolder.dataSource.areAllChecked());

		return view;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return mDataSources.get(groupPosition).getInstances().size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mDataSources.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return mDataSources.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public int getGroupType(int groupPosition) {
		if (mDataSources.get(groupPosition).instanceable()) {
			return VIEW_TYPE_DATASOURCE;
		} else {
			return VIEW_TYPE_DATASOURCEINSTANCE;
		}
	}

	@Override
	public int getGroupTypeCount() {
		return 2; // Data Source & Instance
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View view,
			ViewGroup parent) {
		DataSourceHolder dataSource = mDataSources.get(groupPosition);

		if (dataSource.instanceable()) {
			return getDataSourceView(dataSource, groupPosition, view, parent);
		} else {
			return getDataSourceInstanceView(dataSource.getInstances().get(0),
					view, parent);
		}
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
		return mDataSources.isEmpty();
	}

}