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

import java.net.SocketException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.n52.geoar.alg.proj.MercatorProj;
import org.n52.geoar.alg.proj.MercatorRect;
import org.n52.geoar.utils.GeoLocationRect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.SystemClock;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.index.ItemVisitor;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * Interface to request data from a specific {@link DataSource}. Builds an
 * automatic tile based cache of measurements to reduce data transfer.
 * 
 * @author Holger Hopmann
 * @author Arne de Wall
 */
public class DataCache {

    private static final int MAX_TILES_TO_REMOVE = 25;

    /**
     * Future-like interface for cancellation of requests
     * 
     */
    public interface Cancelable {
        void cancel();
    }

    public interface OnProgressUpdateListener {
        void onProgressUpdate(int progress, int size);
    }

    public interface GetDataCallback {
        void onReceiveMeasurements(
                List<? extends SpatialEntity2<? extends Geometry>> data);

        void onAbort(DataSourceErrorType reason);
    }

    public abstract interface GetDataBoundsCallback extends
            OnProgressUpdateListener {
        void onReceiveDataUpdate(MercatorRect bbox,
                List<? extends SpatialEntity2<? extends Geometry>> data);

        void onAbort(MercatorRect bbox, DataSourceErrorType reason);
    }

    private static Cancelable NOOPCANCELABLE = new Cancelable() {

        @Override
        public void cancel() {
        }
    };

    private interface DataCallback {
        void onDataReceived();

        void onAbort(DataSourceErrorType reason);
    }

    /**
     * A tile in the cache
     */
    public class DataTile {

        private static final int CLEANUP_TILES_MIN_COUNT = 50;
        private Envelope tileEnvelope;
        private long lastUpdate;
        private long lastUsage;
        private boolean updateRequired = true;
        private int numEntities;

        private List<DataCallback> awaitDataCallbacks = new ArrayList<DataCallback>();
        private final Runnable fetchRunnable = new Runnable() {

            @Override
            public void run() {
                Filter filter = dataFilter.clone().setBoundingBox(
                        new GeoLocationRect((float) tileEnvelope.getMinX(),
                                (float) tileEnvelope.getMaxY(),
                                (float) tileEnvelope.getMaxX(),
                                (float) tileEnvelope.getMinY()));

                try {
                    // Actual access to DataSource interface
                    LOG.debug("Requesting data from data source");
                    List<? extends SpatialEntity2<? extends Geometry>> data = dataSourceInstance
                            .getDataSource().getMeasurements(filter);
                    numEntities = data.size();
                    addData(tileEnvelope, data);
                    data.clear();
                    dataSourceInstance.clearError(); // XXX no error reporting
                                                     // if following request
                                                     // does not fail
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error(logTag + " Exception on request", e);
                    dataSourceInstance.reportError(e);
                    if (e instanceof SocketException) {
                        abort(DataSourceErrorType.CONNECTION);
                    } else {
                        abort(DataSourceErrorType.UNKNOWN);
                    }
                    return;
                }

                synchronized (awaitDataCallbacks) {
                    for (DataCallback callback : awaitDataCallbacks) {
                        callback.onDataReceived();
                    }
                    awaitDataCallbacks.clear();

                    lastUpdate = SystemClock.uptimeMillis();
                    updateRequired = false;
                    LOG.debug("Tile update finished");
                }
            }
        };

        private void addCallback(DataCallback callback) {
            synchronized (awaitDataCallbacks) {
                awaitDataCallbacks.add(callback);

                if (awaitDataCallbacks.size() == 1) {
                    fetchingThreadPool.execute(fetchRunnable);
                }
            }
        }

        private void removeCallback(DataCallback callback) {
            synchronized (awaitDataCallbacks) {
                awaitDataCallbacks.remove(callback);
                if (awaitDataCallbacks.isEmpty()) {
                    fetchingThreadPool.remove(fetchRunnable);
                }
            }
        }

