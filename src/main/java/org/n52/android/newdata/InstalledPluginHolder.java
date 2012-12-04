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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.CheckManager;
import org.n52.android.newdata.CheckList.Checker;
import org.n52.android.newdata.PluginLoader.PluginInfo;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

public class InstalledPluginHolder extends PluginHolder {

	private List<DataSourceHolder> mDataSources = new ArrayList<DataSourceHolder>();
	private File pluginFile;
	private Long version;
	private String identifier;
	private boolean loaded = false;
	private String description;
	private String name;

	@CheckManager
	private Checker mChecker;

	public InstalledPluginHolder(String identifier, Long version,
			File pluginFile) {
		this.identifier = this.name = identifier;
		this.version = version;
		this.pluginFile = pluginFile;
	}

	public InstalledPluginHolder(PluginInfo pluginInfo) {
		this.version = pluginInfo.version;
		this.identifier = pluginInfo.identifier;
		this.description = pluginInfo.description;
		this.name = pluginInfo.name;
		this.pluginFile = pluginInfo.pluginFile;
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	};

	List<DataSourceHolder> getDataSources() {
		if (!loaded) {
			try {
				loadPlugin();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return mDataSources;
	}

	public File getPluginFile() {
		return pluginFile;
	}

	public void setChecked(boolean state) {
		mChecker.setChecked(state);
	}

	public boolean isChecked() {
		return mChecker.isChecked();
	}

	@SuppressLint("NewApi")
	private void loadPlugin() throws IOException {
		mDataSources.clear();

		String pluginBaseFileName = getPluginFile().getName().substring(0,
				getPluginFile().getName().lastIndexOf("."));
		// Check if the Plugin exists
		if (!getPluginFile().exists())
			throw new FileNotFoundException("Not found: " + getPluginFile());

		File tmpDir = GeoARApplication.applicationContext.getDir(
				pluginBaseFileName, 0);

		Enumeration<String> entries = DexFile
				.loadDex(
						getPluginFile().getAbsolutePath(),
						tmpDir.getAbsolutePath() + "/" + pluginBaseFileName
								+ ".dex", 0).entries();

		// Path for optimized dex equals path which will be used by
		// dexClassLoader
		// create separate ClassLoader for each plugin
		DexClassLoader dexClassLoader = new DexClassLoader(getPluginFile()
				.getAbsolutePath(), tmpDir.getAbsolutePath(), null,
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

						mDataSources.add(dataSourceHolder);
					} else {
						Log.i("GeoAR",
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

		loaded = true;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getClass().getName());
		super.writeToParcel(dest, flags);
	}

}
