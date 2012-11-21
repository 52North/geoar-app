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

import org.n52.android.R;
import org.n52.android.newdata.DataSourceDownloader.OnDataSourceResultListener;
import org.n52.android.newdata.DataSourceLoader.OnDataSourcesChangeListener;
import org.n52.android.newdata.PluginGridAdapter.OnItemCheckedListener;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TabHost;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment for managing and downloading data sources to use within the
 * application
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

		final DataSourceAdapter gridAdapterInstalled = new DataSourceAdapter(
				getActivity());
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
		gridAdapterInstalled
				.setOnItemCheckedListener(new OnItemCheckedListener() {

					@Override
					public void onItemChecked(boolean newState, int position) {
						DataSourceHolder dataSource = gridAdapterInstalled
								.getItem(position);
						if (dataSource.isSelected() != newState) {
							if (newState) {
								dataSource.select(true);
							} else {
								dataSource.unselect();
							}
						}
					}
				});
		gridAdapterInstalled.setShowCheckBox(true);

		final DataSourceDownloadAdapter gridAdapterDownload = new DataSourceDownloadAdapter(
				getActivity());
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

	private class DataSourceAdapter extends PluginGridAdapter<DataSourceHolder>
			implements OnDataSourcesChangeListener {

		public DataSourceAdapter(Context context) {
			super(context);
			dataSources = DataSourceLoader.getInstalledDataSources();
			DataSourceLoader.addOnInstalledDataSourcesUpdateListener(this);
			DataSourceLoader.addOnSelectedDataSourcesUpdateListener(this);
			// TODO remove..Listener?
		}

		@Override
		public void onDataSourcesChange() {
			notifyDataSetChanged();
		}

		@Override
		protected boolean getItemChecked(int position) {
			return getItem(position).isSelected();
		}
	}

	private class DataSourceDownloadAdapter extends
			PluginGridAdapter<DataSourceDownloadHolder> implements
			OnDataSourceResultListener {

		@Override
		public int getViewTypeCount() {
			return super.getViewTypeCount() + 1; // Normal + Progress
		}

		public DataSourceDownloadAdapter(Context context) {
			super(context);
			DataSourceDownloader.getDataSources(this);
		}

		@Override
		public int getItemViewType(int position) {
			if (dataSources == null && position == 0) {
				return super.getViewTypeCount() + 1;
			} else {
				return super.getItemViewType(position);
			}
		}

		@Override
		public int getCount() {
			if (dataSources != null)
				return super.getCount();
			return 1; // The loading view
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (dataSources == null) {
				return new ProgressBar(getActivity());
			}

			return super.getView(position, view, parent);
		}

		@Override
		public void onDataSourceResult(
				List<DataSourceDownloadHolder> dataSources) {
			this.dataSources = dataSources;
			notifyDataSetChanged();
		}
	}
}
