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

import java.util.Arrays;

import android.graphics.PointF;

/**
 * Process for finding average noise per slice. See 5.3.2
 * 
 * @author Holger Hopmann
 */
public class NoiseView {

	/**
	 * Interface for classes which can serve with data for a "noise view"
	 * 
	 */
	public interface NoiseGridValueProvider {

		public final static byte NO_DATA = 0;

		byte getNoiseValue(int x, int y);

		void addOnNoiseViewChangedListener(NoiseViewChangedListener listener);

		void removeOnNoiseViewChangedListener(NoiseViewChangedListener listener);

	}

	/**
	 * Interface for a simple listener for update noise view bounds
	 * 
	 */
	public interface NoiseViewChangedListener {
		void onNoiseViewChanged(PointF bottomLeft, PointF bottomRight,
				PointF topLeft, PointF topRight, PointF viewerPos);
	}

	/**
	 * Private helper class to hold the x-spread of a scanline
	 */
	private class Scanline {
		private int startX = -1, endX = -1;

		/**
		 * Incorporates a x value in the scanline's boundary
		 * 
		 * @param x
		 *            x coordinate to set as new extreme, if applicable
		 */
		public void setBoundary(int x) {
			if (startX == -1 || endX == -1) {
				startX = x;
				endX = x;
			} else {
				if (x < startX) {
					startX = x;
				} else if (x > endX) {
					endX = x;
				}
			}
		}
	}

	private NoiseGridValueProvider valueProvider;
	private float[] resValues;

	/**
	 * Constructor Creates a new object capable of averaging noise values by
	 * slices in a given quad
	 * 
	 * @param valueProvider
	 * @param slices
	 */
	public NoiseView(NoiseGridValueProvider valueProvider, int slices) {
		this.valueProvider = valueProvider;
		resValues = new float[slices];
	}

	/**
	 * Computes the averages of noise in slices of the given quad
	 * 
	 * @param bottomLeft
	 * @param bottomRight
	 * @param topLeft
	 * @param topRight
	 * @return Array of float containing averaged values, from left (0) to right
	 *         (slices-1)
	 */
	public float[] getVisibleNoise(PointF bottomLeft, PointF bottomRight,
			PointF topLeft, PointF topRight) {
		// Lower boundary, base line
		Bresenham bottomLineBresenham = new Bresenham();
		int bottomLineLength = bottomLineBresenham.prepareLine(
				(int) bottomLeft.x, (int) bottomLeft.y, (int) bottomRight.x,
				(int) bottomRight.y);
		// top boundary
		Bresenham topLineBresenham = new Bresenham();
		int topLineLength = topLineBresenham.prepareLine((int) topLeft.x,
				(int) topLeft.y, (int) topRight.x, (int) topRight.y);

		// computes distances and error terms, since bresenham is not perfomed
		// all at once so that rounding errors arise
		int slices = resValues.length;
		float stepTopDiff;
		float stepBottomError = 0, stepTopError = 0;
		int stepBottom = bottomLineLength / slices;
		float stepBottomDiff = stepBottom - (bottomLineLength / (float) slices);
		int stepTop = topLineLength / slices;
		stepTopDiff = stepTop - (topLineLength / (float) slices);

		// Scanlinecache
		int minY = (int) Math.floor(Math.min(bottomLeft.y,
				Math.min(bottomRight.y, Math.min(topLeft.y, topRight.y))));
		int maxY = (int) Math.ceil(Math.max(bottomLeft.y,
				Math.max(bottomRight.y, Math.max(topLeft.y, topRight.y))));
		Scanline[] scanlines = new Scanline[maxY - minY + 1];
		Scanline[] nextScanlines = new Scanline[scanlines.length];

		int refY = minY;

		// Initialization
		Bresenham initColBresenham = new Bresenham();
		int initColLength = initColBresenham.prepareLine(bottomLineBresenham.x,
				bottomLineBresenham.y, topLineBresenham.x, topLineBresenham.y);
		movePoint(initColLength, initColBresenham, scanlines, refY);

		// Search for slices
		for (int col = 0; col < slices; col++) {
			// loop for each slice

			// reset noise accumulator
			int numSoundValues = 0;
			double sumSoundPressure = 0;

			// move lower line / error update
			if (stepBottomError <= -0.5f) {
				movePoint(stepBottom + 1, bottomLineBresenham, scanlines, refY);
				stepBottomError += 1;
			} else {
				movePoint(stepBottom, bottomLineBresenham, scanlines, refY);
			}
			stepBottomError += stepBottomDiff;

			// move upper line / error update
			if (stepTopError <= -0.5f) {
				movePoint(stepTop + 1, topLineBresenham, scanlines, refY);
				stepTopError += 1;
			} else {
				movePoint(stepTop, topLineBresenham, scanlines, refY);
			}
			stepTopError += stepTopDiff;

			// prepare for next iteration: reset scanlines
			Arrays.fill(nextScanlines, null);

			// Determine border line, for current and next iteration together
			Bresenham colBresenham = new Bresenham();
			int colLength = colBresenham.prepareLine(bottomLineBresenham.x,
					bottomLineBresenham.y, topLineBresenham.x,
					topLineBresenham.y);

			movePoint(colLength, colBresenham, nextScanlines, refY);

			// Utilize boundaries for current iteration/slice
			for (int i = 0; i < nextScanlines.length; i++) {
				if (nextScanlines[i] == null) {
					continue;
				}
				if (scanlines[i] != null) {
					// Update boundary if one exists for next slice
					scanlines[i].setBoundary(nextScanlines[i].startX);
				} else {
					// use complete boundary if none exists for current
					// iteration
					scanlines[i] = nextScanlines[i];
				}
			}

			// Scanline algorithm to find noise values
			for (int i = 0; i < scanlines.length; i++) {
				Scanline scanline = scanlines[i];
				if (scanline == null) {
					continue;
				}
				int y = refY + i;
				for (int x = scanline.startX; x <= scanline.endX; x++) {
					byte rawNoise = valueProvider.getNoiseValue(x, y);
					if (rawNoise != NoiseGridValueProvider.NO_DATA) {
						// byte [0,255] to float
						float noise = (rawNoise & 0xFF) / 2f;
						++numSoundValues;
						// sum of pressure, division by 1000 to avoid overflow
						sumSoundPressure += Math.pow(10, noise / 10f) / 1000f;
					}
				}
			}

			if (numSoundValues != 0) {
				// has valid data
				resValues[col] = (float) (10 * Math
						.log10((sumSoundPressure / (float) numSoundValues) * 1000f));
			} else {
				// no data for slice
				resValues[col] = NoiseGridValueProvider.NO_DATA;
			}

			// swap scanline caches, next becomes current
			Scanline[] tempScanlines = scanlines;
			scanlines = nextScanlines;
			nextScanlines = tempScanlines;

		}
		return resValues;
	}

	/**
	 * Moves line by a given amount of cells and updates corresponding scanlines
	 * with new boundary values
	 * 
	 * @param step
	 * @param lineBresenham
	 * @param scanlines
	 * @param refY
	 *            y value as a starting point to find index in scanline array
	 *            corresponding to y coordinate
	 */
	private void movePoint(int step, Bresenham lineBresenham,
			Scanline[] scanlines, int refY) {
		for (int i = 0; i < step; i++) {
			lineBresenham.moveNext();
			int index = lineBresenham.y - refY;
			if (scanlines[index] == null) {
				scanlines[index] = new Scanline();
			}
			scanlines[index].setBoundary(lineBresenham.x);
		}
	}

}
