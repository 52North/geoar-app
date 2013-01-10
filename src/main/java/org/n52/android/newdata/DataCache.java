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

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.Tile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to request measurements from a specific {@link DataSource}. Builds
 * an automatic tile based cache of measurements to reduce data transfer. Update
 * interval of cached tiles is controlled by, cache gets freed as required by
 * the VM
 * 
 * @author Holger Hopmann
 * @author Arne de Wall
 */
public class DataCache {

	/**
	 * Future-like interface for cancellation of requests
	 * 
	 */
	public interface RequestHolder {
		void cancel();
	}

	public interface OnProgressUpdateListener {
		void onProgressUpdate(int progress, int size, int stepRequest);
	}

	public interface GetDataCallback {
		void onReceiveMeasurements(Tile tile, List<? extends SpatialEntity> data);

		void onAbort(Tile tile, int reason);
	}

	public abstract interface GetDataBoundsCallback extends
			OnProgressUpdateListener {
		void onReceiveDataUpdate(MercatorRect bbox,
				List<? extends SpatialEntity> data);

		void onAbort(MercatorRect bbox, int reason);
	}

	/**
	 * A tile in the cache
	 */
	public class DataTile {
		public Tile tile;
		public long lastUpdate;
		public boolean updatePending;
		public Runnable fetchRunnable;
		public List<? extends SpatialEntity> data;

		protected List<GetDataCallback> getDataCallbacks = new ArrayList<GetDataCallback>();

		public void addCallback(GetDataCallback callback) {
			synchronized (getDataCallbacks) {
				getDataCallbacks.add(callback);
			}
		}

		public void setMeasurements(List<? extends SpatialEntity> measurements) {
			lastUpdate = System.currentTimeMillis();
			this.data = measurements;

			synchronized (getDataCallbacks) {
				updatePending = false;
				for (GetDataCallback callback : getDataCallbacks) {
					callback.onReceiveMeasurements(this.tile, this.data);
				}
				getDataCallbacks.clear();
			}
		}

		public void abort(int reason) {
			synchronized (getDataCallbacks) {
				updatePending = false;
				for (GetDataCallback callback : getDataCallbacks) {
					callback.onAbort(this.tile, reason);
				}
				getDataCallbacks.clear();
			}
		}

		public DataTile(Tile tile) {
			this.tile = tile;
		}

		public boolean hasMeasurements() {
			return data != null;
		}

	}

	public static final int ABORT_UNKOWN = 0;
	public static final int ABORT_NO_CONNECTION = 1;
	public static final int ABORT_CANCELED = 2;
	public static final int STEP_REQUEST = 3;

	private static final long MIN_RELOAD_INTERVAL = 60000;

	private static ThreadPoolExecutor SHARED_THREAD_POOL = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(3);

	protected Map<Tile, SoftReference<DataTile>> tileCacheMapping = new HashMap<Tile, SoftReference<DataTile>>();
	protected DataSourceInstanceHolder dataSource;
	private ThreadPoolExecutor fetchingThreadPool;
	protected byte tileZoom; // Zoom level for the tiling system of this
								// cache
	protected Filter dataFilter;
	protected String logTag;
	private long minReloadInterval;

	private static final Logger LOG = LoggerFactory.getLogger(DataCache.class);

	public DataCache(DataSourceInstanceHolder dataSource) {
		this(dataSource, SHARED_THREAD_POOL);
	}

	public DataCache(DataSourceInstanceHolder dataSource,
			ThreadPoolExecutor fetchingThreadPool) {
		this(dataSource, dataSource.getParent().getCacheZoomLevel(),
				fetchingThreadPool);
	}

	public DataCache(DataSourceInstanceHolder dataSource, byte tileZoom,
			ThreadPoolExecutor fetchingThreadPool) {
		this.dataSource = dataSource;
		this.tileZoom = tileZoom;
		this.logTag = getClass().getSimpleName() + " " + dataSource.getName();
		this.fetchingThreadPool = fetchingThreadPool;
		minReloadInterval = this.dataSource.getParent().getMinReloadInterval();
		if (minReloadInterval <= 0) {
			minReloadInterval = Long.MAX_VALUE;
		} else {
			minReloadInterval = Math
					.max(minReloadInterval, MIN_RELOAD_INTERVAL);
		}

		dataFilter = dataSource.getCurrentFilter();

	}

	/**
	 * Sets a new {@link Filter} to use for requesting data. As the cached data
	 * might not match this filter, the cache will be cleared.
	 * 
	 * @param filter
	 */
	public void setFilter(Filter filter) {
		// Perhaps use a re-requesting mechanism as used in NoiseDroid, i.e.
		// find a trade off between clearing the whole cache and
		// requesting/removing missing/extra data.
		clearCache();
		this.dataFilter = filter;
	}

