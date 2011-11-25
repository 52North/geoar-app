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
package org.n52.android.alg;

import android.graphics.Point;

/**
 * 
 * Bresenham Algorithmus basierend auf Beispielen aus
 * http://de.wikipedia.org/wiki/Bresenham-Algorithmus
 * 
 * Die Rasterisierung erfolgt hierbei nicht als ganzes, sondern muss
 * schrittweise durchgef�hrt werden, was es erlaubt einzelne Abschnitte einer
 * Linie zu rasterisieren
 * 
 * @author Holger Hopmann
 * 
 */
public final class Bresenham {
	private int dx, dy, err, sx, sy;
	int x, y;
	private boolean init = false;

	public int prepareLine(Point p0, Point p1) {
		return prepareLine(p0.x, p0.y, p1.x, p1.y);
	}

	public int prepareLine(int x0, int y0, int x1, int y1) {
		this.x = x0;
		this.y = y0;

		// Entfernung in beiden Dimensionen berechnen
		dx = Math.abs(x1 - x0);
		dy = Math.abs(y1 - y0);

		sx = x1 >= x0 ? 1 : -1;
		sy = y1 >= y0 ? 1 : -1;

		err = (dx > 0) ? dx / 2 : dy / 2;

		init = true;
		return Math.max(dx, dy) + 1;
	}

	public void moveNext() {
		if (init) {
			init = false;
		} else {

			// feststellen, welche Entfernung gr��er ist
			if (dx > dy) {
				// x ist schnelle Richtung
				err += dy;

				// Fehler
				if (err >= dx) {
					err -= dx;
					y += sy;
				}
				x += sx;
			} else {
				// y ist schnelle Richtung
				err += dx;

				// Fehler
				if (err >= dy) {
					err -= dy;
					x += sx;
				}

				y += sy;
			}
		}
	}

}