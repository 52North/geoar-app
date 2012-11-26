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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * 
 */
public class DataSourceLoader {

	public interface OnDataSourcesChangeListener {
		void onDataSourcesChange();
	}

	private static FilenameFilter pluginFilenameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String fileName) {
			return fileName.endsWith(".apk") || fileName.endsWith(".zip")
					|| fileName.endsWith(".jar");
		}
	};

	private static final String SELECTION_PREF = "selected_datasources";
	private static final String PLUGIN_PATH = Environment
			.getExternalStorageDirectory() + "/GeoAR/";
	private static CheckList<DataSourceHolder> mInstalledDataSources = new CheckList<DataSourceHolder>();
	private static CheckList<DataSourceHolder> mSelectedDataSources = new CheckList<DataSourceHolder>();
	private static Set<OnDataSourcesChangeListener> mSelectedDataSourcesChangeListeners = new HashSet<OnDataSourcesChangeListener>();
	private static Set<OnDataSourcesChangeListener> mInstalledDataSourcesChangeListeners = new HashSet<OnDataSourcesChangeListener>();

	static {
		loadPlugins();

		// Ensure activating and deactivating of selected data sources
		mSelectedDataSources
				.addOnCheckedChangeListener(new OnCheckedChangedListener<DataSourceHolder>() {
					@Override
					public void onCheckedChanged(DataSourceHolder item,
							boolean newState) {
						if (newState == true) {
							item.activate();
						} else {
							item.deactivate();
						}
					}
				});

		restoreDataSourceSelection();
	}

	public static void reloadPlugins() {
		mInstalledDataSources.clear();
		loadPlugins();

		// Check if a selected data source got removed
		DataSourceHolder dataSource;
		Iterator<DataSourceHolder> it = mSelectedDataSources.iterator();
		while (it.hasNext()) {
			dataSource = it.next();
			if (!mInstalledDataSources.contains(dataSource)) {
				dataSource.deactivate();
				it.remove();
			}
		}
		notifyInstalledDataSourceListeners();
		notifySelectedDataSourceListeners();
	}

	private static void restoreDataSourceSelection() {
		try {
			SharedPreferences preferences = GeoARApplication.applicationContext
					.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
							Context.MODE_PRIVATE);

			String[] identifierStateArray = preferences.getString(
					SELECTION_PREF, "").split(";");
			for (String identifierState : identifierStateArray) {
				String[] split = identifierState.split("~");
				String identifier = split[0];
				DataSourceHolder dataSource = getDataSourceByIdentifier(identifier);
				if (dataSource != null) {
					selectDataSource(dataSource);
					mSelectedDataSources.checkItem(dataSource,
							Boolean.parseBoolean(split[1]));
				}
			}
		} catch (Exception e) {
			// TODO
		}
	}

	public static void saveDataSourceSelection() {
		String identifierPref = "";
		for (DataSourceHolder dataSource : mSelectedDataSources) {
			if (identifierPref.length() != 0)
				identifierPref += ";";
			identifierPref += dataSource.getIdentifier() + "~"
					+ mSelectedDataSources.isChecked(dataSource);
		}

		SharedPreferences preferences = GeoARApplication.applicationContext
				.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
						Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(SELECTION_PREF, identifierPref);
		editor.commit();
	}

	private static DataSourceHolder getDataSourceByIdentifier(String identifier) {
		for (DataSourceHolder dataSource : mInstalledDataSources) {
			if (dataSource.getIdentifier().equals(identifier))
				return dataSource;
		}
		return null;
	}

	public static void selectDataSource(DataSourceHolder dataSource) {
		if (!mSelectedDataSources.contains(dataSource)) {
			mSelectedDataSources.add(dataSource);
			notifySelectedDataSourceListeners();
		}
	}

	public static void unselectDataSource(DataSourceHolder dataSource) {
		if (mSelectedDataSources.contains(dataSource)) {
			dataSource.deactivate();
			mSelectedDataSources.remove(dataSource);
			notifySelectedDataSourceListeners();
		}

	}

	private static void notifySelectedDataSourceListeners() {
		for (OnDataSourcesChangeListener listener : mSelectedDataSourcesChangeListeners) {
			listener.onDataSourcesChange();
		}
	}

	private static void notifyInstalledDataSourceListeners() {
		for (OnDataSourcesChangeListener listener : mInstalledDataSourcesChangeListeners) {
			listener.onDataSourcesChange();
		}
	}

	public static CheckList<DataSourceHolder> getSelectedDataSources() {
		return mSelectedDataSources;
	}

	public static CheckList<DataSourceHolder> getInstalledDataSources() {
		return mInstalledDataSources;
	}

	public static void addOnSelectedDataSourcesUpdateListener(
			OnDataSourcesChangeListener listener) {
		mSelectedDataSourcesChangeListeners.add(listener);
	}

	public static void removeOnSelectedDataSourcesUpdateListener(
			OnDataSourcesChangeListener listener) {
		mSelectedDataSourcesChangeListeners.remove(listener);
	}

	public static void addOnInstalledDataSourcesUpdateListener(
			OnDataSourcesChangeListener listener) {
		mSelectedDataSourcesChangeListeners.add(listener);
	}

	public static void removeOnInstalledDataSourcesUpdateListener(
			OnDataSourcesChangeListener listener) {
		mSelectedDataSourcesChangeListeners.remove(listener);
	}

	@SuppressLint("NewApi")
	private static void loadPlugins() {

		String[] apksInDirectory = new File(PLUGIN_PATH)
				.list(pluginFilenameFilter);

		if (apksInDirectory == null || apksInDirectory.length == 0)
			return;

		try {
			for (String pluginFileName : apksInDirectory) {
				String pluginBaseFileName = pluginFileName.substring(0,
						pluginFileName.lastIndexOf("."));
				String pluginPath = PLUGIN_PATH + pluginFileName;
				// Check if the Plugin exists
				if (!new File(pluginPath).exists())
					throw new FileNotFoundException("Directory not found: "
							+ pluginPath);

				File tmpDir = GeoARApplication.applicationContext.getDir(
						pluginBaseFileName, 0);

				Enumeration<String> entries = DexFile.loadDex(
						pluginPath,
						tmpDir.getAbsolutePath() + "/" + pluginBaseFileName
								+ ".dex", 0).entries();
				// Path for optimized dex equals path which will be used by
				// dexClassLoader

				// create separate ClassLoader for each plugin
				DexClassLoader dexClassLoader = new DexClassLoader(pluginPath,
						tmpDir.getAbsolutePath(), null,
						GeoARApplication.applicationContext.getClassLoader());

				while (entries.hasMoreElements()) {
					// Check each classname for annotations
					String entry = entries.nextElement();

					Class<?> entryClass = dexClassLoader.loadClass(entry);
					if (entryClass
							.isAnnotationPresent(Annotations.DataSource.class)) {
						// Class is a annotated as datasource
						if (org.n52.android.newdata.DataSource.class
								.isAssignableFrom(entryClass)) {
							// Class is a datasource
							@SuppressWarnings("unchecked")
							DataSourceHolder dataSourceHolder = new DataSourceHolder(
									(Class<? extends DataSource<? super Filter>>) entryClass);
							mInstalledDataSources.add(dataSourceHolder);
						} else {
							Log.e("GeoAR",
									"Datasource "
											+ entryClass.getSimpleName()
											+ " is not implementing DataSource interface");
							// TODO handle error
						}
					}
				}

			}
			// TODO Handle exceptions
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

	}
}
