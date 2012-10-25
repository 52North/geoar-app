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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.n52.android.GeoARApplication;
import org.n52.android.newdata.Annotations.DataSource;
import org.n52.android.newdata.Annotations.SupportedVisualization;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;

/**
 * 
 */
public class DataSourceLoader {

	public static class DataSourceHolder {
		private static Map<Class<? extends Visualization>, Visualization> visualizationMap = new HashMap<Class<? extends Visualization>, Visualization>();

		private org.n52.android.newdata.DataSource dataSource;
		private List<Visualization> visualizations = new ArrayList<Visualization>();
		private String name;
		private long minReloadInterval;
		private byte preferredZoomLevel;

		public DataSourceHolder(
				Class<? extends org.n52.android.newdata.DataSource> entryClass) {

			DataSource dataSourceAnnotation = entryClass
					.getAnnotation(Annotations.DataSource.class);
			if (dataSourceAnnotation == null) {
				throw new RuntimeException("Class not annotated as data source");
			}

			name = dataSourceAnnotation.name();
			minReloadInterval = dataSourceAnnotation.minReloadInterval();
			preferredZoomLevel = dataSourceAnnotation.preferredZoomLevel();
			try {
				dataSource = entryClass.newInstance();
			} catch (InstantiationException e) {
				throw new RuntimeException(
						"No default constructor for datasource");
			} catch (IllegalAccessException e) {
				throw new RuntimeException(
						"No valid constructor for datasource");
			}

			SupportedVisualization supportedVisualization = entryClass
					.getAnnotation(SupportedVisualization.class);
			if (supportedVisualization != null) {
				Class<? extends Visualization>[] visualizationClasses = supportedVisualization
						.visualizationClasses();

				try {
					for (int i = 0; i < visualizationClasses.length; i++) {
						// Find cached instance or create new one
						Visualization v = visualizationMap
								.get(visualizationClasses[i]);
						if (v == null) {
							// New instance needed
							v = visualizationClasses[i].newInstance();
							visualizationMap.put(visualizationClasses[i], v);
						}

						visualizations.add(v);
					}
				} catch (InstantiationException e) {
					throw new RuntimeException(
							"Referenced visualization has no appropriate constructor");
				} catch (IllegalAccessException e) {
					throw new RuntimeException(
							"Referenced visualization has no appropriate constructor");
				}
			}
		}

		public org.n52.android.newdata.DataSource getDataSource() {
			return dataSource;
		}

		public long getMinReloadInterval() {
			return minReloadInterval;
		}

		public String getName() {
			return name;
		}

		public byte getPreferredZoomLevel() {
			return preferredZoomLevel;
		}

		public List<Visualization> getVisualizations() {
			return visualizations;
		}
	};

	private FilenameFilter pluginFilenameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String fileName) {
			return fileName.endsWith(".apk") || fileName.endsWith(".zip")
					|| fileName.endsWith(".jar");
		}
	};

	private static final String PLUGIN_PATH = Environment
			.getExternalStorageDirectory() + "/GeoAR/";
	private Context context;
	private List<DataSourceHolder> dataSources = new ArrayList<DataSourceHolder>();
	private static DataSourceLoader instance;

	public static DataSourceLoader getInstance() {
		if (instance == null) {
			instance = new DataSourceLoader(GeoARApplication.applicationContext);
		}

		return instance;
	}

	private DataSourceLoader(Context context) {
		this.context = context;
		loadPlugins();
	}

	private void loadPlugins() {

		String[] apksInDirectory = new File(PLUGIN_PATH)
				.list(pluginFilenameFilter);

		if (apksInDirectory.length == 0)
			throw new RuntimeException(
					"No Datasource APKs Found in GeoAR directory");

		try {
			for (String pluginFileName : apksInDirectory) {
				String pluginBaseFileName = pluginFileName.substring(0,
						pluginFileName.lastIndexOf("."));
				String pluginPath = PLUGIN_PATH + pluginFileName;
				// Check if the Plugin exists
				if (!new File(pluginPath).exists())
					throw new FileNotFoundException("Directory not found: "
							+ pluginPath);

				File tmpDir = context.getDir(pluginBaseFileName, 0);

				Enumeration<String> entries = DexFile.loadDex(
						pluginPath,
						tmpDir.getAbsolutePath() + "/" + pluginBaseFileName
								+ ".dex", 0).entries();
				// Path for optimized dex equals path which will be used by
				// dexClassLoader

				// create separate ClassLoader for each plugin
				DexClassLoader dexClassLoader = new DexClassLoader(pluginPath,
						tmpDir.getAbsolutePath(), null, this.getClass()
								.getClassLoader());

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
									(Class<? extends org.n52.android.newdata.DataSource>) entryClass);
							dataSources.add(dataSourceHolder);
						} else {
							Log.e("GeoAR",
									"Datasource "
											+ entryClass.getSimpleName()
											+ " is not implementing DataSource interface");
							// handle error
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

	public List<DataSourceHolder> getDataSources() {
		return dataSources;
	}

}
