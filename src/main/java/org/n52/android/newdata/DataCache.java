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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.n52.android.alg.OnProgressUpdateListener;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.Tile;

import android.util.Log;

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

	public static final int ABORT_UNKOWN = 0;
	public static final int ABORT_NO_CONNECTION = 1;
	public static final int ABORT_CANCELED = 2;
	public static final int STEP_REQUEST = 3;

	/**
	 * Future-like interface for cancellation of requests
	 * 
	 * 
	 */
	public interface RequestHolder {
		void cancel();
	}

	public interface GetDataCallback {
		void onReceiveMeasurements(Tile tile, List<SpatialEntity> data);

		void onAbort(Tile tile, int reason);
	}

	public abstract interface GetDataBoundsCallback extends
			OnProgressUpdateListener {
		void onReceiveDataUpdate(MercatorRect bbox, List<SpatialEntity> data);

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
		public List<SpatialEntity> data;

		protected List<GetDataCallback> getDataCallbacks = new ArrayList<GetDataCallback>();

		public void addCallback(GetDataCallback callback) {
			synchronized (getDataCallbacks) {
				getDataCallbacks.add(callback);
			}
		}

		public void setMeasurements(List<SpatialEntity> measurements) {
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

	protected Map<Tile, SoftReference<DataTile>> tileCacheMapping = new HashMap<Tile, SoftReference<DataTile>>();
	protected DataSourceHolder dataSource;
	protected ThreadPoolExecutor fetchingThreadPool;
	final protected byte tileZoom; // Zoom level for the tiling system of this
									// cache
	protected AbstractFilter dataFilter;

	public DataCache(DataSourceHolder dataSource) {
		this.dataSource = dataSource;
		tileZoom = dataSource.getPreferredZoomLevel();
		// TODO Filter
		//dataFilter = new Filter();// DataSourceAdapter.createMeasurementFilter();//
		// new MeasurementFilter();
		fetchingThreadPool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(3); // TODO make size changeable
	}

	public void setFilter(AbstractFilter filter) {
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
			// Cancel all operations
			for (SoftReference<DataTile> cacheReference : tileCacheMapping
					.values()) {
				if (cacheReference.get() != null) {
					cacheReference.get().abort(ABORT_CANCELED);
				}
			}
			tileCacheMapping.clear();
		}
	}

	public RequestHolder getDataByTile(Tile tile, GetDataCallback callback,
			boolean forceUpdate) {
		Log.i("Test", "getMeasures " + tile.x + ", " + tile.y);

		RequestHolder requestHolder = null;

		synchronized (tileCacheMapping) {
			SoftReference<DataTile> cacheReference = tileCacheMapping.get(tile);
			DataTile cachedTile = cacheReference != null ? cacheReference.get()
					: null;
			if (cachedTile != null) {
				// Tile bereits vorhanden
				if (cachedTile.hasMeasurements()) {
					callback.onReceiveMeasurements(cachedTile.tile,
							cachedTile.data);
				}
				if (!cachedTile.hasMeasurements()
						|| cachedTile.lastUpdate <= System.currentTimeMillis()
								- dataSource.getMinReloadInterval()
						|| forceUpdate) {
					cachedTile.addCallback(callback);
					requestHolder = fetchMeasurements(cachedTile);
				}
			} else {
				cachedTile = new DataTile(tile);
				cachedTile.addCallback(callback);
				tileCacheMapping.put(tile, new SoftReference<DataTile>(
						cachedTile));

				requestHolder = fetchMeasurements(cachedTile);
			}
		}

		return requestHolder;
	}

	private RequestHolder fetchMeasurements(final DataTile dataTile) {

		if (!dataTile.updatePending) {

			dataTile.updatePending = true;
			dataTile.fetchRunnable = new Runnable() {
				public void run() {
					// try {				
					dataTile.setMeasurements(dataSource.getDataSource()
							.getMeasurements(
									 dataFilter.clone().setTile(dataTile.tile)));
					// }
					// catch (RequestException e) {
					// Log.i("NoiseAR", "RequestException" + e.getMessage());
					// dataTile.abort(ABORT_UNKOWN);
					// } catch (ConnectException e) {
					// dataTile.abort(ABORT_NO_CONNECTION);
					// }
				}
			};
			fetchingThreadPool.execute(dataTile.fetchRunnable);
		}

		return new RequestHolder() {
			public void cancel() {
				fetchingThreadPool.remove(dataTile.fetchRunnable);
			}
		};
	}

	// TODO ByGeoLocationRect
	// TODO reuse of arrays
	public RequestHolder getDataByBBox(final MercatorRect bounds,
			final GetDataBoundsCallback callback, boolean forceUpdate) {
		// Transform provided bounds into tile bounds using the zoom level of
		// this cache
		final int tileLeftX = (int) MercatorProj
				.transformPixelXToTileX(MercatorProj.transformPixel(
						bounds.left, bounds.zoom, tileZoom), tileZoom);
		final int tileTopY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.top, bounds.zoom, tileZoom),
				tileZoom);
		int tileRightX = (int) MercatorProj.transformPixelXToTileX(MercatorProj
				.transformPixel(bounds.right, bounds.zoom, tileZoom), tileZoom);
		int tileBottomY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.bottom, bounds.zoom,
						tileZoom), tileZoom);
		final int tileGridWidth = tileRightX - tileLeftX + 1;

		// List to monitor loading of all data for all required tiles
		final Vector<List<SpatialEntity>> tileDataList = new Vector<List<SpatialEntity>>();
		tileDataList.setSize(tileGridWidth * (tileBottomY - tileTopY + 1));

		// Callback for data of a tile
		final GetDataCallback measureCallback = new GetDataCallback() {
			boolean active = true;
			private int progress;

			public void onReceiveMeasurements(Tile tile,
					List<SpatialEntity> data) {

				if (!active) {
					return;
				}

				int checkIndex = ((tile.y - tileTopY) * tileGridWidth)
						+ (tile.x - tileLeftX);

				if (tileDataList.set(checkIndex, data) == null) {
					// Previously no elements for that tile
					progress++;
					callback.onProgressUpdate(progress, tileDataList.size(),
							STEP_REQUEST);
				}
				if (!tileDataList.contains(null)) {
					// All tiles loaded
					final List<SpatialEntity> measurementsList = new ArrayList<SpatialEntity>();
					// Merge all data of each requested tile
					for (List<SpatialEntity> tileData : tileDataList) {
						measurementsList.addAll(tileData);
					}

					new Thread(new Runnable() {
						public void run() {
							if (active) {
								callback.onReceiveDataUpdate(bounds,
										measurementsList);
							}
						}
					}).run();
				}
			}

			public void onAbort(Tile tile, int reason) {
				callback.onAbort(bounds, reason);
				if (reason == ABORT_CANCELED) {
					active = false;
				}
			}

		};

		// Actually request data
		for (int y = tileTopY; y <= tileBottomY; y++)
			for (int x = tileLeftX; x <= tileRightX; x++) {
				Tile tile = new Tile(x, y, tileZoom);

				getDataByTile(tile, measureCallback, forceUpdate);
				// TODO cache return value to allow proper cancellation behavior
			}

		return new RequestHolder() {
			public void cancel() {
				measureCallback.onAbort(null, ABORT_CANCELED);
			}
		};
	}

}
