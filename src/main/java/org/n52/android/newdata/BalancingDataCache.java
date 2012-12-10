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

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.n52.android.alg.proj.MercatorRect;

/**
 * Special {@link DataCache} which will automatically create a proxy
 * {@link StrictDataCache} if a data source is only queryable with a too low
 * zoom value, e.g. if the data source can not serve data filtered by bounding
 * box at all.
 * 
 * In this case, the {@link StrictDataCache} will be set up with the zoom level
 * required by the data source, and this data cache will use a higher zoom
 * level, optimized for visualizations. Each data request will be made on the
 * underlying proxy data source which will perform spatial filtering.
 * 
 * To avoid deadlocks, the proxy data caches use their own thread pool.
 * 
 */
public class BalancingDataCache extends DataCache {

	private static final byte MIN_ZOOM = 8; // Zoom level below which a proxy
												// data cache will be used
	private static final byte ZOOM_GAP = 2; // Guaranteed zoom level difference
											// to proxy data cache

	private static ThreadPoolExecutor SHARED_PROXY_THREAD_POOL = (ThreadPoolExecutor) Executors
			.newFixedThreadPool(3);

	private DataCache proxyDataCache;

	public BalancingDataCache(DataSourceHolder dataSource) {
		super(dataSource);

		if (tileZoom < MIN_ZOOM) {
			proxyDataCache = new StrictDataCache(dataSource,
					SHARED_PROXY_THREAD_POOL);
			tileZoom = (byte) Math.max(tileZoom + ZOOM_GAP, MIN_ZOOM);
		}
	}

	@Override
	public void clearCache() {
		// TODO Lock?
		super.clearCache();
		if (proxyDataCache != null) {
			proxyDataCache.clearCache();
		}
	}

	@Override
	public void setFilter(Filter filter) {
		// TODO Lock?
		if (proxyDataCache != null) {
			proxyDataCache.setFilter(filter);
		}
		super.setFilter(filter);
	}

	@Override
	protected void requestDataForTile(final DataTile dataTile) {
		if (proxyDataCache != null) {
			proxyDataCache.getDataByBBox(dataTile.tile.getMercatorRect(),
					new GetDataBoundsCallback() {

						@Override
						public void onProgressUpdate(int progress, int size,
								int stepRequest) {
							// TODO Auto-generated method stub
						}

						@Override
						public void onReceiveDataUpdate(MercatorRect bbox,
								List<? extends SpatialEntity> data) {
							dataTile.setMeasurements(data);
						}

						@Override
						public void onAbort(MercatorRect bbox, int reason) {
							dataTile.abort(ABORT_UNKOWN);
						}
					}, false);
		} else {
			super.requestDataForTile(dataTile);
		}
	}

}
