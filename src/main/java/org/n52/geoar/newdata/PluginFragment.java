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
import org.n52.geoar.ar.view.IntroController;
import org.n52.geoar.newdata.CheckList.OnCheckedChangedListener;
import org.n52.geoar.newdata.CheckList.OnItemChangedListenerWrapper;
import org.n52.geoar.newdata.PluginDownloader.OnDataSourceResultListener;
import org.n52.geoar.newdata.PluginGridAdapter.OnItemCheckedListener;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Fragment for managing and downloading data sources to use within the
 * application
 * 
 */
public class PluginFragment extends SherlockFragment {

	private static final String CURRENT_TAB_KEY = "current_tab";
	private GridView mGridViewInstalled;
	private GridView mGridViewDownload;
	private DownloadPluginsAdapter gridAdapterDownload;
	private InstalledPluginsAdapter gridAdapterInstalled;
	private TabHost mTabHost;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(final LayoutInflater inflater,
			final ViewGroup container, final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_plugins, container, false);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mGridViewInstalled = (GridView) getView().findViewById(
				R.id.gridViewInstalled);
		mGridViewDownload = (GridView) getView().findViewById(
				R.id.gridViewDownload);

		gridAdapterInstalled = new InstalledPluginsAdapter(getActivity());
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
		mGridViewInstalled.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getActionMasked();
				if (getActivity() != null && action == MotionEvent.ACTION_UP) {
					float currentXPosition = event.getX();
					float currentYPosition = event.getY();
					int position = mGridViewDownload.pointToPosition(
							(int) currentXPosition, (int) currentYPosition);
					InstalledPluginHolder plugin = gridAdapterInstalled
							.getItem(position);
					if (plugin != null)
						PluginDialogFragment.newInstance(plugin).show(
								getFragmentManager(), "Plugin");
				}
				return false;
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
		gridAdapterDownload = new DownloadPluginsAdapter(getActivity());
		mGridViewDownload.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				// if (getActivity() != null) {
				// PluginDownloadHolder plugin = gridAdapterDownload
				// .getItem(position);
				//
				// PluginDialogFragment.newInstance(plugin).show(
				// getFragmentManager(), "Plugin");
				// }
			}
		});
		mGridViewDownload.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int action = event.getActionMasked();
				if (getActivity() != null && action == MotionEvent.ACTION_UP) {
					float currentXPosition = event.getX();
					float currentYPosition = event.getY();
					int position = mGridViewDownload.pointToPosition(
							(int) currentXPosition, (int) currentYPosition);
					PluginDownloadHolder plugin = gridAdapterDownload
							.getItem(position);
					if (plugin != null)
						PluginDialogFragment.newInstance(plugin).show(
								getFragmentManager(), "Plugin");
				}
				return false;
			}
		});
		gridAdapterDownload.setShowCheckBox(false);

		mTabHost = (TabHost) getView().findViewById(android.R.id.tabhost);
		mTabHost.setup();
		TabSpec download = mTabHost.newTabSpec("download")
				.setIndicator(getActivity().getString(R.string.download))
				.setContent(R.id.gridViewDownload);
		mTabHost.addTab(mTabHost.newTabSpec("installed")
				.setIndicator(getActivity().getString(R.string.installed))
				.setContent(R.id.gridViewInstalled));
		mTabHost.addTab(download);

		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				if (tabId.equals("download")
						&& mGridViewDownload.getAdapter() == null) {
					mGridViewDownload.setAdapter(gridAdapterDownload);
					IntroController.notify(R.string.intro_desc_1_4);
					IntroController.addViewToStep(7,
							((SherlockFragmentActivity) getActivity())
									.findViewById(R.id.item_map));
				}
				if (tabId.equals("installed")) {
					IntroController.notify(R.string.intro_desc_2_2);
				}
			}
		});
		if (savedInstanceState != null) {
			mTabHost.setCurrentTab(savedInstanceState
					.getInt(CURRENT_TAB_KEY, 0));
		} else {
			mTabHost.setCurrentTab(0);
		}

		addStepsToIntro();
		IntroController.notify(mTabHost.getTabWidget()
				.getChildTabViewAt(1));
	}

	private void addStepsToIntro() {
		IntroController.addViewToStep(3, mTabHost.getTabWidget()
				.getChildTabViewAt(1));
		IntroController.addViewToStep(4, null);
		IntroController.addViewToStep(5, mTabHost.getTabWidget()
				.getChildTabViewAt(0));
		IntroController.addViewToStep(6, null);
		View view = ((SherlockFragmentActivity) getActivity())
				.findViewById(R.id.item_map);
		if (view != null)
			IntroController.addViewToStep(7, view);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mTabHost != null) {
			outState.putInt(CURRENT_TAB_KEY, mTabHost.getCurrentTab());
		}
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
			if (mTabHost.getCurrentTabTag().equals("download")) {
				PluginDownloader.getDataSources(gridAdapterDownload, true);
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		gridAdapterInstalled.destroy();
		gridAdapterDownload.destroy();
	}

	private class InstalledPluginsAdapter extends
			PluginGridAdapter<InstalledPluginHolder> {

		private OnCheckedChangedListener<InstalledPluginHolder> pluginCheckedChangeListener = new OnCheckedChangedListener<InstalledPluginHolder>() {

			@Override
			public void onCheckedChanged(InstalledPluginHolder item,
					boolean newState) {
				notifyDataSetChanged();
			}
		};
		private OnItemChangedListenerWrapper<InstalledPluginHolder> pluginItemChangeListener = new OnItemChangedListenerWrapper<InstalledPluginHolder>() {

			@Override
			public void onItemChanged() {
				notifyDataSetInvalidated();
			}
		};

		public InstalledPluginsAdapter(Context context) {
			super(context);
			plugins = PluginLoader.getInstalledPlugins();
			PluginLoader.getInstalledPlugins().addOnCheckedChangeListener(
					pluginCheckedChangeListener);
			PluginLoader.getInstalledPlugins().addOnItemChangeListener(
					pluginItemChangeListener);
		}

		public void destroy() {
			PluginLoader.getInstalledPlugins().removeOnCheckedChangeListener(
					pluginCheckedChangeListener);
			PluginLoader.getInstalledPlugins().removeOnItemChangeListener(
					pluginItemChangeListener);
		}

		@Override
		protected boolean getItemChecked(int position) {
			return getItem(position).isChecked();
		}

		@Override
		protected String getPluginStatus(InstalledPluginHolder plugin) {
			return plugin.isChecked() ? getString(R.string.activated) : "";
		}
	}

	private class DownloadPluginsAdapter extends
			PluginGridAdapter<PluginDownloadHolder> implements
			OnDataSourceResultListener {

		private OnItemChangedListenerWrapper<InstalledPluginHolder> pluginItemChangeListener = new OnItemChangedListenerWrapper<InstalledPluginHolder>() {
			@Override
			public void onItemChanged() {
				notifyDataSetInvalidated();
			}
		};
		boolean initialized = false;

		public DownloadPluginsAdapter(Context context) {
			super(context);
			PluginLoader.getInstalledPlugins().addOnItemChangeListener(
					pluginItemChangeListener);
		}

		@Override
		public int getViewTypeCount() {
			return super.getViewTypeCount() + 1; // Normal + Progress
		}

		@Override
		public int getItemViewType(int position) {
			if (plugins == null && position == 0) {
				return super.getViewTypeCount();
			} else {
				return super.getItemViewType(position);
			}
		}

		@Override
		public int getCount() {
			if (!initialized) {
				PluginDownloader.getDataSources(this);
				initialized = true;
			}
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
			// IntroController.releaseNextStep();
			notifyDataSetInvalidated();
		}

		@Override
		protected String getPluginStatus(PluginDownloadHolder plugin) {
			InstalledPluginHolder installedPlugin = PluginLoader
					.getPluginByIdentifier(plugin.getIdentifier());
			if (installedPlugin != null) {
				if (installedPlugin.getVersion() != null
						&& plugin.getVersion() != null
						&& installedPlugin.getVersion() < plugin.getVersion()) {
					return getActivity().getString(R.string.update_available);
				} else {
					return getActivity().getString(R.string.installed);
				}
			} else {
				return "";
			}
		}

		public void destroy() {
			PluginLoader.getInstalledPlugins().removeOnItemChangeListener(
					pluginItemChangeListener);
		}
	}
}