	public Filter getFilter() {
		return dataFilter;
	}

	/**
	 * Cancels all fetching operations and clears the cache
	 */
	public void clearCache() {
		synchronized (tileCacheMapping) {
			LOG.info(logTag + " Clearing cache");
			// Cancel all operation
			for (SoftReference<DataTile> cacheReference : tileCacheMapping
					.values()) {
				if (cacheReference.get() != null) {
					cacheReference.get().abort(ABORT_CANCELED);
				}
			}
			tileCacheMapping.clear();
		}
	}

	/**
	 * Method to request data by spatial index {@link Tile}. This method will
	 * automatically fetch new data if the specified {@link Tile} is (no longer)
	 * cached and based on the expiration settings of the underlying
	 * {@link DataSource}.
	 * 
	 * @param tile
	 * @param callback
	 *            The callback which will receive the requested data
	 * @param forceUpdate
	 *            Allows to force requesting of new data instead of returning
	 *            cached date
	 * @return Holder to cancel this request
	 */
	public RequestHolder getDataByTile(Tile tile, GetDataCallback callback,
			boolean forceUpdate) {
		return getDataByTile(tile, callback, forceUpdate, true);
	}

	/**
	 * Method to request data by spatial index {@link Tile}. This method will
	 * automatically fetch new data if the specified {@link Tile} is (no longer)
	 * cached and based on the expiration settings of the underlying
	 * {@link DataSource}.
	 * 
	 * @param tile
	 * @param callback
	 *            The callback which will receive the requested data
	 * @param forceUpdate
	 *            Allows to force requesting of new data instead of returning
	 *            cached date
	 * @return Holder to cancel this request
	 */
	private RequestHolder getDataByTile(Tile tile, GetDataCallback callback,
			boolean forceUpdate, boolean async) {
		RequestHolder requestHolder = null;

		synchronized (tileCacheMapping) {
			if (forceUpdate) {
				// Log.i(logTag, "Forcing update");
			}

			SoftReference<DataTile> cacheReference = tileCacheMapping.get(tile);
			DataTile cachedTile = cacheReference != null ? cacheReference.get()
					: null;
			if (cachedTile != null) {
				// Tile bereits vorhanden
				if (cachedTile.hasMeasurements() && !forceUpdate) {
					// Log.i(logTag, "Tile Cache direct hit " + tile.x + ", "
					// + tile.y);

					callback.onReceiveMeasurements(cachedTile.tile,
							cachedTile.data);
				}
				if (!cachedTile.hasMeasurements()
						|| cachedTile.lastUpdate <= System.currentTimeMillis()
								- minReloadInterval || forceUpdate) {
					// Log.i(logTag, "Tile Cache hit " + tile.x + ", " +
					// tile.y);
					cachedTile.addCallback(callback);
					requestHolder = fetchMeasurements(cachedTile, async);
				}
			} else {
				// Log.i(logTag, "Tile Cache miss " + tile.x + ", " + tile.y);
				cachedTile = new DataTile(tile);
				cachedTile.addCallback(callback);
				tileCacheMapping.put(tile, new SoftReference<DataTile>(
						cachedTile));

				requestHolder = fetchMeasurements(cachedTile, async);
			}
		}

		return requestHolder;
	}

	/**
	 * Queues fetching of data for a {@link DataTile} using the currently set
	 * {@link Filter}.
	 * 
	 * @param dataTile
	 * @return Holder to dequeue the request
	 */
	private RequestHolder fetchMeasurements(final DataTile dataTile,
			boolean async) {

		if (!dataTile.updatePending) {

			dataTile.updatePending = true;
			dataTile.fetchRunnable = new Runnable() {
				public void run() {
					// try {

					LOG.debug(logTag + " Requesting Data " + dataTile.tile.x
							+ ", " + dataTile.tile.y);

					try {
						requestDataForTile(dataTile);

					} catch (RuntimeException e) {
						e.printStackTrace();
						dataTile.abort(ABORT_UNKOWN);
					}
					// TODO exception handling

					// }
					// catch (RequestException e) {
					// Log.i("NoiseAR", "RequestException" + e.getMessage());
					// dataTile.abort(ABORT_UNKOWN);
					// } catch (ConnectException e) {
					// dataTile.abort(ABORT_NO_CONNECTION);
					// }
				}
			};
			if (async) {
				fetchingThreadPool.execute(dataTile.fetchRunnable);
			} else {
				dataTile.fetchRunnable.run();
			}
		}

		return new RequestHolder() {
			public void cancel() {
				fetchingThreadPool.remove(dataTile.fetchRunnable);
			}
		};
	}

