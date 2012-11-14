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
package org.n52.android.newdata;

import java.util.List;

import org.n52.android.data.ImageLoader;
import org.n52.android.geoar.R;
import org.n52.android.newdata.DataSourceDownloader.DataSourceDownloadHolder;
import org.n52.android.newdata.DataSourceDownloader.OnDataSourceResultListener;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesChangeListener;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * 
 * 
 */
public class DataSourceFragment extends SherlockFragment {

	private GridView mGridViewInstalled;
	private GridView mGridViewDownload;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_cb, container, false);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mGridViewInstalled = (GridView) getView().findViewById(
				R.id.gridViewInstalled);
		mGridViewDownload = (GridView) getView().findViewById(
				R.id.gridViewDownload);

		final GridAdapterInstalled gridAdapterInstalled = new GridAdapterInstalled();
		mGridViewInstalled.setAdapter(gridAdapterInstalled);
		mGridViewInstalled.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getActivity() != null) {
					DataSourceHolder selectedDataSource = gridAdapterInstalled
							.getItem(position);

					DataSourceDialogFragment.newInstance(selectedDataSource)
							.show(getFragmentManager(), "DataSource");
				}
			}
		});

		final GridAdapterDownload gridAdapterDownload = new GridAdapterDownload();
		mGridViewDownload.setAdapter(gridAdapterDownload);

		TabHost tabHost = (TabHost) getView()
				.findViewById(android.R.id.tabhost);
		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("installed")
				.setIndicator("Installed").setContent(R.id.gridViewInstalled));
		tabHost.addTab(tabHost.newTabSpec("download").setIndicator("Download")
				.setContent(R.id.gridViewDownload));

		tabHost.setCurrentTab(0);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_datasources, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_reload:
			DataSourceLoader.reloadPlugins();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class GridAdapterInstalled extends BaseAdapter implements
			OnDataSourcesChangeListener {

		private class ViewHolder {
			public ImageView imageView;
			public TextView textView;
			public CheckBox checkBox;
		}

		private List<DataSourceHolder> dataSources;
		private LayoutInflater inflater;

		public GridAdapterInstalled() {
			dataSources = DataSourceLoader.getInstalledDataSources();
			inflater = (LayoutInflater) getActivity().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);

			DataSourceLoader.addOnInstalledDataSourcesUpdateListener(this);
			DataSourceLoader.addOnSelectedDataSourcesUpdateListener(this);
			// TODO remove..Listener?
		}

		@Override
		public int getCount() {
			if (dataSources != null)
				return dataSources.size();
			return 0;
		}

		@Override
		public DataSourceHolder getItem(int position) {
			if (dataSources != null && position < getCount() && position >= 0)
				return dataSources.get(position);
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

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
				viewHolder.checkBox = (CheckBox) view
						.findViewById(R.id.checkBox);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			final DataSourceHolder dataSource = dataSources.get(position);
			// load image via imageCache
			ImageLoader.getInstance().displayImage("", viewHolder.imageView); // TODO?
			viewHolder.textView.setText(dataSource.getName());
			viewHolder.checkBox.setChecked(dataSource.isSelected());

			viewHolder.checkBox
					.setOnCheckedChangeListener(new OnCheckedChangeListener() {

						@Override
						public void onCheckedChanged(CompoundButton buttonView,
								boolean isChecked) {
							if(isChecked) 
								dataSource.select();
							else 
								dataSource.unselect();
						}
					});
			return view;
		}

		@Override
		public void onDataSourcesChange() {
			notifyDataSetChanged();
		}
	}

	private class GridAdapterDownload extends BaseAdapter implements
			OnDataSourceResultListener {

		private class ViewHolder {
			public ImageView imageView;
			public TextView textView;
		}

		@Override
		public int getViewTypeCount() {
			return 2; // Normal and Progress
		}

		private List<DataSourceDownloadHolder> dataSources;
		private LayoutInflater inflater;

		public GridAdapterDownload() {
			inflater = (LayoutInflater) getActivity().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);

			DataSourceDownloader.getDataSources(this);
		}

		@Override
		public int getItemViewType(int position) {
			if (dataSources == null && position == 0) {
				return 1;
			} else {
				return 0;
			}
		}

		@Override
		public int getCount() {
			if (dataSources != null)
				return dataSources.size();
			return 1; // The loading view
		}

		@Override
		public DataSourceDownloadHolder getItem(int position) {
			if (dataSources != null && position < getCount() && position >= 0)
				return dataSources.get(position);
			return null;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (dataSources == null) {
				return new ProgressBar(getActivity());
			}

			ViewHolder viewHolder;

			if (view == null) {
				view = inflater.inflate(R.layout.cb_grid_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) view
						.findViewById(R.id.cb_grid_image);
				viewHolder.textView = (TextView) view
						.findViewById(R.id.cb_grid_label);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			DataSourceDownloadHolder dataSource = dataSources.get(position);
			// load image via imageCache
			ImageLoader.getInstance().displayImage("", viewHolder.imageView); // TODO
			viewHolder.textView.setText(dataSource.getName());

			return view;
		}

		@Override
		public void onDataSourceResult(
				List<DataSourceDownloadHolder> dataSources) {
			this.dataSources = dataSources;
			notifyDataSetChanged();
		}
	}
}
