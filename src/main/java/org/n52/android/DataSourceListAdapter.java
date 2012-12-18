package org.n52.android;

import java.util.List;

import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.CheckList.OnItemChangedListenerWrapper;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.DataSourceInstanceHolder;
import org.n52.android.newdata.InstalledPluginHolder;
import org.n52.android.newdata.PluginLoader;
import org.n52.android.newdata.Visualization;

import android.content.Context;
import android.util.TypedValue;
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

	/**
	 * Holder for group items
	 * 
	 */
	private class DataSourceViewHolder {
		int groupPosition;
		DataSourceHolder dataSource;
		ImageView imageViewSettings;
		ImageView imageViewAdd;
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
		OnClickListener addClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dataSource.addInstance(mContext);
			}
		};
	}

	/**
	 * Holder for add instance items
	 * 
	 */
	private class AddDataSourceInstanceViewHolder {
		DataSourceHolder dataSource;
		ImageView imageViewAdd;
		TextView textView;

		OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dataSource.addInstance(mContext);
			}
		};

	}

	private ExpandableListView listView;

	private Context mContext;
	private LayoutInflater mInflater;
	private List<DataSourceHolder> mDataSources;

	private OnCheckedChangedListener<InstalledPluginHolder> mPluginChangedListener = new OnCheckedChangedListener<InstalledPluginHolder>() {

		@Override
		public void onCheckedChanged(InstalledPluginHolder item,
				boolean newState) {
			mDataSources = PluginLoader.getDataSources();
			notifyDataSetInvalidated();
			updateListener();
		}
	};

	private int childPadding;

	private OnItemChangedListenerWrapper<DataSourceInstanceHolder> dataSourceChangedListener = new OnItemChangedListenerWrapper<DataSourceInstanceHolder>() {
		@Override
		public void onItemChanged() {
			notifyDataSetChanged();
		}
	};

	public <E extends Visualization> DataSourceListAdapter(Context context,
			ExpandableListView listView) {
		this.listView = listView;
		this.mContext = context;
		childPadding = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 25, context.getResources()
						.getDisplayMetrics());
		mInflater = LayoutInflater.from(context);
		mDataSources = PluginLoader.getDataSources();
		PluginLoader.getInstalledPlugins().addOnCheckedChangeListener(
				mPluginChangedListener);
		updateListener();
		// TODO remove listener somehow
	}

	private void updateListener() {
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.getInstances().addOnItemChangeListener(
					dataSourceChangedListener);
		}
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
	public int getChildrenCount(int groupPosition) {
		DataSourceHolder dataSource = mDataSources.get(groupPosition);
		if (dataSource.instanceable() && !dataSource.getInstances().isEmpty()) {
			return dataSource.getInstances().size() + 1;
		} else {
			return 0;
		}
	}

	@Override
	public int getChildTypeCount() {
		return 2; // Instance & New Instance
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		if (childPosition >= mDataSources.get(groupPosition).getInstances()
				.size()) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View view, ViewGroup parent) {
		View resultView;
		DataSourceHolder dataSource = mDataSources.get(groupPosition);
		if (childPosition >= dataSource.getInstances().size()) {
			resultView = getNewDataSourceInstanceView(dataSource, view, parent);
		} else {
			DataSourceInstanceHolder dataSourceInstance = dataSource
					.getInstances().get(childPosition);
			resultView = getDataSourceInstanceView(dataSourceInstance, view,
					parent);
		}
		resultView.setPadding(childPadding, 0, 0, 0);
		return resultView;
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
			return 0;
		} else {
			return 1;
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

	private View getNewDataSourceInstanceView(DataSourceHolder dataSource,
			View view, ViewGroup parent) {
		AddDataSourceInstanceViewHolder viewHolder;

		if (view == null) {
			view = mInflater.inflate(
					R.layout.datasource_list_adddatasourceinstance_item,
					parent, false);
			viewHolder = new AddDataSourceInstanceViewHolder();
			viewHolder.imageViewAdd = (ImageView) view
					.findViewById(R.id.imageViewAdd);
			viewHolder.imageViewAdd
					.setOnClickListener(viewHolder.clickListener);

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textView.setOnClickListener(viewHolder.clickListener);

			view.setTag(viewHolder);
		} else {
			viewHolder = (AddDataSourceInstanceViewHolder) view.getTag();
		}

		viewHolder.dataSource = dataSource;
		return view;
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

			viewHolder.imageViewAdd = (ImageView) view
					.findViewById(R.id.imageViewAdd);
			viewHolder.imageViewAdd
					.setOnClickListener(viewHolder.addClickListener);

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

		if (viewHolder.dataSource.getInstances().isEmpty()) {
			// No Instances
			viewHolder.checkBox.setVisibility(View.GONE);
			viewHolder.imageViewSettings.setVisibility(View.GONE);
			if (dataSource.instanceable()) {
				viewHolder.imageViewAdd.setVisibility(View.VISIBLE);
			} else {
				viewHolder.imageViewAdd.setVisibility(View.GONE);
			}
		} else {
			viewHolder.checkBox.setChecked(viewHolder.dataSource
					.areAllChecked());
			viewHolder.checkBox.setVisibility(View.VISIBLE);
			viewHolder.imageViewSettings.setVisibility(View.VISIBLE);
			viewHolder.imageViewAdd.setVisibility(View.GONE);
		}

		return view;
	}

}