	protected void requestDataForTile(DataTile dataTile) {
		Filter filter = dataFilter.clone().setBoundingBox(
				dataTile.tile.getGeoLocationRect());

		// Actual access to DataSoure interface
		dataTile.setMeasurements(dataSource.getDataSource().getMeasurements(
				filter));
	}

	/**
	 * Requests data for a specific spatial bounding box. Internally determines
	 * all tiles from the tile cache which intersect the bounding box,
	 * concurrently requests data for each tile and returns their aggregated
	 * results via the specified callback.
	 * 
	 * Note that returned {@link SpatialEntity}s may lie outside the requested
	 * bounding box.
	 * 
	 * @param bounds
	 *            The minimum bounding box to request data for
	 * @param callback
	 *            The callback will finally receive the requested data
	 * @param forceUpdate
	 *            Forces to update the cache instead of returned cached data
	 * @return Holder to cancel this request
	 */
	// TODO ByGeoLocationRect
	// TODO reuse of result arrays, less allocations
	public RequestHolder getDataByBBox(final MercatorRect bounds,
			final GetDataBoundsCallback callback, final boolean forceUpdate) {
		// Transform provided bounds into tile bounds using the zoom level of
		// this cache
		final int tileLeftX = (int) MercatorProj
				.transformPixelXToTileX(MercatorProj.transformPixel(
						bounds.left, bounds.zoom, tileZoom), tileZoom);
		final int tileTopY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.top, bounds.zoom, tileZoom),
				tileZoom);
		final int tileRightX = (int) MercatorProj.transformPixelXToTileX(
				MercatorProj
						.transformPixel(bounds.right, bounds.zoom, tileZoom),
				tileZoom);
		final int tileBottomY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.bottom, bounds.zoom,
						tileZoom), tileZoom);
		final int tileGridWidth = tileRightX - tileLeftX + 1;
		final int tileCount = tileGridWidth * (tileBottomY - tileTopY + 1);
		// List to monitor loading of all data for all required tiles
		final BitSet tileMonitorSet = new BitSet(tileCount);
		tileMonitorSet.set(0, tileCount);

		// final Vector<List<? extends SpatialEntity>> tileDataList = new
		// Vector<List<? extends SpatialEntity>>();
		// tileDataList.setSize(tileGridWidth * (tileBottomY - tileTopY + 1));

		// Callback for data of a tile
		final GetDataCallback measureCallback = new GetDataCallback() {
			boolean active = true;
			private int progress;
			List<SpatialEntity> measurementsList = new ArrayList<SpatialEntity>();

			public void onReceiveMeasurements(Tile tile,
					List<? extends SpatialEntity> data) {

				if (!active) {
					return;
				}

				int checkIndex = ((tile.y - tileTopY) * tileGridWidth)
						+ (tile.x - tileLeftX);

				if (tileMonitorSet.get(checkIndex)) {
					// Still waiting for that tile
					tileMonitorSet.clear(checkIndex);
					progress++;
					measurementsList.addAll(data);
					callback.onProgressUpdate(progress, tileCount, STEP_REQUEST);
				}
				if (tileMonitorSet.isEmpty()) {
					// All tiles loaded

					// new Thread(new Runnable() {
					// public void run() {
					if (active) {
						callback.onReceiveDataUpdate(bounds, measurementsList);
					}
					// }
					// }).run();
				}
			}

			public void onAbort(Tile tile, int reason) {
				callback.onAbort(bounds, reason);
				if (reason == ABORT_CANCELED) {
					active = false;
				}
			}

		};

		callback.onProgressUpdate(0, tileCount, STEP_REQUEST);

		// Actually request data

		Runnable getAllTilesRunnable = new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub

				// Log.i(logTag, "DataByBBox, " + tileCount +
				// " Tiles affected ");
				for (int y = tileTopY; y <= tileBottomY; y++)
					for (int x = tileLeftX; x <= tileRightX; x++) {
						Tile tile = new Tile(x, y, tileZoom);

						getDataByTile(tile, measureCallback, forceUpdate, false);
						// TODO cache return value to allow proper cancellation
						// behavior
					}
			}
		};
		fetchingThreadPool.execute(getAllTilesRunnable);

		return new RequestHolder() {
			public void cancel() {
				measureCallback.onAbort(null, ABORT_CANCELED);
			}
		};
	}

}
