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

/**
 * Class holding all relevant settings for the interpolation and behavior of the
 * application
 * 
 * @author Holger Hopmann
 */
public class Settings {
	
	public static enum DataSourceSetting{
		NOISE,
		AIRPOLUTION
	}
	
	// AR view settings
	// Radius in meters for the interpolation in AR view
	public static final int SIZE_AR_INTERPOLATION = 150;
	// Threshold in position to request new interpolation
	public static final int RELOAD_DIST_AR = 50;
	// Zoom used in AR view for interpolation
	public static final byte ZOOM_AR = 15;

	// Interpolation settings
	// Min zoom value used in clustering measurements. Accuracy concerns
	public static final byte MIN_ZOOM_CLUSTERING_COORDINATES = 12;
	// Min zoom value used in finding measurement positions. Accuracy concerns
	public static final byte MIN_ZOOM_MEASUREMENT_POSITION = 12;
	// Max radius of a Cluster
	public static final float MAX_CLUSTER_SIZE_METER = 25f;
	// Max number of iterations for kMeans
	public static final int MAX_KMEANS_COUNT = 15;
	// Minimal accuracy required for measurement
	public static final int MIN_MEASUREMENT_LOCATION_ACCURACY = 200;

	// Settings for diagram
	public static final float MAX_NOISE_DIAGRAM = 100;
	public static final float MIN_NOISE_DIAGRAM = 30;
	public static final int NUM_SLICES_DIAGRAM = 20;

	// Settings for the mapview
	// Max zoom to use for interpolation, scales for higher zoom
	public static final byte MAX_ZOOM_MAPINTERPOLATION = 15;
	// Min zoom required to perform interpolation
	public static final byte MIN_ZOOM_MAPINTERPOLATION = 13;
	// Buffer in px to request in advance around map extent
	public static final int BUFFER_MAPINTERPOLATION = 100;

}
