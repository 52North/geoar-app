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
package org.n52.android.view.geoar;

import android.content.Context;

public class ARViewController {

	public interface IRenderSettings {

		// public void
	}

	public interface IRenderable {
		// renders the current Frame
		public void onDrawFrame();

		public void onCreateSurface();

		public void unloadResources();
	}

	// private GetMeasurementBoundsCallback callback = new
	// GetMeasurementBoundsCallback() {
	//
	// @Override
	// public void onProgressUpdate(int progress, int maxProgress, int step) {
	// if (infoHandler != null) {
	// String stepTitle = "";
	// switch (step) {
	// case Interpolation.STEP_CLUSTERING:
	// stepTitle = context.getString(R.string.clustering);
	// break;
	// case Interpolation.STEP_INTERPOLATION:
	// stepTitle = context.getString(R.string.interpolation);
	// break;
	// case MeasurementManager.STEP_REQUEST:
	// stepTitle = context.getString(R.string.measurement_request);
	// break;
	// }
	// infoHandler.setProgressTitle(stepTitle, AugmentedRenderer.this);
	// infoHandler.setProgress(progress, maxProgress,
	// AugmentedRenderer.this);
	// }
	//
	// }
	//
	// @Override
	// public void onReceiveDataUpdate(MercatorRect bounds,
	// MeasurementsCallback measureCallback) {
	// // Save result reference in variable. Those should always be the same
	// currentInterpolationRect = bounds;
	// currentMeasurement = measureCallback;
	//
	// List<POI> newPOIs = new ArrayList<POI>();
	// for(Measurement m : measureCallback.measurementBuffer){
	// newPOIs.add(new POI(m));
	// }
	// poiList = newPOIs;
	// // poiRenderer.setMeasurementsAsPOIs(measureCallback.measurementBuffer);
	//
	// // TODO needed for Interpolation
	// // glInterpolation.setWidth(bounds.width());
	// // glInterpolation.setHeight(bounds.height());
	// // Ask the corresponding texture to reload its data on next draw
	// // interpolationTexture.reload();
	// }
	//
	// @Override
	// public void onAbort(MercatorRect bounds, int reason) {
	// if (infoHandler != null) {
	// infoHandler.clearProgress(AugmentedRenderer.this);
	// if ( reason == MeasurementManager.ABORT_NO_CONNECTION) {
	// infoHandler.setStatus(R.string.connection_error, 5000,
	// AugmentedRenderer.this);
	// } else if(reason == MeasurementManager.ABORT_UNKOWN){
	// infoHandler.setStatus(R.string.unkown_error, 5000,
	// AugmentedRenderer.this);
	// }
	// }
	// }
	// };
	//
	public ARViewController(Context context) {

	}

	public void addRenderer(IRenderable r) {

	}


}
