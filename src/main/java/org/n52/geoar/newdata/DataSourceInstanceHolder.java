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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.Set;

import org.n52.geoar.GeoARApplication;
import org.n52.geoar.R;
import org.n52.geoar.newdata.CheckList.CheckManager;
import org.n52.geoar.newdata.CheckList.CheckedChangedListener;
import org.n52.geoar.newdata.DataSourceInstanceSettingsDialogActivity.SettingsResultListener;
import org.n52.geoar.settings.SettingsHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;

public class DataSourceInstanceHolder implements Parcelable {

	public interface DataSourceSettingsChangedListener {
		void onDataSourceSettingsChanged();
	}

	private static int nextId = 0;
	private static final int CLEAR_CACHE = 1;
	private static final int CLEAR_CACHE_AFTER_DEACTIVATION_DELAY = 10000;
	private static final Logger LOG = LoggerFactory
			.getLogger(DataSourceInstanceHolder.class);

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
	private DataSource<? super Filter> dataSource;
	private boolean injected = false;
	private DataSourceHolder parentHolder;
	private final int id = nextId++;
	private DataCache dataCache;
	private Filter currentFilter;
	private Boolean hasSettings = null;
	@CheckManager
	private CheckList<DataSourceInstanceHolder>.Checker mChecker;
	private Exception lastError;
	private Set<DataSourceSettingsChangedListener> mSettingsChangedListeners = new HashSet<DataSourceInstanceHolder.DataSourceSettingsChangedListener>(
			0);

	public DataSourceInstanceHolder(DataSourceHolder parentHolder,
			DataSource<? super Filter> dataSource) {
		this.parentHolder = parentHolder;
		this.dataSource = dataSource;
		try {
			this.currentFilter = parentHolder.getFilterClass().newInstance();
		} catch (InstantiationException e) {
			throw new RuntimeException(
					"Referenced filter has no appropriate constructor");
		} catch (IllegalAccessException e) {
			throw new RuntimeException(
					"Referenced filter has no appropriate constructor");
		}

		dataCache = new DataCache(this);
	}

	@CheckedChangedListener
	public void checkedChanged(boolean state) {
		if (state) {
			activate();
		} else {
			deactivate();
		}
	}

	/**
	 * Prevents datasource from getting unloaded. Should be called when
	 * datasource is added to map/ar.
	 */
	public void activate() {
		LOG.info("Activating data source instance " + getName());

		// prevents clearing of cache by removing messages
		dataSourceHandler.removeMessages(CLEAR_CACHE);
		if (!injected) {
			parentHolder.perfomInjection(dataSource);
			injected = true;
		}
	}

	/**
	 * Queues unloading of datasource and cached data
	 */
	public void deactivate() {
		// Clears the cache 30s after calling this method
		LOG.info("Deactivating data source " + getName());
		dataSourceHandler.sendMessageDelayed(
				dataSourceHandler.obtainMessage(CLEAR_CACHE),
				CLEAR_CACHE_AFTER_DEACTIVATION_DELAY);
	}

	public DataCache getDataCache() {
		return dataCache;
	}

	public void createSettingsDialog(Context context) {
		SettingsResultListener resultListener = new SettingsResultListener() {
			@Override
			void onSettingsResult(int resultCode) {
				if (resultCode == Activity.RESULT_OK) {
					notifySettingsChanged();
				}
			}
		};

		Intent intent = new Intent(context,
				DataSourceInstanceSettingsDialogActivity.class);
		intent.putExtra("dataSourceInstance", this);
		intent.putExtra("resultListener", resultListener); // unsure whether
															// Intent uses weak
															// references too
		context.startActivity(intent);
	}

	public Filter getCurrentFilter() {
		return currentFilter;
	}

	public DataSourceHolder getParent() {
		return parentHolder;
	}

	public void addOnSettingsChangedListener(
			DataSourceSettingsChangedListener listener) {
		mSettingsChangedListeners.add(listener);
	}

	public void removeOnSettingsChangedListener(
			DataSourceSettingsChangedListener listener) {
		mSettingsChangedListeners.remove(listener);
	}

	/**
	 * It does not only notify listeners, but also clears the current cache.
	 * TODO
	 */
	void notifySettingsChanged() {
		dataCache.setFilter(currentFilter);
		for (DataSourceSettingsChangedListener listener : mSettingsChangedListeners) {
			listener.onDataSourceSettingsChanged();
		}
	}

	public String getName() {
		if (parentHolder.getNameCallbackMethod() != null) {
			// try to use name callback
			try {
				return (String) parentHolder.getNameCallbackMethod().invoke(
						dataSource);
			} catch (Exception e) {
				LOG.warn("Data Source " + parentHolder.getName()
						+ " NameCallback fails");
			}
		}

		if (parentHolder.instanceable()) {
			return parentHolder.getName() + " " + id;
		} else {
			return parentHolder.getName();
		}
	}

	public DataSource<? super Filter> getDataSource() {
		return dataSource;
	}

	public boolean hasSettings() {
		if (hasSettings == null) {
			hasSettings = SettingsHelper.hasSettings(getCurrentFilter())
					|| SettingsHelper.hasSettings(getDataSource());
		}

		return hasSettings;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// Parcel based on unique DataSourceHolder id
		dest.writeParcelable(parentHolder, 0);
		dest.writeInt(id);
	}

	public static final Parcelable.Creator<DataSourceInstanceHolder> CREATOR = new Parcelable.Creator<DataSourceInstanceHolder>() {
		public DataSourceInstanceHolder createFromParcel(Parcel in) {
			DataSourceHolder dataSource = in
					.readParcelable(DataSourceHolder.class.getClassLoader());
			int id = in.readInt();
			// Find DataSourceInstance with provided id
			for (DataSourceInstanceHolder instance : dataSource.getInstances()) {
				if (instance.id == id) {
					return instance;
				}
			}

			return null;
		}

		public DataSourceInstanceHolder[] newArray(int size) {
			return new DataSourceInstanceHolder[size];
		}
	};

	public boolean isChecked() {
		return mChecker.isChecked();
	}

	public void setChecked(boolean state) {
		mChecker.setChecked(state);
	}

	public void saveState(ObjectOutputStream objectOutputStream)
			throws IOException {

		// Store filter, serializable
		objectOutputStream.writeObject(currentFilter);

		// Store data source instance settings using settings framework
		SettingsHelper.storeSettings(objectOutputStream, this.dataSource);

		objectOutputStream.writeBoolean(isChecked());
	}

	public void restoreState(ObjectInputStream objectInputStream)
			throws IOException {
		try {
			// restore filter, serializable
			currentFilter = (Filter) objectInputStream.readObject();
			dataCache.setFilter(currentFilter);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// restore data source instance settings, settings framework
		SettingsHelper.restoreSettings(objectInputStream, this.dataSource);

		setChecked(objectInputStream.readBoolean());
	}

	public void reportError(Exception e) {
		lastError = e;
	}

	public void clearError() {
		lastError = null;
	}

	public String getErrorString() {
		if (lastError == null) {
			return null;
		}

		if (lastError instanceof SocketException) {
			return GeoARApplication.applicationContext
					.getString(R.string.connection_error);
		} else if (lastError instanceof PluginException) {
			return ((PluginException) lastError).getTitle();
		}

		return GeoARApplication.applicationContext
				.getString(R.string.unknown_error);
	}

	public boolean hasErrorMessage() {
		if (lastError == null) {
			return false;
		}
		return lastError instanceof PluginException;
	}

	public String getErrorMessage() {
		if (lastError == null || !(lastError instanceof PluginException)) {
			return null;
		}

		return lastError.toString();
	}
}
