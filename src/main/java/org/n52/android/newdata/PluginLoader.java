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
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class PluginLoader {
	private static final FilenameFilter PLUGIN_FILENAME_FILTER = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String fileName) {
			return fileName.endsWith(".apk") || fileName.endsWith(".zip")
					|| fileName.endsWith(".jar");
		}
	};
	private static final String PLUGIN_SELECTION_PREF = "selected_plugins";
	private static final String DATASOURCE_SELECTION_PREF = "selected_datasources";
	private static final File PLUGIN_DIRECTORY_PATH = GeoARApplication.applicationContext
			.getExternalFilesDir(null);
	private static CheckList<InstalledPluginHolder> mInstalledPlugins = new CheckList<InstalledPluginHolder>(
			InstalledPluginHolder.class);
	private static CheckList<DataSourceHolder> mDataSources = new CheckList<DataSourceHolder>(
			DataSourceHolder.class);
	private static OnCheckedChangedListener<InstalledPluginHolder> pluginCheckedChangedListener = new OnCheckedChangedListener<InstalledPluginHolder>() {

		@Override
		public void onCheckedChanged(InstalledPluginHolder item,
				boolean newState) {
			for (DataSourceHolder dataSource : item.getDataSources()) {
				if (newState == true)
					addDataSource(dataSource);
				else
					removeDataSource(dataSource);
			}
		}
	};

	static {
		mInstalledPlugins
				.addOnCheckedChangeListener(pluginCheckedChangedListener);
		loadPlugins();
		restoreSelection();
	}

	private static void restoreSelection() {
		try {
			SharedPreferences preferences = GeoARApplication.applicationContext
					.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
							Context.MODE_PRIVATE);

			// Restore plugin selection state
			String[] identifierStateArray = preferences.getString(
					PLUGIN_SELECTION_PREF, "").split(";");
			for (String identifier : identifierStateArray) {
				InstalledPluginHolder plugin = getPluginByIdentifier(identifier);
				if (plugin != null) {
					plugin.setChecked(true);
				}
			}

			// Restore data source selection state
			identifierStateArray = preferences.getString(
					DATASOURCE_SELECTION_PREF, "").split(";");
			for (String identifier : identifierStateArray) {
				DataSourceHolder dataSource = getDataSourceByIdentifier(identifier);
				if (dataSource != null) {
					dataSource.setChecked(true);
				}
			}
		} catch (Exception e) {
			// TODO
		}
	}

	public static void saveSelection() {
		String dataSourceIdentifierPref = "";
		for (DataSourceHolder dataSource : mDataSources.getCheckedItems()) {
			if (dataSourceIdentifierPref.length() != 0)
				dataSourceIdentifierPref += ";";
			dataSourceIdentifierPref += dataSource.getIdentifier();
		}
		String pluginIdentifierPref = "";
		for (InstalledPluginHolder plugin : mInstalledPlugins.getCheckedItems()) {
			if (pluginIdentifierPref.length() != 0)
				pluginIdentifierPref += ";";
			pluginIdentifierPref += plugin.getIdentifier();
		}

		SharedPreferences preferences = GeoARApplication.applicationContext
				.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
						Context.MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putString(DATASOURCE_SELECTION_PREF, dataSourceIdentifierPref);
		editor.putString(PLUGIN_SELECTION_PREF, pluginIdentifierPref);
		editor.commit();
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

		String[] apksInDirectory = PLUGIN_DIRECTORY_PATH
				.list(PLUGIN_FILENAME_FILTER);

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

		for (Entry<String, PluginInfo> entry : pluginVersionMap.entrySet()) {
			InstalledPluginHolder pluginHolder = new InstalledPluginHolder(
					entry.getKey(), entry.getValue().version, new File(
							PLUGIN_DIRECTORY_PATH,
							entry.getValue().pluginFileName));
			mInstalledPlugins.add(pluginHolder);
		}

	}

	public static void reloadPlugins() {
		mInstalledPlugins.clear();
		loadPlugins();
	}

	public static CheckList<InstalledPluginHolder> getInstalledPlugins() {
		return mInstalledPlugins;
	}

	private static void addDataSource(DataSourceHolder dataSource) {
		if (!mDataSources.contains(dataSource))
			mDataSources.add(dataSource);
	}

	private static void removeDataSource(DataSourceHolder dataSource) {
		mDataSources.remove(dataSource);
	}

	public static CheckList<DataSourceHolder> getSelectedDataSources() {
		return mDataSources;
	}

	public static InstalledPluginHolder getPluginByIdentifier(String identifier) {
		for (InstalledPluginHolder plugin : mInstalledPlugins) {
			if (plugin.getIdentifier().equals(identifier)) {
				return plugin;
			}
		}
		return null;
	}

	private static DataSourceHolder getDataSourceByIdentifier(String identifier) {
		for (DataSourceHolder dataSource : mDataSources) {
			if (dataSource.getIdentifier().equals(identifier)) {
				return dataSource;
			}
		}
		return null;
	}
}
