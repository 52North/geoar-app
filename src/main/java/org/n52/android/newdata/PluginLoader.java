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
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class PluginLoader {

	public static class PluginInfo {
		public PluginInfo(File pluginFile, String name, String description,
				Long version, String identifier, String publisher) {
			this.name = name;
			this.description = description;
			this.version = version;
			this.pluginFile = pluginFile;
			this.identifier = identifier;
			this.publisher = publisher;
		}

		File pluginFile;
		String name;
		String description;
		Long version;
		String identifier;
		public String publisher;
	}

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
	// Pattern captures the plugin version string
	private static final Pattern pluginVersionPattern = Pattern
			.compile("-(\\d+(?:\\.\\d+)*)[.-]");
	// Pattern captures the plugin name, ignoring the optional version and
	// filename ending
	private static final Pattern pluginNamePattern = Pattern
			.compile("^((?:.(?!-\\d+\\.))+.).*\\.[^.]+$");
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
	private static DefaultHttpClient mHttpClient;

	static {
		mInstalledPlugins
				.addOnCheckedChangeListener(pluginCheckedChangedListener);
		loadPlugins();
		restoreSelection();
	}

	public static DefaultHttpClient getSharedHttpClient() {
		if (mHttpClient == null) {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			registry.register(new Scheme("https", SSLSocketFactory
					.getSocketFactory(), 443));

			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setSoTimeout(httpParameters, 10000);
			HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
			ClientConnectionManager cm = new ThreadSafeClientConnManager(
					httpParameters, registry);
			mHttpClient = new DefaultHttpClient(cm, httpParameters);
		}

		return mHttpClient;
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
	 * Extracts and parses the geoar-plugin.xml plugin-descriptor to create and
	 * fill a {@link PluginInfo} instance.
	 * 
	 * @param pluginFile
	 * @return
	 */
	private static PluginInfo readPluginInfoFromPlugin(File pluginFile) {
		try {
			ZipFile zipFile = new ZipFile(pluginFile);
			ZipEntry pluginDescriptorEntry = zipFile
					.getEntry("geoar-plugin.xml");
			if (pluginDescriptorEntry == null) {
				return null;
			}

			Document document = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder()
					.parse(zipFile.getInputStream(pluginDescriptorEntry));
			// Find name
			String name = null;
			NodeList nodeList = document.getElementsByTagName("name");
			if (nodeList != null && nodeList.getLength() >= 1) {
				name = nodeList.item(0).getTextContent();
			} else {
				Log.w("GeoAR", "Plugin Descriptor for " + pluginFile.getName()
						+ " does not specify a name");
			}

			// Find publisher
			String publisher = null;
			nodeList = document.getElementsByTagName("publisher");
			if (nodeList != null && nodeList.getLength() >= 1) {
				publisher = nodeList.item(0).getTextContent();
			} else {
				Log.w("GeoAR", "Plugin Descriptor for " + pluginFile.getName()
						+ " does not specify a publisher");
			}

			// Find description
			String description = null;
			nodeList = document.getElementsByTagName("description");
			if (nodeList != null && nodeList.getLength() >= 1) {
				description = nodeList.item(0).getTextContent();
			} else {
				Log.w("GeoAR", "Plugin Descriptor for " + pluginFile.getName()
						+ " does not specify a description");
			}

			// Find identifier
			String identifier = null;
			nodeList = document.getElementsByTagName("identifier");
			if (nodeList != null && nodeList.getLength() >= 1) {
				identifier = nodeList.item(0).getTextContent();
			} else {
				Log.w("GeoAR", "Plugin Descriptor for " + pluginFile.getName()
						+ " does not specify an identifier");
			}

			// Find version
			Long version = null;
			nodeList = document.getElementsByTagName("version");
			if (nodeList != null && nodeList.getLength() >= 1) {
				String versionString = "-" + nodeList.item(0).getTextContent();

				Matcher matcher = pluginVersionPattern.matcher(versionString);
				if (matcher.find() && matcher.group(1) != null) {
					try {
						version = parseVersionNumber(matcher.group(1));
					} catch (NumberFormatException e) {
						Log.e("GeoAR", "Plugin filename version invalid: "
								+ matcher.group(1));
					}
				}
			} else {
				Log.w("GeoAR", "Plugin Descriptor for " + pluginFile.getName()
						+ " does not specify a version");
			}

			if (identifier == null) {
				identifier = name;
			}

			return new PluginInfo(pluginFile, name, description, version,
					identifier, publisher);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Reads {@link PluginInfo} from the path of a plugin. This will only
	 * extract a name and version information
	 * 
	 * @param pluginFile
	 * @return
	 */
	private static PluginInfo readPluginInfoFromFilename(File pluginFile) {
		String pluginFileName = pluginFile.getName();

		Matcher matcher = pluginNamePattern.matcher(pluginFileName);
		if (!matcher.matches()) {
			Log.e("GeoAR", "Plugin filename invalid: " + pluginFileName);
			return null;
		}

		String name = matcher.group(1);

		Long version = null;
		matcher = pluginVersionPattern.matcher(pluginFileName);
		if (matcher.find() && matcher.group(1) != null) {
			try {
				version = parseVersionNumber(matcher.group(1));
			} catch (NumberFormatException e) {
				Log.e("GeoAR",
						"Plugin filename version invalid: " + matcher.group(1));
			}
		}

		return new PluginInfo(pluginFile, name, null, version, name, null);
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

		String[] apksInDirectory = PLUGIN_DIRECTORY_PATH
				.list(PLUGIN_FILENAME_FILTER);

		if (apksInDirectory == null || apksInDirectory.length == 0)
			return;

		// Map to store all plugins with their versions for loading only the
		// newest
		HashMap<String, PluginInfo> pluginVersionMap = new HashMap<String, PluginInfo>();

		for (String pluginFileName : apksInDirectory) {

			PluginInfo pluginInfo = readPluginInfoFromPlugin(new File(
					PLUGIN_DIRECTORY_PATH, pluginFileName));
			if (pluginInfo == null) {
				Log.i("GeoAR", "Plugin " + pluginFileName
						+ " has no plugin descriptor");
				pluginInfo = readPluginInfoFromFilename(new File(
						PLUGIN_DIRECTORY_PATH, pluginFileName));
			}

			if (pluginInfo.identifier == null) {
				Log.e("GeoAR", "Plugin " + pluginFileName
						+ " has an invalid plugin descriptor");
				continue;
			}
			if (pluginInfo.version == null) {
				pluginInfo.version = -1L; // Set unknown version to version -1
			}

			PluginInfo pluginInfoMapping = pluginVersionMap
					.get(pluginInfo.identifier);
			if (pluginInfoMapping == null
					|| pluginInfoMapping.version < pluginInfo.version) {
				// Plugin not yet known or newer
				pluginVersionMap.put(pluginInfo.identifier, pluginInfo);
			}
		}

		for (PluginInfo pluginInfo : pluginVersionMap.values()) {
			InstalledPluginHolder pluginHolder = new InstalledPluginHolder(
					pluginInfo);
			mInstalledPlugins.add(pluginHolder);
		}

	}

	public static void reloadPlugins() {

		// TODO selection state
		mInstalledPlugins.clear();
		mDataSources.clear();
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