        public Cancelable awaitData(final DataCallback callback,
                boolean forceUpdate) {
            lastUsage = SystemClock.uptimeMillis();

            cleanupTilesCounter++;
            if (cleanupTilesCounter >= CLEANUP_TILES_MIN_COUNT) {
                cleanupTilesCounter = 0;
                removeUnusedTiles();
            }

            if (forceUpdate || requiresUpdate()) {
                updateRequired = true;
            }
            if (updateRequired) {
                addCallback(callback);
                return new Cancelable() {
                    @Override
                    public void cancel() {
                        removeCallback(callback);
                        callback.onAbort(DataSourceErrorType.CANCELED);
                    }
                };
            } else {
                callback.onDataReceived();
                return NOOPCANCELABLE;
            }
        }

        public Cancelable getData(final Envelope envelope,
                final GetDataCallback callback, boolean forceUpdate) {
            return awaitData(new DataCallback() {

                @Override
                public void onDataReceived() {
                    final List<SpatialEntity2<? extends Geometry>> resultList = new ArrayList<SpatialEntity2<? extends Geometry>>();
                    synchronized (mEntityIndex) {
                        mEntityIndex.query(envelope, new ItemVisitor() {
                            @Override
                            public void visitItem(Object item) {
                                @SuppressWarnings("unchecked")
                                SpatialEntity2<? extends Geometry> entity = (SpatialEntity2<? extends Geometry>) item;
                                if (envelope.intersects(entity.getEnvelope())) {
                                    resultList.add(entity);
                                }
                            }
                        });
                    }
                    callback.onReceiveMeasurements(resultList);
                }

                @Override
                public void onAbort(DataSourceErrorType reason) {
                    callback.onAbort(reason);
                }
            }, forceUpdate);
        }

        public Cancelable getData(final GetDataCallback callback,
                boolean forceUpdate) {
            return getData(tileEnvelope, callback, forceUpdate);
        }

        public void abort(DataSourceErrorType reason) {
            synchronized (awaitDataCallbacks) {
                // updatePending = false; // XXX
                for (DataCallback callback : awaitDataCallbacks) {
                    callback.onAbort(reason);
                }
                awaitDataCallbacks.clear();
                fetchingThreadPool.remove(fetchRunnable);
            }
        }

        public DataTile(Envelope tileEnvelope) {
            this.tileEnvelope = tileEnvelope;
        }

        public boolean requiresUpdate() {
            return lastUpdate <= SystemClock.uptimeMillis() - minReloadInterval;
        }

    }

    public enum DataSourceErrorType {
        UNKNOWN, CONNECTION, CANCELED
    }

    private static final long MIN_RELOAD_INTERVAL = 60000;

    private static ThreadPoolExecutor SHARED_THREAD_POOL = (ThreadPoolExecutor) Executors
            .newFixedThreadPool(3);

    private DataSourceInstanceHolder dataSourceInstance;
    private ThreadPoolExecutor fetchingThreadPool;
    // protected byte tileZoom; // Zoom level for the tiling system of this
    // cache
    private Filter dataFilter;
    private String logTag;
    private long minReloadInterval;

    private long cleanupTilesCounter;

