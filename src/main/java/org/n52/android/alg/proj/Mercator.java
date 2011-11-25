/**
 * Copyright 2011 52°North Initiative for Geospatial Open Source Software GmbH
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
 * 
 */

package org.n52.android.alg.proj;

/**
 * @author Daniel Nüst
 * 
 */
public class Mercator {

	public static int transformPixel(int x, byte zoom, byte dstZoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static int longitudeToPixelX(double longitude, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static int latitudeToPixelY(double latitude, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static float calculateGroundResolution(double latitude, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static int pixelXToTileX(int transformPixel, byte tileZoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static int pixelYToTileY(int transformPixel, byte tileZoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double pixelYToLatitude(int top, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static double pixelXToLongitude(int left, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static float tileYToLatitude(int y, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static float tileXToLongitude(int x, byte zoom) {
		// TODO Auto-generated method stub
		return 0;
	}

}
