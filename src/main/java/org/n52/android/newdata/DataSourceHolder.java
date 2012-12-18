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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import org.n52.android.newdata.CheckList.CheckedChangedListener;
import org.n52.android.newdata.filter.SettingsHelper;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class DataSourceHolder implements Parcelable {
	public static final Parcelable.Creator<DataSourceHolder> CREATOR = new Parcelable.Creator<DataSourceHolder>() {
		@Override
		public DataSourceHolder createFromParcel(Parcel in) {
			int id = in.readInt();
			// Find DataSourceHolder with provided id
			for (DataSourceHolder holder : PluginLoader.getDataSources()) {
				if (holder.id == id) {
					return holder;
				}
			}

			return null;
		}

		@Override
		public DataSourceHolder[] newArray(int size) {
			return new DataSourceHolder[size];
		}
	};
	private static final int CLEAR_CACHE = 1;
	private static final int CLEAR_CACHE_AFTER_DEACTIVATION_DELAY = 10000;

	private static int nextId = 0;
	private static Map<Class<? extends Visualization>, Visualization> visualizationMap = new HashMap<Class<? extends Visualization>, Visualization>();
	private byte cacheZoomLevel;
	private Class<? extends DataSource<? super Filter>> dataSourceClass;

	private Handler dataSourceHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (msg.what == CLEAR_CACHE) {
				// Delayed clearing of cache after datasource has been
				// deactivated
				for (DataSourceInstanceHolder instance : mDataSourceInstances) {
					instance.deactivate();
				}
				return true;
			}

			return false;
		}
	});
	private String description;

	private Class<? extends Filter> filterClass;
	private final int id = nextId++;

	private byte maxZoomLevel;
	private CheckList<DataSourceInstanceHolder> mDataSourceInstances;
	private long minReloadInterval;
	private boolean mInstanceable;
	private byte minZoomLevel;
	private InstalledPluginHolder mPluginHolder;
	private String name;

	private CheckList<Visualization> visualizations;

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
		mInstanceable = SettingsHelper.hasSettings(dataSourceClass);

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

	}

	/**
	 * Prevents datasource from getting unloaded. Should be called when
	 * datasource is added to map/ar.
	 */
	public void activate() {
		Log.i("GeoAR", "Activating data source " + getName());

		// prevents clearing of cache by removing messages
		dataSourceHandler.removeMessages(CLEAR_CACHE);
	}

	public boolean areAllChecked() {
		return mDataSourceInstances.allChecked();
	}

	/**
	 * Called by {@link CheckList} when this item gets un-/checked
	 * 
	 * @param state
	 */
	@CheckedChangedListener
	protected void checkedChanged(boolean state) {
		if (state == true) {
			activate();
		} else {
			deactivate();
		}
	}

	public void createFilterDialog(Context context) {
		// TODO
	}

	/**
	 * Queues unloading of datasource and cached data
	 */
	public void deactivate() {
		Log.i("GeoAR", "Deactivating data source " + getName());
		dataSourceHandler.sendMessageDelayed(
				dataSourceHandler.obtainMessage(CLEAR_CACHE),
				CLEAR_CACHE_AFTER_DEACTIVATION_DELAY);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	public byte getCacheZoomLevel() {
		return cacheZoomLevel;
	}

	public String getDescription() {
		return description;
	}

	public Class<? extends Filter> getFilterClass() {
		return filterClass;
	}

	public String getIdentifier() {
		return dataSourceClass.getSimpleName();
	}

	public CheckList<DataSourceInstanceHolder> getInstances() {
		if (mDataSourceInstances == null) {
			initializeInstances();
		}
		return mDataSourceInstances;
	}

	public byte getMaxZoomLevel() {
		return maxZoomLevel;
	}

	public long getMinReloadInterval() {
		return minReloadInterval;
	}

	public byte getMinZoomLevel() {
		return minZoomLevel;
	}

	public String getName() {
		return name;
	}

	/**
	 * Returns {@link Visualization}s supported by this data source.
	 * 
	 * As {@link DataSource}s get lazily initialized, this method will not
	 * return any {@link Visualization}s until the underlying {@link DataSource}
	 * is accessed, e.g. by {@link DataSourceHolder#getDataSource()}.
	 * 
	 * @return
	 */
	public CheckList<Visualization> getVisualizations() {
		if (visualizations == null) {
			initializeVisualizations();
		}
		return visualizations;
	}

	/**
	 * Indicates whether the represented data source can handle multiple
	 * instances
	 * 
	 * @return
	 */
	public boolean instanceable() {
		return mInstanceable;
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
		} catch (LinkageError e) {
			Log.e("GeoAR", "Data source " + getName() + " uses invalid class, "
					+ e.getMessage());
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

	public void restoreState(ObjectInputStream objectInputStream)
			throws IOException {
		if (!mInstanceable) {
			boolean checked = objectInputStream.readBoolean();
			if (checked || mDataSourceInstances != null) {
				// Set saved state if instance was checked or if it is already
				// initialized
				getInstances().get(0).setChecked(checked);
			}
		}
	}

	public void saveState(ObjectOutputStream objectOutputStream)
			throws IOException {
		if (!mInstanceable) {
			objectOutputStream.writeBoolean(mDataSourceInstances != null
					&& mDataSourceInstances.get(0).isChecked());
		}
	}

	/**
	 * Calling this method affects all {@link DataSourceInstanceHolder} of this
	 * object
	 * 
	 * @param state
	 */
	public void setChecked(boolean state) {
		if (mDataSourceInstances != null) {
			mDataSourceInstances.checkAll(state);
		}
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Parcel based on unique internal DataSourceHolder id
		dest.writeInt(id);
	}

	/**
	 * Lazily loads the instances of this data source. If it is not
	 * {@link DataSourceHolder#instanceable()}, a default single instance will
	 * be created.
	 */
	private void initializeInstances() {
		mDataSourceInstances = new CheckList<DataSourceInstanceHolder>(
				DataSourceInstanceHolder.class);

		if (!mInstanceable) {
			// data source has no instance-specific settings, create the single
			// instance
			Log.i("GeoAR", "Creating single-instance data source instance "
					+ getName());
			try {
				DataSource<? super Filter> dataSource = dataSourceClass
						.newInstance();
				perfomInjection(dataSource);

				mDataSourceInstances.add(new DataSourceInstanceHolder(this,
						dataSource));
			} catch (InstantiationException e) {
				throw new RuntimeException(
						"No default constructor for datasource");
			} catch (IllegalAccessException e) {
				throw new RuntimeException(
						"No valid constructor for datasource");
			}
		}

	}

	/**
	 * Lazily loads all supported {@link Visualization}s of this data source.
	 * {@link Visualization} are shared for all data sources.
	 */
	private void initializeVisualizations() {
		visualizations = new CheckList<Visualization>();

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
					visualizations.checkItem(v); // TODO
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

	public void addInstance(Context context) {
		DataSourceInstanceHolder instance;
		try {
			DataSource<? super Filter> dataSource = dataSourceClass
					.newInstance();
			perfomInjection(dataSource);
			instance = new DataSourceInstanceHolder(this, dataSource);
			mDataSourceInstances.add(instance);

			Intent intent = new Intent(context,
					DataSourceInstanceSettingsDialogActivity.class);
			intent.putExtra("dataSourceInstance", instance);
			context.startActivity(intent);
		} catch (InstantiationException e) {
			throw new RuntimeException("No default constructor for datasource");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No valid constructor for datasource");
		}

	}
}