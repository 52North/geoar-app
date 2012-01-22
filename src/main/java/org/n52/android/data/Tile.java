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
package org.n52.android.data;

import org.n52.android.alg.proj.MercatorProj;

import android.graphics.RectF;

/**
 * A tile in the spatial index, based on {@link MercatorProj}
 * 
 * @author Holger Hopmann
 * 
 */
public class Tile {

	public int x;
	public int y;
	public byte zoom;

	public Tile(int x, int y, byte zoom) {
		this.x = x;
		this.y = y;
		this.zoom = zoom;
	}

	@Override
	public int hashCode() {
		return x ^ y ^ zoom;
	}

	/**
	 * Gets Lon Lat Bounding Box from that tile
	 * 
	 * @return
	 */
	public RectF getLLBBox() {
		RectF bb = new RectF();
		bb.top = (float) MercatorProj.transformTileYToLat(y, zoom);
		bb.bottom = (float) MercatorProj.transformTileYToLat(y + 1, zoom);
		bb.left = (float) MercatorProj.transformTileXToLon(x, zoom);
		bb.right = (float) MercatorProj.transformTileXToLon(x + 1, zoom);
		return bb;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Tile) {
			Tile another = (Tile) o;
			return x == another.x && y == another.y && zoom == another.zoom;
		} else {
			return false;
		}
	}

}
