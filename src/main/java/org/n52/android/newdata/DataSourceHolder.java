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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.n52.android.newdata.Annotations.SupportedVisualization;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

public class DataSourceHolder implements Parcelable {
	private static Map<Class<? extends Visualization>, Visualization> visualizationMap = new HashMap<Class<? extends Visualization>, Visualization>();
	private static int nextId = 0;
	private DataSource<? super Filter> dataSource;
	private CheckList<Visualization> visualizations = new CheckList<Visualization>();
	private Class<? extends Filter> filterClass;
	
	private String name;
	private long minReloadInterval;
	private byte preferredZoomLevel;
	private final int id = nextId++;
	public String description;

	private DataCache dataCache;

	private static final int CLEAR_CACHE = 1;

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

	@SuppressWarnings("unchecked")
	public DataSourceHolder(
			Class<? extends DataSource<? super Filter>> dataSourceClass) {

		Annotations.DataSource dataSourceAnnotation = dataSourceClass
				.getAnnotation(Annotations.DataSource.class);
		if (dataSourceAnnotation == null) {
			throw new RuntimeException("Class not annotated as datasource");
		}

		name = dataSourceAnnotation.name();
		description = dataSourceAnnotation.description();
		minReloadInterval = dataSourceAnnotation.minReloadInterval();
		preferredZoomLevel = dataSourceAnnotation.preferredZoomLevel();
		try {
			dataSource = dataSourceClass.newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException("No default constructor for datasource");
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No valid constructor for datasource");
		}

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
		
		// Find filter
		Type[] interfaces = dataSourceClass
				.getGenericInterfaces();
		for (Type interfaceType : interfaces) {
			ParameterizedType type = (ParameterizedType) interfaceType;
			if (!type.getRawType().equals(DataSource.class)) {
				continue;
			}
			
			this.filterClass = (Class<? extends Filter>) type.getActualTypeArguments()[0];
		}

		dataCache = new DataCache(this);
	}

	public org.n52.android.newdata.DataSource<? super Filter> getDataSource() {
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

	public byte getPreferredZoomLevel() {
		return preferredZoomLevel;
	}

	public CheckList<Visualization> getVisualizations() {
		return visualizations;
	}

	/**
	 * Prevents datasource from getting unloaded. Should be called when
	 * datasource is added to map/ar
	 */
	public void activate() {
		// prevents clearing of cache by removing messages
		dataSourceHandler.removeMessages(CLEAR_CACHE);
	}

	/**
	 * Queues unloading of datasource and cached data
	 */
	public void deactivate() {
		// Clears the cache 30s after calling this method
		dataSourceHandler.sendMessageDelayed(
				dataSourceHandler.obtainMessage(CLEAR_CACHE), 30000);
	}

	public DataCache getDataCache() {
		return dataCache;
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
			for (DataSourceHolder holder : DataSourceLoader.getInstance()
					.getAvailableDataSources()) {
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