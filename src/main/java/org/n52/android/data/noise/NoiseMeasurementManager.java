package org.n52.android.data.noise;

import java.lang.ref.SoftReference;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.n52.android.alg.Interpolation;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.DataSource;
import org.n52.android.data.DataSource.RequestException;
import org.n52.android.data.Measurement;
import org.n52.android.data.MeasurementFilter;
import org.n52.android.data.MeasurementManager;
import org.n52.android.data.Tile;

import android.util.Log;

public class NoiseMeasurementManager extends MeasurementManager {

	
	public NoiseMeasurementManager(DataSource dataSource) {
		super(dataSource);
		this.dataSource = dataSource;
		tileZoom = dataSource.getPreferredRequestZoom();
		measurementFilter = new NoiseMeasurementFilter();
		downloadThreadPool = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(3);
	}


	public void setMeasurementFilter(MeasurementFilter measurementFilter) {
		clearCache();
		this.measurementFilter = measurementFilter;
	}

	public MeasurementFilter getMeasurementFilter() {
		return measurementFilter;
	}

	/**
	 * Cancels all operations on current measurements and clears the cache
	 */
	protected void clearCache() {
		synchronized (tileCacheMapping) {
			for (SoftReference<MeasurementTile> cacheReference : tileCacheMapping
					.values()) {
				if (cacheReference.get() != null) {
					cacheReference.get().abort(ABORT_CANCELED);
				}
			}
			tileCacheMapping.clear();
		}
	}

	public RequestHolder getMeasurementsByTile(Tile tile,
			GetMeasurementsCallback callback, boolean forceUpdate) {
		Log.i("Test", "getMeasures " + tile.x + ", " + tile.y);

		RequestHolder requestHolder = null;

		synchronized (tileCacheMapping) {
			SoftReference<MeasurementTile> cacheReference = tileCacheMapping
					.get(tile);
			MeasurementTile tileCache = cacheReference != null ? cacheReference
					.get() : null;
			if (tileCache != null) {
				// Tile bereits vorhanden
				if (tileCache.hasMeasurements()) {
					callback.onReceiveMeasurements(tileCache);
				}
				if (!tileCache.hasMeasurements()
						|| tileCache.lastUpdate <= System.currentTimeMillis()
								- dataSource.getDataReloadMinInterval()
						|| forceUpdate) {
					tileCache.addCallback(callback);
					requestHolder = fetchMeasurements(tileCache);
				}
			} else {
				tileCache = new MeasurementTile(tile);
				tileCache.addCallback(callback);
				tileCacheMapping.put(tile, new SoftReference<MeasurementTile>(
						tileCache));

				requestHolder = fetchMeasurements(tileCache);
			}
		}

		return requestHolder;
	}

	private RequestHolder fetchMeasurements(final MeasurementTile tileCache) {

		if (!tileCache.updatePending) {

			tileCache.updatePending = true;
			tileCache.fetchRunnable = new Runnable() {
				public void run() {
					try {
						tileCache.setMeasurements(dataSource.getMeasurements(
								tileCache.tile, measurementFilter));
					} catch (RequestException e) {
						Log.i("NoiseAR", "RequestException" + e.getMessage());
						tileCache.abort(ABORT_UNKOWN);
					} catch (ConnectException e) {
						tileCache.abort(ABORT_NO_CONNECTION);
					}
				}
			};
			downloadThreadPool.execute(tileCache.fetchRunnable);
		}

		return new RequestHolder() {
			public void cancel() {
				downloadThreadPool.remove(tileCache.fetchRunnable);
			}
		};
	}

	// ************ Interpolation ************



	@Override
	public RequestHolder getMeasurementCallback(final MercatorRect bounds,
			final GetMeasurementBoundsCallback callback, boolean forceUpdate,
			MeasurementsCallback dataCallback) {
		// Kachelgrenzen
		final int tileLeftX = (int) MercatorProj.transformPixelXToTileX(
				MercatorProj.transformPixel(bounds.left, bounds.zoom, tileZoom),
				tileZoom);
		final int tileTopY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.top, bounds.zoom, tileZoom),
				tileZoom);
		int tileRightX = (int) MercatorProj.transformPixelXToTileX(
				MercatorProj.transformPixel(bounds.right, bounds.zoom, tileZoom),
				tileZoom);
		int tileBottomY = (int) MercatorProj.transformPixelYToTileY(
				MercatorProj.transformPixel(bounds.bottom, bounds.zoom, tileZoom),
				tileZoom);
		final int tileGridWidth = tileRightX - tileLeftX + 1;

		// Liste zum Nachverfolgen der erhaltenen Messungen
		final Vector<List<Measurement>> tileMeasurementsList = new Vector<List<Measurement>>();
		tileMeasurementsList.setSize(tileGridWidth
				* (tileBottomY - tileTopY + 1));

		// Callback für die Messungen
		final GetMeasurementsCallback measureCallback = new GetMeasurementsCallback() {
			boolean active = true;
			private int progress;
			
			
			public void onReceiveMeasurements(MeasurementTile measurements) {
				
				final MeasurementsCallback res = new MeasurementsCallback();
				
				if (!active) {
					return;
				}

				int checkIndex = ((measurements.tile.y - tileTopY) * tileGridWidth)
						+ (measurements.tile.x - tileLeftX);

				if (tileMeasurementsList.set(checkIndex,
						measurements.measurements) == null) {
					progress++;
					callback.onProgressUpdate(progress,
							tileMeasurementsList.size(), STEP_REQUEST);
				}
				if (!tileMeasurementsList.contains(null)) {
					final List<Measurement> measurementsList = new ArrayList<Measurement>();
					for (List<Measurement> tileMeasurements : tileMeasurementsList) {
						measurementsList.addAll(tileMeasurements);
					}
					
					res.measurementBuffer = measurementsList;
					
					new Thread(new Runnable() {
						public void run() {
							if (active) {
								
								res.interpolationBuffer = Interpolation
										.interpolate(measurementsList, bounds,
												res.interpolationBuffer, callback);
								callback.onReceiveDataUpdate(bounds,
										res);
							}
						}
					}).run();
				}
			}

			public void onAbort(MeasurementTile measurements, int reason) {
				callback.onAbort(bounds, reason);
				if (reason == ABORT_CANCELED) {
					active = false;
				}
			}
		};

		// Messungen besorgen
		for (int y = tileTopY; y <= tileBottomY; y++)
			for (int x = tileLeftX; x <= tileRightX; x++) {
				Tile tile = new Tile(x, y, tileZoom);
				
				getMeasurementsByTile(tile, measureCallback, forceUpdate);
			}

		return new RequestHolder() {
			public void cancel() {
				measureCallback.onAbort(null, ABORT_CANCELED);
			}
		};
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource selectedSource) {
		clearCache();
		tileZoom = dataSource.getPreferredRequestZoom();
		dataSource = selectedSource;
	}








}
