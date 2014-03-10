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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.n52.geoar.newdata.Annotations;
import org.n52.geoar.newdata.DataSource;
import org.n52.geoar.newdata.Filter;
import org.n52.geoar.GeoARApplication;
import org.n52.geoar.newdata.CheckList.CheckManager;
import org.n52.geoar.newdata.PluginLoader.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
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
	private Context mPluginContext;
	private Bitmap pluginIcon;
	private String publisher;

	@CheckManager
	private CheckList<InstalledPluginHolder>.Checker mChecker;
	private DexClassLoader mPluginDexClassLoader;
	private boolean iconLoaded;

	private static final Logger LOG = LoggerFactory
			.getLogger(InstalledPluginHolder.class);

	public InstalledPluginHolder(PluginInfo pluginInfo) {
		this.version = pluginInfo.version;
		this.identifier = pluginInfo.identifier;
		this.description = pluginInfo.description;
		this.name = pluginInfo.name;
		this.pluginFile = pluginInfo.pluginFile;
		this.publisher = pluginInfo.publisher;
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
	public String getPublisher() {
		return publisher;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	};

	/**
	 * Changes to the returned list do not affect loaded data sources
	 * 
	 * @return
	 */
	public List<DataSourceHolder> getDataSources() {
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
		mPluginDexClassLoader = new DexClassLoader(getPluginFile()
				.getAbsolutePath(), tmpDir.getAbsolutePath(), null,
				GeoARApplication.applicationContext.getClassLoader());

		try {
			while (entries.hasMoreElements()) {
				// Check each classname for annotations
				String entry = entries.nextElement();

				Class<?> entryClass = mPluginDexClassLoader.loadClass(entry);
				if (entryClass
						.isAnnotationPresent(Annotations.DataSource.class)) {
					// Class is a annotated as datasource
					if (org.n52.geoar.newdata.DataSource.class
							.isAssignableFrom(entryClass)) {
						// Class is a datasource

						@SuppressWarnings("unchecked")
						DataSourceHolder dataSourceHolder = new DataSourceHolder(
								(Class<? extends DataSource<? super Filter>>) entryClass,
								this);

						mDataSources.add(dataSourceHolder);
					} else {
						LOG.error("Datasource " + entryClass.getSimpleName()
								+ " is not implementing DataSource interface");
						// TODO handle error, somehow propagate back to user
					}
				}
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Dex changed");
		} catch (LinkageError e) {
			LOG.error("Data source " + getName() + " uses invalid class, "
					+ e.getMessage());
		}

		loaded = true;
	}

	/**
	 * Returns a {@link Context} wrapping the
	 * {@link GeoARApplication#applicationContext}, but returning the value of
	 * {@link InstalledPluginHolder#getPluginResources()} as
	 * {@link Context#getResources()} return value.
	 * 
	 * @return
	 */
	public Context getPluginContext() {
		if (mPluginContext == null) {
			mPluginContext = new PluginContext(
					GeoARApplication.applicationContext, this);
		}
		return mPluginContext;
	}

	public ClassLoader getPluginClassLoader() {
		return mPluginDexClassLoader;
	}

	@Override
	public Bitmap getPluginIcon() {
		if (!iconLoaded) {
			try {
				iconLoaded = true;
				ZipFile zipFile = new ZipFile(pluginFile);
				ZipEntry pluginIconEntry = zipFile.getEntry("icon.png");
				if (pluginIconEntry != null) {
					pluginIcon = BitmapFactory.decodeStream(zipFile
							.getInputStream(pluginIconEntry));
				} else {
					LOG.info("Plugin " + getName() + " has no icon");
				}
			} catch (IOException e) {
				LOG.error("Plugin " + getName() + " has an invalid icon");
			}
		}

		return pluginIcon;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getClass().getName());
		super.writeToParcel(dest, flags);
	}

	public void saveState(ObjectOutputStream objectOutputStream)
			throws IOException {
		objectOutputStream.writeBoolean(isChecked());
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.saveState(objectOutputStream);
		}
	}

	public void restoreState(PluginStateInputStream objectInputStream)
			throws IOException {
		setChecked(objectInputStream.readBoolean());
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.restoreState(objectInputStream);
		}
	}

	/**
	 * Should be called after all state initialization took place, e.g. after
	 * {@link InstalledPluginHolder#restoreState(PluginStateInputStream)}
	 */
	public void postConstruct() {
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.postConstruct();
		}
	}

}
