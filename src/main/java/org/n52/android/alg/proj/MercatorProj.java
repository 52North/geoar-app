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
 * @author Arne de Wall
 * 
 */

public class MercatorProj {

	private static final int TILE = 256;

	private static final float EARTH_RADIUS = 6378137;
	private static final float EARTH_CIRCUMFERENCE = 40075016.685578f;

	private static int getMapSize(byte zoom){
		return (int) TILE << zoom; // 256 * 2 ^ zoomlevel
	}
	
	public static double getGroundResolution(double lat, byte zoom){
		return Math.cos(lat * Math.PI / 180) * EARTH_CIRCUMFERENCE / getMapSize(zoom);
	}
	
	public static double transformLonToPixelX(double lon, byte zoom){
		return ((lon + 180)/360) * getMapSize(zoom) + 0.5;
	}
	
	public static double transformTileXToLon(long tileX, byte zoom) {
		return transformPixelXToLon(tileX * TILE, zoom);
	}

	public static double transformTileYToLat(long tileY, byte zoom) {
		return transformPixelYToLat(tileY * TILE, zoom);
	}
	
	public static double transformLatToPixelY(double lat, byte zoom){
		double sinLat = Math.sin(lat * (Math.PI / 180));
		return ((0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI)) * getMapSize(zoom));
	}
	
	public static double transformPixelXToLon(float x, byte zoom){
		double size = getMapSize(zoom);
		return ((Math.min(Math.max(x, 0), size-1) / size) - 0.5) * 360;
	}
	
	public static double transformPixelYToLat(float y, byte zoom){
		double size = getMapSize(zoom);
		return (90-360*Math.atan(Math.exp(
				-(0.5-(Math.min(Math.max(y, 0), size-1)/size))
				*2*Math.PI)) / Math.PI);
	}

	public static int transformPixelXToTileX(double pixelX, byte zoom) {
		return (int) pixelX / TILE;
	}

	public static int transformPixelYToTileY(double pixelY, byte zoom) {
		return (int) pixelY / TILE;
	}

	public static long transformPixel(int pixel, byte inZoom, byte outZoom) {
		return (long) (pixel * Math.pow(2, outZoom - inZoom));
	}
}
