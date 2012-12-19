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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.CheckList.CheckManager;
import org.n52.android.newdata.PluginLoader.PluginInfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
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
	private Resources mPluginResources;
	private Context mPluginContext;
	private Bitmap pluginIcon;
	private String publisher;

	@CheckManager
	private CheckList<InstalledPluginHolder>.Checker mChecker;
	private DexClassLoader mPluginDexClassLoader;
	private boolean iconLoaded;

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
					if (org.n52.android.newdata.DataSource.class
							.isAssignableFrom(entryClass)) {
						// Class is a datasource

						@SuppressWarnings("unchecked")
						DataSourceHolder dataSourceHolder = new DataSourceHolder(
								(Class<? extends DataSource<? super Filter>>) entryClass,
								this);

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
		} catch (LinkageError e) {
			Log.e("GeoAR", "Data source " + getName() + " uses invalid class, "
					+ e.getMessage());
		}

		loaded = true;
	}

	/**
	 * Constructs a {@link Resources} instance by creating an
	 * {@link AssetManager} linked explicitly to this custom plugin using
	 * reflection to access the non-code assets of this plugin without relying
	 * on {@link Context#getResources()}, which is not available for manually
	 * loaded application packages
	 * 
	 * @return
	 */
	public Resources getPluginResources() {
		if (mPluginResources == null) {

			Constructor<AssetManager> constructor;
			try {
				// For unknown reasons, the AssetManager constructor is not
				// public
				constructor = AssetManager.class.getConstructor();
				constructor.setAccessible(true);
				AssetManager assetManager = constructor.newInstance();

				// For unknown reasons, the addAssetPath method is not public
				Method method = AssetManager.class.getMethod("addAssetPath",
						String.class);
				method.setAccessible(true);
				method.invoke(assetManager, getPluginFile().getAbsolutePath());

				// get display metrics
				WindowManager windowManager = (WindowManager) GeoARApplication.applicationContext
						.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics displayMetrics = new DisplayMetrics();
				windowManager.getDefaultDisplay().getMetrics(displayMetrics);

				// Create Resources object, no Configuration object required
				mPluginResources = new Resources(assetManager, displayMetrics,
						null);
			} catch (NoSuchMethodException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return mPluginResources;
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

			// Special ContextWrapper will return plugin resources instead of
			// system resources and it will intercept LayoutInflater service
			// calls to return a special inflater based on the plugin resources.
			// Usually, system services are cached in a static Map and do not
			// reflect different Context/Resources instances
			mPluginContext = new ContextWrapper(
					GeoARApplication.applicationContext) {

				private LayoutInflater mLayoutInflater;
				private Theme mTheme;

				@Override
				public Resources getResources() {
					return getPluginResources();
				}

				@Override
				public Object getSystemService(String name) {
					if (LAYOUT_INFLATER_SERVICE.equals(name)) {
						if (mLayoutInflater == null) {
							// Get and store a LayoutInflater for this special
							// plugin context and do not use the cached inflater
							mLayoutInflater = LayoutInflater.from(
									getBaseContext()).cloneInContext(this);
						}
						return mLayoutInflater;
					}
					return super.getSystemService(name);
				}

				@Override
				public String getPackageResourcePath() {
					return getPluginFile().getAbsolutePath();
				}

				@Override
				public ClassLoader getClassLoader() {
					return mPluginDexClassLoader;
				}

				@Override
				public Theme getTheme() {
					if (mTheme == null) {
						// Create special theme to include the custom Resources
						// instance for a plugin
						mTheme = getResources().newTheme();
						mTheme.setTo(getBaseContext().getTheme());
					}
					return mTheme;
				}

				@Override
				public String getPackageCodePath() {
					return getPluginFile().getAbsolutePath();
				}
				
			};
		}
		return mPluginContext;
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
					Log.i("GeoAR", "Plugin " + getName() + " has no icon");
				}
			} catch (IOException e) {
				Log.i("GeoAR", "Plugin " + getName() + " has an invalid icon");
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

	public void restoreState(ObjectInputStream objectInputStream) throws IOException {
		setChecked(objectInputStream.readBoolean());
		for (DataSourceHolder dataSource : mDataSources) {
			dataSource.restoreState(objectInputStream);
		}
	}

}
