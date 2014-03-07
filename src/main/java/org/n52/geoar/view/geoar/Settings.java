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
package org.n52.geoar.view.geoar;

/**
 * Class holding all relevant settings for the interpolation and behavior of the
 * application
 * 
 * @author Holger Hopmann
 */
public class Settings {

	// AR view settings
	// Radius in meters for the interpolation in AR view
	public static final int SIZE_AR_INTERPOLATION = 1500;
	// Threshold in position to request new interpolation
	public static final int RELOAD_DIST_AR = 50;
	// Zoom used in AR view for interpolation, please make sure it fits somehow
	// with SIZE_AR_INTERPOLATION...
	public static final byte ZOOM_AR = 12;

	// Buffer in px to request in advance around map extent
	public static final int BUFFER_MAPINTERPOLATION = 100;

}
