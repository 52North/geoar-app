package org.n52.android.newdata;

import org.n52.android.newdata.CheckList.CheckManager;
import org.n52.android.newdata.filter.FilterDialogActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class DataSourceInstanceHolder implements Parcelable {
	private static int nextId = 0;
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
	private DataSource<? super Filter> dataSource;
	private DataSourceHolder parentHolder;
	private final int id = nextId++;
	private DataCache dataCache;
	private Filter currentFilter;
	@CheckManager
	private CheckList<DataSourceInstanceHolder>.Checker mChecker;

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

		dataCache = new BalancingDataCache(this);
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

	public DataCache getDataCache() {
		return dataCache;
	}

	public void createFilterDialog(Context context) {
		Intent intent = new Intent(context, FilterDialogActivity.class);
		intent.putExtra("dataSourceInstance", this);
		context.startActivity(intent);
	}

	public Filter getCurrentFilter() {
		return currentFilter;
	}

	public DataSourceHolder getParent() {
		return parentHolder;
	}

	public String getName() {
		return parentHolder.getName() + " " + id;
		// TODO
	}

	public DataSource<? super Filter> getDataSource() {
		return dataSource;
	}

	/**
	 * Checks whether the underlying {@link DataSource} is already created
	 * 
	 * @return
	 */
	public boolean isInitialized() {
		return dataSource != null;
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
			DataSourceHolder dataSource = in.readParcelable(DataSourceHolder.class.getClassLoader());
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

}
