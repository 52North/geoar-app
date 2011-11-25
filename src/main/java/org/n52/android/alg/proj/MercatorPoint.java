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

import android.graphics.Point;

/**
 * Extends a {@link Point} to also reflect a zoom property
 * 
 * @author Holger Hopmann
 * 
 */
public class MercatorPoint extends Point {
	public byte zoom;

	public MercatorPoint(int x, int y, byte zoom) {
		super(x, y);
		this.zoom = zoom;
	}

	/**
	 * Gets euclidean distance
	 * 
	 * @param p
	 * @return
	 */
	public double distanceTo(MercatorPoint p) {
		if (zoom != p.zoom) {
			p = p.transform(zoom);
		}

		return Math.sqrt(Math.pow(x - p.x, 2) + Math.pow(y - p.y, 2));
	}

	/**
	 * Transforms a point to be expressed in another {@link Mercator} zoom level
	 * 
	 * @param dstZoom
	 * @return
	 */
	public MercatorPoint transform(byte dstZoom) {
		return new MercatorPoint(
				(int) Mercator.transformPixel(x, zoom, dstZoom),
				(int) Mercator.transformPixel(y, zoom, dstZoom), dstZoom);
	}
}