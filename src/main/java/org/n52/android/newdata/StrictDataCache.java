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

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import org.n52.android.alg.proj.MercatorRect;

/**
 * {@link DataCache} which will strictly filter data results by bounding box.
 * 
 */
public class StrictDataCache extends DataCache {

	public StrictDataCache(DataSourceHolder dataSource, byte tileZoom,
			ThreadPoolExecutor fetchingThreadPool) {
		super(dataSource, tileZoom, fetchingThreadPool);
	}

	public StrictDataCache(DataSourceHolder dataSource,
			ThreadPoolExecutor fetchingThreadPool) {
		super(dataSource, fetchingThreadPool);
	}

	@Override
	public RequestHolder getDataByBBox(MercatorRect bounds,
			final GetDataBoundsCallback callback, boolean forceUpdate) {
		GetDataBoundsCallback strictCallback = new GetDataBoundsCallback() {

			@Override
			public void onProgressUpdate(int progress, int size, int stepRequest) {
				callback.onProgressUpdate(progress, size, stepRequest);
			}

			@Override
			public void onReceiveDataUpdate(MercatorRect bbox,
					List<? extends SpatialEntity> data) {
				Iterator<? extends SpatialEntity> iterator = data.iterator();
				while (iterator.hasNext()) {
					SpatialEntity entity = iterator.next();
					if (!bbox.contains(entity))
						iterator.remove();
				}

				callback.onReceiveDataUpdate(bbox, data);
			}

			@Override
			public void onAbort(MercatorRect bbox, int reason) {
				callback.onAbort(bbox, reason);
			}
		};
		return super.getDataByBBox(bounds, strictCallback, forceUpdate);
	}

}
