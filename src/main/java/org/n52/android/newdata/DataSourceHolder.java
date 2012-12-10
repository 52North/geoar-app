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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.n52.android.newdata.Annotations.PostConstruct;
import org.n52.android.newdata.Annotations.SharedHttpClient;
import org.n52.android.newdata.Annotations.SupportedVisualization;
import org.n52.android.newdata.Annotations.SystemService;
import org.n52.android.newdata.CheckList.CheckManager;
import org.n52.android.newdata.CheckList.CheckedChangedListener;
import org.n52.android.newdata.filter.FilterDialogActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class DataSourceHolder implements Parcelable {
	private static Map<Class<? extends Visualization>, Visualization> visualizationMap = new HashMap<Class<? extends Visualization>, Visualization>();
	private static int nextId = 0;
	private DataSource<? super Filter> dataSource;
	private CheckList<Visualization> visualizations = new CheckList<Visualization>();

	private long minReloadInterval;
	private byte cacheZoomLevel;
	private final int id = nextId++;
	private String description;

	private DataCache dataCache;

	@CheckManager
	private CheckList<DataSourceHolder>.Checker mChecker;

	private static final int CLEAR_CACHE = 1;
	private static final int CLEAR_CACHE_AFTER_DEACTIVATION_DELAY = 10000;

	private Handler dataSourceHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == CLEAR_CACHE) {
				// Delayed clearing of cache after datasource has been
				// deactivated
				dataCache.clearCache();
				return true;
			}

			return false;
		}
	});
	private Class<? extends Filter> filterClass;
	private Filter currentFilter;
	private String name;
	private byte minZoomLevel;
	private byte maxZoomLevel;
	private Class<? extends DataSource<? super Filter>> dataSourceClass;
	private InstalledPluginHolder mPluginHolder;

	/**
	 * Wrapper for a {@link DataSource}. Provides access to general settings of
	 * a data source, as well as its {@link Filter} implementation and supported
	 * {@link Visualization}s. This holder also maintains the state of a
	 * {@link DataSource} as it automatically activates and deactivates a data
	 * source and clears its {@link DataCache} if required.
	 * 
	 * @param dataSourceClass
	 * @param pluginHolder
	 *            {@link InstalledPluginHolder} containing this data source
	 */
	@SuppressWarnings("unchecked")
	public DataSourceHolder(
			Class<? extends DataSource<? super Filter>> dataSourceClass,
			InstalledPluginHolder pluginHolder) {
		this.mPluginHolder = pluginHolder;
		this.dataSourceClass = dataSourceClass;
		Annotations.DataSource dataSourceAnnotation = dataSourceClass
				.getAnnotation(Annotations.DataSource.class);
		if (dataSourceAnnotation == null) {
			throw new RuntimeException("Class not annotated as datasource");
		}

		name = dataSourceAnnotation.name();
		description = dataSourceAnnotation.description();
		minReloadInterval = dataSourceAnnotation.minReloadInterval();
		cacheZoomLevel = dataSourceAnnotation.cacheZoomLevel();
		minZoomLevel = dataSourceAnnotation.minZoomLevel();
		maxZoomLevel = dataSourceAnnotation.maxZoomLevel();

		// Find filter by getting the actual generic parameter type of the
		// implemented DataSource interface

		Class<?> currentClass = dataSourceClass;
		while (currentClass != null) {
			Type[] interfaces = currentClass.getGenericInterfaces();
			for (Type interfaceType : interfaces) {
				ParameterizedType type = (ParameterizedType) interfaceType;
				if (!type.getRawType().equals(DataSource.class)) {
					continue;
				}

				filterClass = (Class<? extends Filter>) type
						.getActualTypeArguments()[0];
				try {
					this.currentFilter = filterClass.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(
							"Referenced filter has no appropriate constructor");
				} catch (IllegalAccessException e) {
					throw new RuntimeException(
							"Referenced filter has no appropriate constructor");
				}

			}

			if (filterClass == null) {
				currentClass = currentClass.getSuperclass();
			} else {
				break;
			}
		}
		if (filterClass == null) {
			throw new RuntimeException(
					"Data source does not specify a filter class");
		}

		dataCache = new BalancingDataCache(this);
	}

	public DataSource<? super Filter> getDataSource() {
		if (dataSource == null) {
			initializeDataSource();
			Log.i("GeoAR",
					"Data source is was not yet initialized, activation sequence missing");
		}
		return dataSource;
	}

	public long getMinReloadInterval() {
		return minReloadInterval;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public byte getCacheZoomLevel() {
		return cacheZoomLevel;
	}

	public byte getMinZoomLevel() {
		return minZoomLevel;
	}

	public byte getMaxZoomLevel() {
		return maxZoomLevel;
	}

	public CheckList<Visualization> getVisualizations() {
		return visualizations;
	}

	public Filter getCurrentFilter() {
		return currentFilter;
	}

	/**
	 * Method which injects all fields with GeoAR-{@link Annotations} and
	 * finally calls the {@link PostConstruct} method (if available) for any
	 * object.
	 * 
	 * @param target
	 *            Any object
	 */
	public void perfomInjection(Object target) {
		// Field injection
		try {
			for (Field f : target.getClass().getDeclaredFields()) {
				if (f.isAnnotationPresent(SystemService.class)) {
					String serviceName = f.getAnnotation(SystemService.class)
							.value();
					f.setAccessible(true);
					f.set(target, mPluginHolder.getPluginContext()
							.getSystemService(serviceName));
				}

				if (f.isAnnotationPresent(SharedHttpClient.class)) {
					f.setAccessible(true);
					f.set(target, PluginLoader.getSharedHttpClient());
				}

				if (f.isAnnotationPresent(Annotations.PluginContext.class)) {
					f.setAccessible(true);
					f.set(target, mPluginHolder.getPluginContext());
				}
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Post construct

		try {
			for (Method m : target.getClass().getDeclaredMethods()) {
				if (m.isAnnotationPresent(PostConstruct.class)) {
					m.setAccessible(true);
					m.invoke(target);
				}
			}
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initializeDataSource() {
		// Construction of data source instance

		try {
			dataSource = dataSourceClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("No default constructor for datasource");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No valid constructor for datasource");
		}

		// Field injection
		perfomInjection(dataSource);

		// Visualizations
		SupportedVisualization supportedVisualization = dataSourceClass
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
						perfomInjection(v);
						visualizationMap.put(visualizationClasses[i], v);
					}

					visualizations.add(v);
				}

				if (visualizations.size() > 0) {
					// Set first visualization as checked
					visualizations.checkItem(0, true);
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

	/**
	 * Prevents datasource from getting unloaded. Should be called when
	 * datasource is added to map/ar.
	 */
	// TODO perhaps package private
	public void activate() {
		Log.i("GeoAR", "Activating data source " + getName());
		if (dataSource == null) {
			initializeDataSource();
		}
		// prevents clearing of cache by removing messages
		dataSourceHandler.removeMessages(CLEAR_CACHE);
	}

	/**
	 * Queues unloading of datasource and cached data
	 */
	public void deactivate() {
		// Clears the cache 30s after calling this method
		Log.i("GeoAR", "Deactivating data source " + getName());
		dataSourceHandler.sendMessageDelayed(
				dataSourceHandler.obtainMessage(CLEAR_CACHE),
				CLEAR_CACHE_AFTER_DEACTIVATION_DELAY);
	}

	@CheckedChangedListener
	public void checkedChanged(boolean state) {
		if (state == true) {
			activate();
		} else {
			deactivate();
		}
	}

	public boolean isChecked() {
		return mChecker.isChecked();
	}

	public void setChecked(boolean state) {
		mChecker.setChecked(state);
	}

	public DataCache getDataCache() {
		return dataCache;
	}

	public Class<?> getFilterClass() {
		return filterClass;
	}

	public void createFilterDialog(Context context) {
		Intent intent = new Intent(context, FilterDialogActivity.class);
		intent.putExtra("dataSource", this);
		context.startActivity(intent);
	}

	public String getIdentifier() {
		return dataSourceClass.getSimpleName();
	}

	// Parcelable

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Parcel based on unique DataSourceHolder id
		dest.writeInt(id);
	}

	public static final Parcelable.Creator<DataSourceHolder> CREATOR = new Parcelable.Creator<DataSourceHolder>() {
		public DataSourceHolder createFromParcel(Parcel in) {
			int id = in.readInt();
			// Find DataSourceHolder with provided id
			for (DataSourceHolder holder : PluginLoader
					.getSelectedDataSources()) {
				if (holder.id == id) {
					return holder;
				}
			}

			return null;
		}

		public DataSourceHolder[] newArray(int size) {
			return new DataSourceHolder[size];
		}
	};

}