    private Quadtree mQueryIndex = new Quadtree();
    private Quadtree mEntityIndex = new Quadtree();
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
        this.dataSourceInstance = dataSource;
        // this.tileZoom = tileZoom;
        this.logTag = getClass().getSimpleName() + " " + dataSource.getName();
        this.fetchingThreadPool = fetchingThreadPool;
        minReloadInterval = this.dataSourceInstance.getParent()
                .getMinReloadInterval();
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
        synchronized (mEntityIndex) {
            synchronized (mQueryIndex) {
                @SuppressWarnings("unchecked")
                List<DataTile> dataTiles = mQueryIndex.queryAll();
                for (DataTile dataTile : dataTiles) {
                    dataTile.abort(DataSourceErrorType.CANCELED);
                }
                mQueryIndex = new Quadtree();
            }

            mEntityIndex = new Quadtree();
        }
    }

    private Cancelable getDataByEnvelope(Envelope envelope,
            GetDataCallback callback, boolean forceUpdate) {
        DataTile containingDataTile = null;
        synchronized (mQueryIndex) {
            @SuppressWarnings("unchecked")
            List<DataTile> queryResult = mQueryIndex.query(envelope);

            for (DataTile dataTile : queryResult) {
                if (dataTile.tileEnvelope.contains(envelope)) {
                    containingDataTile = dataTile;
                    break;
                }
            }

            if (containingDataTile == null) {
                containingDataTile = new DataTile(envelope);
                mQueryIndex.insert(envelope, containingDataTile);
            }
        }

        return containingDataTile.getData(envelope, callback, forceUpdate);
    }

    private void addData(final Envelope envelope,
            List<? extends SpatialEntity2<? extends Geometry>> data) {
        synchronized (mEntityIndex) {
            final List<SpatialEntity2<? extends Geometry>> entitiesToReplace = new ArrayList<SpatialEntity2<? extends Geometry>>();
            mEntityIndex.query(envelope, new ItemVisitor() {
                @Override
                public void visitItem(Object item) {
                    SpatialEntity2<? extends Geometry> entity = (SpatialEntity2<? extends Geometry>) item;
                    if (envelope.contains(entity.getLongitude(),
                            entity.getLatitude())) {
                        // if
                        // (envelope.intersects(entity.getGeometry().getEnvelopeInternal()))
                        // {
                        entitiesToReplace.add(entity);
                    }
                }
            });

            for (SpatialEntity2<? extends Geometry> entity : entitiesToReplace) {
                // Simply remove features of overlapping regions
                mEntityIndex.remove(envelope, entity);
            }

            for (SpatialEntity2<? extends Geometry> entity : data) {
                mEntityIndex.insert(entity.getGeometry().getEnvelopeInternal(),
                        entity);
                // mEntityIndex.insert(
                // new Envelope(new Coordinate(entity.getLongitude(),
                // entity.getLatitude())), entity);
            }
        }

    }

    private void removeUnusedTiles() {
        // TODO
        synchronized (mQueryIndex) {
            LOG.debug("Removing unused Tiles");
            if (mQueryIndex.size() <= MAX_TILES_TO_REMOVE * 2) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<DataTile> dataTiles = mQueryIndex.queryAll();
            Collections.sort(dataTiles, new Comparator<DataTile>() {
                @Override
                public int compare(DataTile lhs, DataTile rhs) {
                    if (lhs.lastUsage == rhs.lastUsage) {
                        return 0;
                    } else {
                        return lhs.lastUsage < rhs.lastUsage ? -1 : 1;
                    }
                }
            });

            for (int i = 0, len = Math.min(dataTiles.size(),
                    MAX_TILES_TO_REMOVE); i < len; i++) {
                removeTile(dataTiles.get(i));
            }
        }

    }

    /**
     * Removes all cached data for the {@link Envelope} of the specified
     * {@link DataTile}. Also updates the query index.
     * 
     * @param tile
     */
    private void removeTile(final DataTile tile) {
        synchronized (mQueryIndex) {
            final List<DataTile> dataTilesToReload = new ArrayList<DataTile>();
            mQueryIndex.query(tile.tileEnvelope, new ItemVisitor() {
                @Override
                public void visitItem(Object item) {
                    DataTile visitedTile = (DataTile) item;
                    if (tile.tileEnvelope.intersects(visitedTile.tileEnvelope)) {
                        dataTilesToReload.add(visitedTile);
                    }
                }
            });

            for (DataTile tileToReload : dataTilesToReload) {
                mQueryIndex.remove(tileToReload.tileEnvelope, tileToReload);
            }
        }
        synchronized (mEntityIndex) {
            final List<SpatialEntity2<? extends Geometry>> resultList = new ArrayList<SpatialEntity2<? extends Geometry>>();
            mEntityIndex.query(tile.tileEnvelope, new ItemVisitor() {
                @Override
                public void visitItem(Object item) {
                    SpatialEntity2<? extends Geometry> entity = (SpatialEntity2<? extends Geometry>) item;
                    if (tile.tileEnvelope.contains(entity.getLongitude(),
                            entity.getLatitude())) {
                        resultList.add(entity);
                    }
                }
            });

            for (SpatialEntity2<? extends Geometry> entity : resultList) {
                mEntityIndex.remove(tile.tileEnvelope, entity);
            }
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
    public Cancelable getDataByTile(Tile tile, GetDataCallback callback,
            boolean forceUpdate) {
        return getDataByEnvelope(tile.getEnvelope(), callback, forceUpdate);
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
    public Cancelable getDataByBBox(final MercatorRect bounds,
            final GetDataBoundsCallback callback, final boolean forceUpdate) {

        byte tileZoom = (byte) Math.max(0, bounds.zoom);
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
        // Bitset to monitor loading of all data for all required tiles
        final BitSet tileMonitorSet = new BitSet(tileCount);
        tileMonitorSet.set(0, tileCount);

        // Callback for data of a tile
        final AtomicBoolean active = new AtomicBoolean(true);
        final AtomicInteger progress = new AtomicInteger();
        final List<SpatialEntity2<? extends Geometry>> measurementsList = new ArrayList<SpatialEntity2<? extends Geometry>>();

        class IndexedGetDataCallback implements GetDataCallback {
            private int x, y;

            private IndexedGetDataCallback(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public void onReceiveMeasurements(
                    List<? extends SpatialEntity2<? extends Geometry>> data) {

                if (!active.get()) {
                    return;
                }

                int checkIndex = ((y - tileTopY) * tileGridWidth)
                        + (x - tileLeftX);

                if (tileMonitorSet.get(checkIndex)) {
                    // Still waiting for that tile
                    tileMonitorSet.clear(checkIndex);
                    progress.incrementAndGet();
                    if (data != null) {
                        for (SpatialEntity2<? extends Geometry> entity : data)
                            if (!measurementsList.contains(entity))
                                measurementsList.add(entity);
//                        measurementsList.addAll(data);
                    }
                    callback.onProgressUpdate(progress.get(), tileCount);
                    LOG.debug("Loaded Tile " + x + "," + y);

                }
                if (tileMonitorSet.isEmpty()) {
                    // All tiles loaded
                    LOG.debug("Loaded all Tiles");
                    // new Thread(new Runnable() {
                    // public void run() {
                    if (active.get()) {
                        callback.onReceiveDataUpdate(bounds, measurementsList);
                    }
                    // }
                    // }).run();
                }
            }

            public void onAbort(DataSourceErrorType reason) {
                callback.onAbort(bounds, reason);
                if (reason == DataSourceErrorType.CANCELED) {
                    active.set(false);
                }
            }

        }
        callback.onProgressUpdate(0, tileCount);

        // Actually request data

        LOG.debug("Loading " + tileCount + " Tiles");
        final List<Cancelable> cancelableList = new ArrayList<DataCache.Cancelable>();
        for (int y = tileTopY; y <= tileBottomY; y++)
            for (int x = tileLeftX; x <= tileRightX; x++) {
                Tile tile = new Tile(x, y, tileZoom);

                cancelableList
                        .add(getDataByTile(tile, new IndexedGetDataCallback(
                                tile.x, tile.y), forceUpdate));

            }

        return new Cancelable() {
            public void cancel() {
                // FIXME deadlock!
                for (Cancelable cancelable : cancelableList) {
                    cancelable.cancel();
                }
            }
        };
    }
}
