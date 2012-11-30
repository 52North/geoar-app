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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	/**
	 * Parses a version string to long. Assumes that each part of a version
	 * string is < 100
	 * 
	 * @param version
	 *            Version string, e.g. "1.2.3"
	 * @return long built by multiplying each version component by 100 to the
	 *         power of its position from the back, i.e. "0.0.1" -> 1, "0.1.0"
	 *         -> 100
	 */
	private static long parseVersionNumber(String version) {
		String[] split = version.split("\\.");
		long versionNumber = 0;
		for (int i = 0; i < split.length; i++) {
			int num = Integer.parseInt(split[i]);
			if (num < 0 || num >= 100) {
				throw new NumberFormatException(
						"Unable to parse version number, each part may not exceed 100");
			}
			versionNumber += Math.pow(100, (split.length - 1) - i) * num;
		}
		return versionNumber;
	}

	/**
	 * Loads all plugins from SD card, compares version strings for plugins with
	 * same names and loads the most recent plugin.
	 * 
	 * The plugin name is the filename without its ending and without optional
	 * version string. A version string is introduced by a hyphen, following
	 * dot-separated numbers, e.g. "-1.2.3"
	 */
	@SuppressLint("NewApi")
	private static void loadPlugins() {

		/**
		 * Class to hold plugin version and filename information to find the
		 * newest
		 */
		class PluginInfo {
			public PluginInfo(String pluginFileName, long version) {
				this.pluginFileName = pluginFileName;
				this.version = version;
			}

			public String pluginFileName;
			public long version;
		}

		String[] apksInDirectory = new File(PLUGIN_PATH)
				.list(pluginFilenameFilter);

		if (apksInDirectory == null || apksInDirectory.length == 0)
			return;

		// Pattern captures the plugin version string
		Pattern pluginVersionPattern = Pattern
				.compile("-(\\d+(?:\\.\\d+)*)[.-]");

		// Pattern captures the plugin name, ignoring the optional version and
		// filename ending
		Pattern pluginNamePattern = Pattern
				.compile("^((?:.(?!-\\d+\\.))+.).*\\.[^.]+$");

		// Map to store all plugins with their versions for loading only the
		// newest
		HashMap<String, PluginInfo> pluginVersionMap = new HashMap<String, PluginInfo>();

		Matcher matcher;
		for (String pluginFileName : apksInDirectory) {
			matcher = pluginNamePattern.matcher(pluginFileName);
			if (!matcher.matches()) {
				Log.e("GeoAR", "Plugin filename invalid: " + pluginFileName);
				continue;
			}

			String pluginName = matcher.group(1);
			if (pluginName == null) {
				// invalid plugin filename
				Log.e("GeoAR", "Plugin filename invalid: " + pluginFileName);
				continue;
			}

			long version = 0;
			matcher = pluginVersionPattern.matcher(pluginFileName);
			if (matcher.find() && matcher.group(1) != null) {
				try {
					version = parseVersionNumber(matcher.group(1));
				} catch (NumberFormatException e) {
					Log.e("GeoAR", "Plugin filename version invalid: "
							+ matcher.group(1));
				}
			}
			PluginInfo pluginInfo = pluginVersionMap.get(pluginName);
			if (pluginInfo == null) {
				// Plugin not yet known
				pluginVersionMap.put(pluginName, new PluginInfo(pluginFileName,
						version));
			} else {
				// Plugin already found
				if (pluginInfo.version < version) {
					// current plugin has a newer version
					pluginInfo.version = version;
					pluginInfo.pluginFileName = pluginFileName;
				}
			}
		}

		try {
			for (PluginInfo pluginInfo : pluginVersionMap.values()) {
				loadPlugin(pluginInfo.pluginFileName);
			}
		} catch (IOException e) {
			// TODO Handle exceptions
			e.printStackTrace();
		}

	}

	/**
	 * Loads a single plugin using {@link DexClassLoader}. Every annotated and
	 * valid datasource will be added to
	 * {@link DataSourceLoader#mInstalledDataSources};
	 * 
	 * @param pluginFileName
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	private static void loadPlugin(String pluginFileName) throws IOException {
		String pluginBaseFileName = pluginFileName.substring(0,
				pluginFileName.lastIndexOf("."));
		String pluginPath = PLUGIN_PATH + pluginFileName;
		// Check if the Plugin exists
		if (!new File(pluginPath).exists())
			throw new FileNotFoundException("Directory not found: "
					+ pluginPath);

		File tmpDir = GeoARApplication.applicationContext.getDir(
				pluginBaseFileName, 0);

		Enumeration<String> entries = DexFile
				.loadDex(
						pluginPath,
						tmpDir.getAbsolutePath() + "/" + pluginBaseFileName
								+ ".dex", 0).entries();

		// Path for optimized dex equals path which will be used by
		// dexClassLoader
		// create separate ClassLoader for each plugin
		DexClassLoader dexClassLoader = new DexClassLoader(pluginPath,
				tmpDir.getAbsolutePath(), null,
				GeoARApplication.applicationContext.getClassLoader());

		try {
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
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Dex changed");
		}
	}
}
