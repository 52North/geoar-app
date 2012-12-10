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
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.n52.android.newdata.PluginDownloader.OnDataSourceResultListener;
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
public class PluginFragment extends SherlockFragment {

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

		final InstalledPluginsAdapter gridAdapterInstalled = new InstalledPluginsAdapter(
				getActivity());
		mGridViewInstalled.setAdapter(gridAdapterInstalled);
		mGridViewInstalled.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getActivity() != null) {
					InstalledPluginHolder plugin = gridAdapterInstalled
							.getItem(position);

					PluginDialogFragment.newInstance(plugin).show(
							getFragmentManager(), "Plugin");
				}
			}
		});
		gridAdapterInstalled
				.setOnItemCheckedListener(new OnItemCheckedListener() {

					@Override
					public void onItemChecked(boolean newState, int position) {
						InstalledPluginHolder plugin = gridAdapterInstalled
								.getItem(position);

						plugin.setChecked(newState);
					}
				});
		gridAdapterInstalled.setShowCheckBox(true);

		final DownloadPluginsAdapter gridAdapterDownload = new DownloadPluginsAdapter(
				getActivity());
		mGridViewDownload.setAdapter(gridAdapterDownload);
		mGridViewDownload.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (getActivity() != null) {
					PluginDownloadHolder plugin = gridAdapterDownload
							.getItem(position);

					PluginDialogFragment.newInstance(plugin).show(
							getFragmentManager(), "Plugin");
				}
			}
		});
		gridAdapterDownload.setShowCheckBox(false);

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
			PluginLoader.reloadPlugins();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class InstalledPluginsAdapter extends
			PluginGridAdapter<InstalledPluginHolder> {

		private OnCheckedChangedListener<InstalledPluginHolder> pluginChangedListener = new OnCheckedChangedListener<InstalledPluginHolder>() {

			@Override
			public void onCheckedChanged(InstalledPluginHolder item,
					boolean newState) {
				notifyDataSetInvalidated();
			}
		};

		public InstalledPluginsAdapter(Context context) {
			super(context);
			plugins = PluginLoader.getInstalledPlugins();
			PluginLoader.getInstalledPlugins().addOnCheckedChangeListener(
					pluginChangedListener);
			// TODO remove listener?
		}

		@Override
		protected boolean getItemChecked(int position) {
			return getItem(position).isChecked();
		}
	}

	private class DownloadPluginsAdapter extends
			PluginGridAdapter<PluginDownloadHolder> implements
			OnDataSourceResultListener {

		@Override
		public int getViewTypeCount() {
			return super.getViewTypeCount() + 1; // Normal + Progress
		}

		public DownloadPluginsAdapter(Context context) {
			super(context);
			PluginDownloader.getDataSources(this);
		}

		@Override
		public int getItemViewType(int position) {
			if (plugins == null && position == 0) {
				return super.getViewTypeCount() + 1;
			} else {
				return super.getItemViewType(position);
			}
		}

		@Override
		public int getCount() {
			if (plugins != null)
				return super.getCount();
			return 1; // The loading view
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (plugins == null) {
				return new ProgressBar(getActivity());
			}

			return super.getView(position, view, parent);
		}

		@Override
		public void onDataSourceResult(List<PluginDownloadHolder> dataSources) {
			this.plugins = dataSources;
			notifyDataSetInvalidated();
		}
	}
}
