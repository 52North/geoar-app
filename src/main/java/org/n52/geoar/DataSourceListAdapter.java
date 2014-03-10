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
package org.n52.geoar;

import java.util.List;

import org.n52.geoar.ar.view.IntroController;
import org.n52.geoar.newdata.CheckList.OnCheckedChangedListener;
import org.n52.geoar.newdata.CheckList.OnItemChangedListenerWrapper;
import org.n52.geoar.newdata.DataSourceHolder;
import org.n52.geoar.newdata.DataSourceInstanceHolder;
import org.n52.geoar.newdata.InstalledPluginHolder;
import org.n52.geoar.newdata.PluginLoader;
import org.n52.geoar.newdata.Visualization;

import android.app.AlertDialog;
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
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Adapter creating the {@link DataSourceHolder} /
 * {@link DataSourceInstanceHolder} tree list. Allows to manage settings and
 * instances.
 * 
 */
public class DataSourceListAdapter extends BaseExpandableListAdapter {

	/**
	 * Holder for child items 
	 * 
	 */
	private class DataSourceInstanceViewHolder {
		DataSourceInstanceHolder dataSourceInstance;
		ImageView imageViewSettings;
		TextView textView;
		TextView textViewDetails;
		CheckBox checkBox;
		OnCheckedChangeListener checkListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (dataSourceInstance.isChecked() != isChecked) {
					dataSourceInstance.setChecked(isChecked);
					notifyDataSetChanged();
					IntroController.finishTaskIfActive(9);
				}
			}
		};
		OnClickListener settingsClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dataSourceInstance.createSettingsDialog(mContext);
			}
		};
		OnClickListener titleClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (dataSourceInstance.hasErrorMessage()) {
					new AlertDialog.Builder(mContext).setTitle("Error")
							.setMessage(dataSourceInstance.getErrorMessage())
							.setNeutralButton(R.string.cancel, null).show();
				}
			}
		};
		public ViewGroup layoutTools;
	}

	/**
	 * Holder for group items
	 * 
	 */
	private class DataSourceViewHolder {
		int groupPosition;
		DataSourceHolder dataSource;
		ImageView imageViewAdd;
		TextView textView;
		CheckBox checkBox;
		ImageView imageViewGroup;
		LinearLayout layoutActions;
		OnCheckedChangeListener checkListener = new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (dataSource.areAllChecked() != isChecked) {
					dataSource.setChecked(isChecked);
					// notifyDataSetChanged();
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
		OnClickListener addClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				dataSource.addInstance(mContext);
				listView.expandGroup(groupPosition);
			}
		};
	}

	/**
	 * Holder for add instance items
	 * 
	 */
	private class AddDataSourceInstanceViewHolder {
		DataSourceHolder dataSource;
		TextView textView;

		OnClickListener clickListener = new OnClickListener() {
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
	private class RemoveUnselectedInstancesViewHolder {
		DataSourceHolder dataSource;
		TextView textView;

		OnClickListener clickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO dialog
				dataSource.removeUncheckedInstances();
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
			if (newState == false) {
				for (DataSourceHolder dataSource : item.getDataSources()) {
					dataSource.getInstances().removeOnItemChangeListener(
							dataSourceItemChangedListener);
					dataSource.getInstances().removeOnCheckedChangeListener(
							dataSourceCheckedChangedListener);
				}
			} else {
				for (DataSourceHolder dataSource : item.getDataSources()) {
					dataSource.getInstances().addOnItemChangeListener(
							dataSourceItemChangedListener);
					dataSource.getInstances().addOnCheckedChangeListener(
							dataSourceCheckedChangedListener);
				}
			}
		}
	};

	private int childPadding;

	private OnItemChangedListenerWrapper<DataSourceInstanceHolder> dataSourceItemChangedListener = new OnItemChangedListenerWrapper<DataSourceInstanceHolder>() {
		@Override
		public void onItemChanged() {
			notifyDataSetChanged();
		}
	};

	private OnCheckedChangedListener<DataSourceInstanceHolder> dataSourceCheckedChangedListener = new OnCheckedChangedListener<DataSourceInstanceHolder>() {
		@Override
		public void onCheckedChanged(DataSourceInstanceHolder item,
				boolean newState) {
			notifyDataSetChanged();
			IntroController.finishTaskIfActive(R.string.intro_task_3);
		}
	};

	private Class<? extends Visualization> visualizationClass;

	public <E extends Visualization> DataSourceListAdapter(Context context,
			ExpandableListView listView,
			Class<? extends Visualization> visualizationClass) {
		this.listView = listView;
		this.mContext = context;
		this.visualizationClass = visualizationClass;
		childPadding = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, 25, context.getResources()
						.getDisplayMetrics());
		mInflater = LayoutInflater.from(context);
		mDataSources = PluginLoader.getDataSources();
		PluginLoader.getInstalledPlugins().addOnCheckedChangeListener(
				mPluginChangedListener);
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.getInstances().addOnItemChangeListener(
					dataSourceItemChangedListener);
			dataSource.getInstances().addOnCheckedChangeListener(
					dataSourceCheckedChangedListener);
		}
		// TODO remove listener somehow
	}

	public void destroy() {
		PluginLoader.getInstalledPlugins().removeOnCheckedChangeListener(
				mPluginChangedListener);
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.getInstances().removeOnItemChangeListener(
					dataSourceItemChangedListener);
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
			return dataSource.getInstances().size() + 2; // Add & Remove
		} else {
			return 0;
		}
	}

	@Override
	public int getChildTypeCount() {
		return 3; // Instance & New Instance & Remove
	}

	@Override
	public int getChildType(int groupPosition, int childPosition) {
		if (childPosition >= mDataSources.get(groupPosition).getInstances()
				.size()) {
			DataSourceHolder dataSource = mDataSources.get(groupPosition);
			return (childPosition - dataSource.getInstances().size()) + 1;
		} else {
			return 0;
		}
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View view, ViewGroup parent) {
		View resultView;
		DataSourceHolder dataSource = mDataSources.get(groupPosition);
		if (childPosition >= dataSource.getInstances().size()) {
			int additionalPosition = childPosition
					- dataSource.getInstances().size();
			switch (additionalPosition) {
			case 0:
				resultView = getNewDataSourceInstanceView(dataSource, view,
						parent);
				break;
			case 1:
				resultView = getRemoveUnselectedDataSourceInstancesView(
						dataSource, view, parent);
				break;
			default:
				return null;
			}
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

		View groupView = dataSource.instanceable() ? getDataSourceView(dataSource,
				groupPosition, isExpanded, view, parent)
				: getDataSourceInstanceView(dataSource.getInstances().get(0),
						view, parent);

		if (dataSource.instanceable()) {
//			return getDataSourceView(dataSource, groupPosition, isExpanded,
//					view, parent);
		} else {
//			return getDataSourceInstanceView(dataSource.getInstances().get(0),
//					view, parent);
		}
		groupView.postInvalidateDelayed(50);
		return groupView;
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
			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textView.setOnClickListener(viewHolder.clickListener);

			view.setTag(viewHolder);
		} else {
			viewHolder = (AddDataSourceInstanceViewHolder) view.getTag();
		}

		viewHolder.dataSource = dataSource;
		return view;
	}

	private View getRemoveUnselectedDataSourceInstancesView(
			DataSourceHolder dataSource, View view, ViewGroup parent) {
		RemoveUnselectedInstancesViewHolder viewHolder;

		if (view == null) {
			view = mInflater
					.inflate(
							R.layout.datasource_list_removeunselecteddatasourceinstances_item,
							parent, false);
			viewHolder = new RemoveUnselectedInstancesViewHolder();

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textView.setOnClickListener(viewHolder.clickListener);

			view.setTag(viewHolder);
		} else {
			viewHolder = (RemoveUnselectedInstancesViewHolder) view.getTag();
		}

		viewHolder.textView.setEnabled(!dataSource.getInstances().allChecked());

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

			viewHolder.layoutTools = (ViewGroup) view
					.findViewById(R.id.layoutTools);

			viewHolder.imageViewSettings = (ImageView) view
					.findViewById(R.id.imageViewSettings);
			viewHolder.imageViewSettings
					.setOnClickListener(viewHolder.settingsClickListener);

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			viewHolder.textViewDetails = (TextView) view
					.findViewById(R.id.textViewDetails);

			view.findViewById(R.id.layoutTitle).setOnClickListener(
					viewHolder.titleClickListener);

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
		String error = dataSourceInstance.getErrorString();
		if (error != null) {
			viewHolder.textViewDetails.setVisibility(View.VISIBLE);
			viewHolder.textViewDetails.setText(error);
		} else {
			viewHolder.textViewDetails.setVisibility(View.GONE);
		}

		if (viewHolder.dataSourceInstance.getParent().getVisualizations()
				.ofType(visualizationClass).isEmpty()) {
			viewHolder.textView.setEnabled(false);
			viewHolder.checkBox.setEnabled(false);
		} else {
			viewHolder.textView.setEnabled(true);
			viewHolder.checkBox.setEnabled(true);
		}

		// Hide all settings
		boolean hasSettings = viewHolder.dataSourceInstance.hasSettings();
		viewHolder.layoutTools.setVisibility(hasSettings ? View.VISIBLE
				: View.GONE);

		return view;
	}

	private View getDataSourceView(DataSourceHolder dataSource,
			int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
		final DataSourceViewHolder viewHolder;

		if (view == null) {
			view = mInflater.inflate(R.layout.datasource_list_datasource_item,
					parent, false);
			viewHolder = new DataSourceViewHolder();
			view.setOnClickListener(viewHolder.clickListener);

			viewHolder.imageViewGroup = (ImageView) view
					.findViewById(R.id.imageViewGroup);

			viewHolder.layoutActions = (LinearLayout) view
					.findViewById(R.id.layoutActions);

			viewHolder.imageViewAdd = (ImageView) view
					.findViewById(R.id.imageViewAdd);
			viewHolder.imageViewAdd
					.setOnClickListener(viewHolder.addClickListener);

			viewHolder.textView = (TextView) view.findViewById(R.id.textView);

			// viewHolder.textView.setOnClickListener(viewHolder.clickListener);

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
		if (isExpanded) {
			viewHolder.imageViewGroup.getDrawable().setState(
					new int[] { android.R.attr.state_expanded });
		} else {
			viewHolder.imageViewGroup.getDrawable().setState(new int[] {});
		}

		if (viewHolder.dataSource.getInstances().isEmpty()) {
			// No Instances
			viewHolder.layoutActions.setVisibility(View.VISIBLE);
			viewHolder.imageViewGroup.setVisibility(View.GONE);
			viewHolder.checkBox.setVisibility(View.GONE);

			// if (dataSource.instanceable()) {
			// viewHolder.imageViewAdd.setVisibility(View.VISIBLE);
			// } else {
			// viewHolder.imageViewAdd.setVisibility(View.GONE);
			// }
		} else {
			viewHolder.checkBox.setChecked(viewHolder.dataSource
					.areAllChecked());
			viewHolder.layoutActions.setVisibility(View.GONE);
			viewHolder.imageViewGroup.setVisibility(View.VISIBLE);
			viewHolder.checkBox.setVisibility(View.VISIBLE);

			// viewHolder.imageViewAdd.setVisibility(View.GONE);
		}

//		IntroController.addViewToStep(9, view);

		if (viewHolder.dataSource.getVisualizations()
				.ofType(visualizationClass).isEmpty()) {
			viewHolder.textView.setEnabled(false);
			viewHolder.checkBox.setEnabled(false);
		} else {
			viewHolder.textView.setEnabled(true);
			viewHolder.checkBox.setEnabled(true);
		}

		return view;
	}
}