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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.OnCheckedChangedListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * 
 */
public class DataSourceLoader {

//	public interface OnDataSourcesChangeListener {
//		void onDataSourcesChange();
//	}
//
//
//
//	private static final String SELECTION_PREF = "selected_datasources";
//	
//	// private static CheckList<DataSourceHolder> mInstalledDataSources = new
//	// CheckList<DataSourceHolder>();
//	private static CheckList<DataSourceHolder> mAvailableDataSources = new CheckList<DataSourceHolder>(
//			DataSourceHolder.class);
//	private static Set<OnDataSourcesChangeListener> mSelectedDataSourcesChangeListeners = new HashSet<OnDataSourcesChangeListener>();
//	// private static Set<OnDataSourcesChangeListener>
//	// mInstalledDataSourcesChangeListeners = new
//	// HashSet<OnDataSourcesChangeListener>();
//
//
//	static {
//		loadPlugins();
//
//		// Ensure activating and deactivating of selected data sources
//		mAvailableDataSources
//				.addOnCheckedChangeListener(new OnCheckedChangedListener<DataSourceHolder>() {
//					@Override
//					public void onCheckedChanged(DataSourceHolder item,
//							boolean newState) {
//						if (newState == true) {
//							item.activate();
//						} else {
//							item.deactivate();
//						}
//					}
//				});
//
//		restoreDataSourceSelection();
//	}
//
//	public static void reloadPlugins() {
//		mInstalledPlugins.clear();
//		loadPlugins();
//
//		// Check if a selected data source got removed
//		DataSourceHolder dataSource;
//		Iterator<DataSourceHolder> it = mAvailableDataSources.iterator();
//		while (it.hasNext()) {
//			dataSource = it.next();
//			for (InstalledPluginHolder plugin : mInstalledPlugins)
//				if (!plugin.getDataSources().contains(dataSource)) {
//					dataSource.deactivate();
//					it.remove();
//				}
//		}
//		// notifyInstalledDataSourceListeners();
//		notifySelectedDataSourceListeners();
//	}
//
//	private static void restoreDataSourceSelection() {
//		try {
//			SharedPreferences preferences = GeoARApplication.applicationContext
//					.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
//							Context.MODE_PRIVATE);
//
//			String[] identifierStateArray = preferences.getString(
//					SELECTION_PREF, "").split(";");
//			for (String identifierState : identifierStateArray) {
//				String[] split = identifierState.split("~");
//				String identifier = split[0];
//				DataSourceHolder dataSource = getDataSourceByIdentifier(identifier);
//				if (dataSource != null) {
//					selectDataSource(dataSource);
//					mAvailableDataSources.checkItem(dataSource,
//							Boolean.parseBoolean(split[1]));
//				}
//			}
//		} catch (Exception e) {
//			// TODO
//		}
//	}
//
//	public static void saveDataSourceSelection() {
//		String identifierPref = "";
//		for (DataSourceHolder dataSource : mAvailableDataSources) {
//			if (identifierPref.length() != 0)
//				identifierPref += ";";
//			identifierPref += dataSource.getIdentifier() + "~"
//					+ mAvailableDataSources.isChecked(dataSource);
//		}
//
//		SharedPreferences preferences = GeoARApplication.applicationContext
//				.getSharedPreferences(GeoARApplication.PREFERENCES_FILE,
//						Context.MODE_PRIVATE);
//		Editor editor = preferences.edit();
//		editor.putString(SELECTION_PREF, identifierPref);
//		editor.commit();
//	}
//
//	private static DataSourceHolder getDataSourceByIdentifier(String identifier) {
//		for (InstalledPluginHolder plugin : mInstalledPlugins)
//			for (DataSourceHolder dataSource : plugin.getDataSources()) {
//				if (dataSource.getIdentifier().equals(identifier))
//					return dataSource;
//			}
//		return null;
//	}
//
//	public static void selectDataSource(DataSourceHolder dataSource) {
//		if (!mAvailableDataSources.contains(dataSource)) {
//			mAvailableDataSources.add(dataSource);
//			notifySelectedDataSourceListeners();
//		}
//	}
//
//	public static void unselectDataSource(DataSourceHolder dataSource) {
//		if (mAvailableDataSources.contains(dataSource)) {
//			dataSource.deactivate();
//			mAvailableDataSources.remove(dataSource);
//			notifySelectedDataSourceListeners();
//		}
//
//	}
//
//	private static void notifySelectedDataSourceListeners() {
//		for (OnDataSourcesChangeListener listener : mSelectedDataSourcesChangeListeners) {
//			listener.onDataSourcesChange();
//		}
//	}
//
//	// private static void notifyInstalledDataSourceListeners() {
//	// for (OnDataSourcesChangeListener listener :
//	// mInstalledDataSourcesChangeListeners) {
//	// listener.onDataSourcesChange();
//	// }
//	// }
//
//	public static CheckList<DataSourceHolder> getSelectedDataSources() {
//		return mAvailableDataSources;
//	}
//
//	// public static CheckList<DataSourceHolder> getInstalledDataSources() {
//	// return mInstalledDataSources;
//	// }
//
//	public static void addOnSelectedDataSourcesUpdateListener(
//			OnDataSourcesChangeListener listener) {
//		mSelectedDataSourcesChangeListeners.add(listener);
//	}
//
//	public static void removeOnSelectedDataSourcesUpdateListener(
//			OnDataSourcesChangeListener listener) {
//		mSelectedDataSourcesChangeListeners.remove(listener);
//	}
//
//	public static void addOnInstalledDataSourcesUpdateListener(
//			OnDataSourcesChangeListener listener) {
//		mSelectedDataSourcesChangeListeners.add(listener);
//	}
//
//	public static void removeOnInstalledDataSourcesUpdateListener(
//			OnDataSourcesChangeListener listener) {
//		mSelectedDataSourcesChangeListeners.remove(listener);
//	}
//
//	
//
//	public static List<InstalledPluginHolder> getInstalledPlugins() {
//		return mInstalledPlugins;
//	}
//
//	
//	public static void addDataSources(List<DataSourceHolder> mDataSources) {
//		mAvailableDataSources.addAll(mDataSources);
//	}
//
//	public static void removeDataSources(List<DataSourceHolder> mDataSources) {
//		mAvailableDataSources.removeAll(mDataSources);
//	}
}
