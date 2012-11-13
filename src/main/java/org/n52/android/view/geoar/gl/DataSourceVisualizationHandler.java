package org.n52.android.view.geoar.gl;

import java.util.ArrayList;
import java.util.List;

import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.newdata.DataCache.GetDataBoundsCallback;
import org.n52.android.newdata.DataCache.RequestHolder;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.SpatialEntity;
import org.n52.android.newdata.Visualization.ARVisualization.ItemVisualization;
import org.n52.android.view.InfoView;
import org.n52.android.view.geoar.Settings;
import org.n52.android.view.geoar.gl.model.RenderNode;
import org.osmdroid.util.GeoPoint;

import android.opengl.GLSurfaceView;

public class DataSourceVisualizationHandler {

	public interface OnProgressUpdateListener extends
			org.n52.android.newdata.DataCache.OnProgressUpdateListener {
		void onAbort(int reason);
	}

	/**
	 * An instance of this class describes a measurement request order which
	 * will be performed in the future, just to make sure that the user will
	 * stop navigating through the map. It ensures that it wont alter this
	 * overlays data if it got canceled before
	 * 
	 */
	protected class UpdateHolder implements Runnable {
		protected boolean canceled;
		protected MercatorRect bounds;
		protected RequestHolder requestHolder;

		protected GetDataBoundsCallback callback = new GetDataBoundsCallback() {

			@Override
			public void onProgressUpdate(int progress, int maxProgress, int step) {
				if (mInfoHandler != null) {
					String stepTitle = "";
					// switch (step) {
					// // case NoiseInterpolation.STEP_CLUSTERING:
					// // stepTitle = mContext.getString(R.string.clustering);
					// // break;
					// // case NoiseInterpolation.STEP_INTERPOLATION:
					// // stepTitle =
					// mContext.getString(R.string.interpolation);
					// // break;
					// case DataCache.STEP_REQUEST:
					// stepTitle = mContext
					// .getString(R.string.measurement_request);
					// break;
					// }

					mInfoHandler.setProgressTitle(stepTitle, UpdateHolder.this);
					mInfoHandler.setProgress(progress, maxProgress,
							UpdateHolder.this);
				}
			}

			@Override
			public void onAbort(MercatorRect bounds, int reason) {
				if (mInfoHandler != null) {
					// mInfoHandler.clearProgress(ARSurfaceViewRenderer.this);
					// if (reason == DataCache.ABORT_NO_CONNECTION) {
					// mInfoHandler.setStatus(R.string.connection_error, 5000,
					// ARSurfaceViewRenderer.this);
					// } else if (reason == DataCache.ABORT_UNKOWN) {
					// mInfoHandler.setStatus(R.string.unkown_error, 5000,
					// ARSurfaceViewRenderer.this);
					// }
				}
			}

			@Override
			public void onReceiveDataUpdate(MercatorRect bounds,
					List<? extends SpatialEntity> data) {
				synchronized (mutex) {
					if (!canceled) {
						List<RenderNode> renderNodes = new ArrayList<RenderNode>();
						List<ItemVisualization> visualizations = dataSourceHolder
								.getVisualizations().getCheckedItems(
										ItemVisualization.class);

						for (SpatialEntity entity : data) {
							for (ItemVisualization visualization : visualizations) {
								RenderNode renderNode = (RenderNode) visualization
										.getEntityVisualization(entity,
												visFactory);
								renderNode.setEntity(entity);
								renderNodes.add(renderNode);
							}
						}
					}
				}
			}

		};

		public UpdateHolder(MercatorRect bounds) {
			this.bounds = bounds;
		}

		public void cancel() {
			synchronized (mutex) {
				if (requestHolder != null) {
					requestHolder.cancel();
				}
				canceled = true;
			}
		}

		@Override
		public void run() {
			synchronized (mutex) {
				if (!canceled) {
					requestHolder = dataSourceHolder.getDataCache().getDataByBBox(
							bounds, callback, false);
				}
			}
		}

	}

	protected UpdateHolder currentUpdate;
	protected UpdateHolder nextUpdate;

	private DataSourceHolder dataSourceHolder;
	private InfoView mInfoHandler;
	protected Object mutex = new Object();
	protected ARVisualizationFactory visFactory;

	protected final GLSurfaceView glSurfaceView;

	public List<RenderNode> renderNodes = new ArrayList<RenderNode>();

	public DataSourceVisualizationHandler(final GLSurfaceView glSurfaceView,
			DataSourceHolder dataSource,
			ARVisualizationFactory visualizationFactory) {
		this.glSurfaceView = glSurfaceView;
		this.visFactory = visualizationFactory;
		this.dataSourceHolder = dataSource;
	}

	public void clear() {
		synchronized (mutex) {
			cancel();
			renderNodes.clear();
		}
	}

	public void cancel() {
		synchronized (mutex) {
			if (currentUpdate != null) {
				currentUpdate.cancel();
			}
		}
	}

	public void setCenter(GeoPoint gPoint) {

//		currentCenterGPoint = gPoint;
//
//		// Calculate thresholds for request of data
//		double meterPerPixel = MercatorProj.getGroundResolution(
//				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
//		int pixelRadius = (int) (Settings.SIZE_AR_INTERPOLATION / meterPerPixel);
//		int pixelReloadDist = (int) (Settings.RELOAD_DIST_AR / meterPerPixel);
//
//		// Calculate new center point in world coordinates
//		int centerPixelX = (int) MercatorProj.transformLonToPixelX(
//				gPoint.getLongitudeE6() / 1E6f, Settings.ZOOM_AR);
//		int centerPixelY = (int) MercatorProj.transformLatToPixelY(
//				gPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
//		currentCenterMercator = new MercatorPoint(centerPixelX, centerPixelY,
//				Settings.ZOOM_AR);
//
//		currentGroundResolution = (float) MercatorProj.getGroundResolution(
//				currentCenterGPoint.getLatitudeE6() / 1E6f, Settings.ZOOM_AR);
//
//		// determination if data request is needed or if just a simple shift is
//		// enough
//		boolean requestInterpolation = false;
//		if (currentInterpolationRect == null) {
//			// Get new data if there were none before
//			requestInterpolation = true;
//		} else {
//			MercatorPoint interpolationCenter = currentInterpolationRect
//					.getCenter();
//			if (currentCenterMercator.zoom != currentInterpolationRect.zoom
//					|| currentCenterMercator.distanceTo(interpolationCenter) > pixelReloadDist) {
//				// request data if new center offsets more than
//				// Settings.RELOAD_DIST_AR meters
//				requestInterpolation = true;
//			}
//		}
//
//		if (requestInterpolation) {
//			// if new data is needed
//			if (currentUpdate != null) {
//				// cancel currently running data request
//				currentUpdate.cancel();
//			}
//			// trigger data request
//			currentUpdate = dataSourceHolder.getDataCache().getDataByBBox(
//					new MercatorRect(currentCenterMercator.x - pixelRadius,
//							currentCenterMercator.y - pixelRadius,
//							currentCenterMercator.x + pixelRadius,
//							currentCenterMercator.y + pixelRadius,
//							Settings.ZOOM_AR), callback, false);
//		}
//
//		// for (GeoLocationUpdateListener r : geoLocationUpdateListener) {
//		// r.onGeoLocationUpdate(currentCenterGPoint);
//		// }

	}

	public DataSourceHolder getDataSourceHolder() {
		return dataSourceHolder;
	}
}
