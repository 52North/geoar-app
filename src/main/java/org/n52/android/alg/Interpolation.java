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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.n52.android.alg.NoiseView.NoiseGridValueProvider;
import org.n52.android.alg.proj.Mercator;
import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.alg.proj.MercatorRect;
import org.n52.android.data.Measurement;
import org.n52.android.view.geoar.Settings;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;

/**
 * Class implementing processing of chapter 4
 * 
 * @author Holger Hopmann
 */
public class Interpolation {
	/**
	 * Class representing a single cluster. Central structure within the
	 * algorithms. A cluster has to get updated from outside a is finally
	 * triggered to update its properties. This class also offers means to
	 * detect if convergence occurred during clustering.
	 * 
	 */
	private static class Cluster implements Comparable<Cluster> {
		private MercatorPoint centerLocationTile;
		private List<Measurement> measurements = new ArrayList<Measurement>();
		public float noise;
		private double latitude;
		private byte zoom;

		// Convergence check
		private float lastNoise;
		private int lastCentroidX, lastCentroidY;
		private int lastSize;

		public Cluster(MercatorPoint centerLocationTile, double latitude) {
			this.centerLocationTile = centerLocationTile;
			this.latitude = latitude;
		}

		/**
		 * Updates properties of cluster. Calculates centroid and noise mean.
		 * 
		 * @return true, falls Konvergenz in diesem Cluster eintrat, also weder
		 *         Centroidposition noch Schallwert sich �nderten
		 */
		public boolean update() {
			// For noise average
			double sumSoundPressure = 0;

			// Centroid
			int centroidX = 0, centroidY = 0;
			double centroidLat = 0;

			for (Measurement measurement : measurements) {
				// Noise average
				sumSoundPressure += Math.pow(10, measurement.noise / 10f) / 1000f;

				// Centroid
				Point locationTile = measurement.getLocationTile(zoom);
				centroidX += locationTile.x;
				centroidY += locationTile.y;
				centroidLat += measurement.latitude;
			}

			boolean converged = true;

			// noise average
			noise = (float) (10 * Math
					.log10((sumSoundPressure / (float) measurements.size()) * 1000f));
			if (noise != lastNoise) {
				converged = false;
			}

			// Centroid
			centroidLat = centroidLat / measurements.size();
			centerLocationTile.x = centroidX / measurements.size();
			centerLocationTile.y = centroidY / measurements.size();
			if (centroidX != lastCentroidX || centroidY != lastCentroidY) {
				converged = false;
			}

			if (measurements.size() != lastSize) {
				converged = false;
			}

			// Save data for next convergence check
			lastCentroidX = centroidX;
			lastCentroidY = centroidY;
			lastNoise = noise;
			lastSize = measurements.size();

			return converged;
		}

		/**
		 * Clears all measurements
		 */
		public void clear() {
			measurements.clear();
		}

		public int compareTo(Cluster another) {
			// compares clusters by y coordinate
			return centerLocationTile.y - another.centerLocationTile.y;
		}

	}

	/**
	 * Simple extended {@link MercatorPoint} to store neighborhood relations
	 * 
	 */
	private static class AkkumulatorPoint extends MercatorPoint {

		public boolean hasNeighborLeft, hasNeighborRight, hasNeighborTop,
				hasNeighborBottom;

		public AkkumulatorPoint(int x, int y, byte zoom) {
			super(x, y, zoom);
		}

	}

	public static final int STEP_CLUSTERING = 1;
	public static final int STEP_INTERPOLATION = 2;

	/**
	 * Performs interpolation. returns row major array of extended byte with
	 * noise values according to specified bounds
	 * 
	 * @param measurements
	 *            Measurements to use for interpolation
	 * @param bounds
	 *            Bounds for interpolation
	 * @param resInterpolation
	 *            byte array which fits for the specified bounds to use for
	 *            interpolation, or null if none exists
	 * @param progressUpdateListener
	 *            Listener to receiie progress updates
	 * @return row major array of extended byte with noise values
	 */
	public static byte[] interpolate(List<Measurement> measurements,
			MercatorRect bounds, byte[] resInterpolation,
			OnProgressUpdateListener progressUpdateListener) {

		List<Cluster> clusters = createMeasurementClusters(measurements,
				bounds.zoom, progressUpdateListener);

		int width = bounds.width();
		int height = bounds.height();
		int size = width * height;
		if (resInterpolation == null) {
			resInterpolation = new byte[size];
		}
		Arrays.fill(resInterpolation, 0, size, NoiseGridValueProvider.NO_DATA);

		// Hough transform
		// List<HoughAccumulatorBin> houghLines = randHoughTrans(clusters);
		// testHough(interpolation, houghLines);

		// variables to track progress
		int progress = 0, maxProgress = clusters.size();

		for (Cluster cluster : clusters) {
			// for each cluster
			if (progressUpdateListener != null) {
				progressUpdateListener.onProgressUpdate(++progress,
						maxProgress, STEP_INTERPOLATION);
			}

			// find cells for which this cluster matches
			List<AkkumulatorPoint> noiseLocations = getNoiseLocations(cluster,
					bounds.zoom);

			// Find attenuation for noise value of cluster. attenQuarterSzie
			// holds width and height of resulting attenuation quarter
			int[] attenQuarterSize = new int[2];
			byte[] attenQuarterData = getNoiseAttenQuarter(cluster.noise,
					cluster.noise / 2f, (int) cluster.latitude, bounds.zoom,
					attenQuarterSize);

			// Updating interpolation with attenuation for all points in cluster
			for (AkkumulatorPoint p : noiseLocations) {
				updateAttenuationData(bounds, resInterpolation, width, height,
						p, attenQuarterSize, attenQuarterData);
			}
		}
		return resInterpolation;
	}

	/**
	 * Updates noise values of a raster based on the noise values of an
	 * attenuation quarter
	 * 
	 * Uses a {@link AkkumulatorPoint} to make use of neighborhood relations to
	 * reduce redundant updates
	 * 
	 * @param bounds
	 *            Bounds of raster
	 * @param resInterpolation
	 *            raster
	 * @param width
	 *            width of raster
	 * @param height
	 *            height of raster
	 * @param p
	 *            reference point for noise attenuation quarter
	 * @param attenQuarterSize
	 *            size of attenuation quarter
	 * @param attenQuarterData
	 *            attenuation quarter
	 */
	private static void updateAttenuationData(MercatorRect bounds,
			byte[] resInterpolation, int width, int height, AkkumulatorPoint p,
			int[] attenQuarterSize, byte[] attenQuarterData) {
		int attenQuarterHeight, dataOffset = 0;
		MercatorPoint loc = p.transform(bounds.zoom);

		int x = loc.x - bounds.left;
		int y = loc.y - bounds.top;

		if (p.hasNeighborTop) {
			// If neighbor above just set one row
			attenQuarterHeight = 1;
			dataOffset = (attenQuarterSize[1] - 1) * attenQuarterSize[0];
		} else {
			attenQuarterHeight = attenQuarterSize[1];
			dataOffset = 0;
		}
		if (!p.hasNeighborRight) {
			// 1. quadrant
			setNoiseAttenQuarter(resInterpolation, x, y - attenQuarterHeight,
					width, height, attenQuarterData, dataOffset,
					attenQuarterSize[0], attenQuarterSize[0],
					attenQuarterHeight, false, false);
		}
		if (!p.hasNeighborLeft) {
			// 2. quadrant
			setNoiseAttenQuarter(resInterpolation, x - attenQuarterSize[0] + 1,
					y - attenQuarterHeight, width, height, attenQuarterData,
					dataOffset, attenQuarterSize[0], attenQuarterSize[0],
					attenQuarterHeight, true, false);
		}

		if (p.hasNeighborRight && p.hasNeighborLeft) {
			// column up
			setNoiseAttenQuarter(resInterpolation, x, y - attenQuarterHeight,
					width, height, attenQuarterData, dataOffset,
					attenQuarterSize[0], 1, attenQuarterHeight, false, false);
		}

		if (p.hasNeighborBottom) {
			// If neighbor at bottom just set one row
			attenQuarterHeight = 1;
			dataOffset = (attenQuarterSize[1] - 1) * attenQuarterSize[0];
		} else {
			attenQuarterHeight = attenQuarterSize[1];
			dataOffset = 0;
		}
		if (!p.hasNeighborRight) {
			// 4. quadrant
			setNoiseAttenQuarter(resInterpolation, x, y, width, height,
					attenQuarterData, dataOffset, attenQuarterSize[0],
					attenQuarterSize[0], attenQuarterHeight, false, true);
		}

		if (!p.hasNeighborLeft) {
			// 3. quadrant
			setNoiseAttenQuarter(resInterpolation, x - attenQuarterSize[0] + 1,
					y, width, height, attenQuarterData, dataOffset,
					attenQuarterSize[0], attenQuarterSize[0],
					attenQuarterHeight, true, true);
		}

		if (p.hasNeighborRight && p.hasNeighborLeft) {
			// column down
			setNoiseAttenQuarter(resInterpolation, x, y, width, height,
					attenQuarterData, dataOffset, attenQuarterSize[0], 1,
					attenQuarterHeight, false, true);
		}
	}

	/**
	 * Performs k Means algorithm to find measurement clusters
	 * 
	 * @param measurements
	 * @param minZoom
	 *            Min zoom value to use for calculations. Algorithm can choose a
	 *            higher zoom value
	 * @param progressUpdateListener
	 *            progress updates
	 * @return identifies clusters
	 */
	public static List<Cluster> createMeasurementClusters(
			List<Measurement> measurements, byte minZoom,
			OnProgressUpdateListener progressUpdateListener) {
		byte zoom = (byte) Math.max(Settings.MIN_ZOOM_CLUSTERING_COORDINATES,
				minZoom);

		List<Cluster> clusters = new ArrayList<Cluster>();

		int maxDiffY = (int) Math.ceil(Settings.MAX_CLUSTER_SIZE_METER
				/ Mercator.calculateGroundResolution(0, zoom));

		for (int count = 1; count <= Settings.MAX_KMEANS_COUNT; count++) {
			// number of iterations times

			if (progressUpdateListener != null) {
				progressUpdateListener.onProgressUpdate(count,
						Settings.MAX_KMEANS_COUNT, STEP_CLUSTERING);
			}

			for (Measurement measurement : measurements) {
				// For each measurement
				if (measurement.getAccuracy() > Settings.MIN_MEASUREMENT_LOCATION_ACCURACY) {
					// Ignore measurements with to low accuracy
					continue;
				}

				MercatorPoint measurementLocationTile = measurement
						.getLocationTile(zoom);
				float lonError = (float) Math.cos(measurement.latitude
						* Math.PI / 180);
				int maxDiffX = (int) Math.ceil(maxDiffY * lonError);

				Cluster minDistCluster = null;

				if (clusters.size() != 0) {
					// Find possibly matching clusters if there are already some

					// create dummy points to make use of built in sorting and
					// searching by y component
					Cluster clusterDummyTopLeft = new Cluster(
							new MercatorPoint(
									(int) (measurementLocationTile.x - maxDiffX),
									(int) (measurementLocationTile.y - maxDiffY),
									zoom), measurement.latitude);
					Cluster clusterDummyBottomRight = new Cluster(
							new MercatorPoint(
									(int) (measurementLocationTile.x + maxDiffX),
									(int) (measurementLocationTile.y + maxDiffY),
									zoom), measurement.latitude);

					// Create subList according to y range
					List<Cluster> searchRangeY = getClusterRange(clusters,
							clusterDummyTopLeft, clusterDummyBottomRight, true);

					double minClusterDist = Float.MAX_VALUE;

					for (Cluster candidateCluster : searchRangeY) {
						// Compare distance for all clusters in y range
						float diffX = (candidateCluster.centerLocationTile.x - measurementLocationTile.x);

						if (Math.abs(diffX) <= maxDiffY) {
							diffX *= lonError;
							float diffY = candidateCluster.centerLocationTile.y
									- measurementLocationTile.y;

							double dist = Math.sqrt(diffY * diffY + diffX
									* diffX);
							if (dist < maxDiffY && minClusterDist > dist) {
								// Identify best cluster
								minClusterDist = dist;
								minDistCluster = candidateCluster;
								if (minClusterDist == 0) {
									break;
								}
							}
						}
					}
				}
				if (minDistCluster != null) {
					// Has fitting cluster, add measurement to it
					minDistCluster.measurements.add(measurement);
				} else {
					// create new cluster otherwise with that measurement
					Cluster cluster = new Cluster(measurementLocationTile,
							measurement.latitude);
					cluster.measurements.add(measurement);

					// Insert new cluster into cluster list using binary search.
					// This allows fast search for y range
					int insertIndex = Collections.binarySearch(clusters,
							cluster);
					if (insertIndex < 0) {
						clusters.add(-insertIndex - 1, cluster);
					} else {
						clusters.add(insertIndex, cluster);
					}
				}
			}

			// Update
			Iterator<Cluster> iterator = clusters.iterator();
			boolean lastIteration = count == Settings.MAX_KMEANS_COUNT;
			boolean converged = true;
			while (iterator.hasNext()) {
				Cluster nextCluster = iterator.next();
				if (nextCluster.measurements.size() == 0) {
					// Leere Cluster l�schen
					iterator.remove();
					converged = false;
				} else {
					if (!nextCluster.update()) {
						converged = false;
					}
					if (!lastIteration) {
						// prepare cluster for next step, if one follows
						nextCluster.clear();
					}
				}
			}
			if (converged) {
				// convergence in all clusters -> break of kMeans loop
				if (progressUpdateListener != null) {
					progressUpdateListener.onProgressUpdate(1, 1,
							STEP_CLUSTERING);
				}
				break;
			}

			if (!lastIteration) {
				// resort cluster because of new centroids
				Collections.sort(clusters);
			}
		}
		return clusters;
	}

	/**
	 * Get a sublist of clusters
	 * 
	 * @param clusters
	 * @param minCluster
	 * @param maxCluster
	 * @param asSubList
	 * @return
	 */
	private static List<Cluster> getClusterRange(List<Cluster> clusters,
			Cluster minCluster, Cluster maxCluster, boolean asSubList) {
		int searchStartIndex = Collections.binarySearch(clusters, minCluster);
		int searchEndIndex = Collections.binarySearch(clusters, maxCluster);

		if (searchStartIndex < 0) {
			searchStartIndex = -searchStartIndex - 1;
		}
		if (searchEndIndex < 0) {
			searchEndIndex = -searchEndIndex - 1;
			// searchEndIndexY = Math.min(searchEndIndexY,
			// clusters.size() - 1);
		}
		if (!asSubList) {
			return new ArrayList<Cluster>(clusters.subList(searchStartIndex,
					searchEndIndex));
		} else {
			return clusters.subList(searchStartIndex, searchEndIndex);
		}
	}

	/**
	 * Puts attenuation quarter into noise raster. Based on a quarter of
	 * attenuation data, mirrored from first quadrant.
	 * 
	 */
	public static void setNoiseAttenQuarter(byte interpolation[], int x, int y,
			int width, int height, byte[] attenQuarterData, int dataOffset,
			int dataStride, int attenQuarterWidth, int attenQuarterHeight,
			boolean flipX, boolean flipY) {
		// Find size of actual area, attenuation get be bigger than resulting
		// raster
		int setHeight = Math.min(y + attenQuarterHeight, height);
		int setWidth = Math.min(x + attenQuarterWidth, width);

		if (setHeight < 0 || setWidth < 0) {
			return;
		}

		int offsetWidth = 0, offsetHeight = 0;
		int scaleX = 1, scaleY = 1;
		if (flipX) {
			offsetWidth = attenQuarterWidth - 1;
			scaleX = -1;
		}
		if (flipY) {
			offsetHeight = attenQuarterHeight - 1;
			scaleY = -1;
		}
		for (int i = Math.max(0, y); i < setHeight; i++) {
			for (int j = Math.max(0, x); j < setWidth; j++) {
				int indexDst = (i * width + j);
				int indexSrc = dataOffset + (offsetHeight + scaleY * (i - y))
						* dataStride + (offsetWidth + scaleX * (j - x));

				if ((attenQuarterData[indexSrc] & 0xFF) > (interpolation[indexDst] & 0xFF)) {
					interpolation[indexDst] = attenQuarterData[indexSrc];
				}
			}
		}
	}

	/**
	 * Gets attenuation data for a noise value and the limit to interpolate to
	 * 
	 * @param noise
	 * @param noiseLimit
	 * @param latitude
	 * @param zoom
	 * @param attenQuarterSize
	 * @return
	 */
	public static byte[] getNoiseAttenQuarter(float noise, float noiseLimit,
			int latitude, byte zoom, int[] attenQuarterSize) {
		// ^
		// O ------- |
		// | # # # # |
		// | # # # # |
		// | # # # # |
		// C ------- | >

		float noiseDistMeter = getDistBetweenNoise(noise, noiseLimit);
		int noiseDistPixel = (int) (noiseDistMeter / Mercator
				.calculateGroundResolution(latitude, zoom));

		int attenQuarterWidth = noiseDistPixel + 1;
		attenQuarterSize[0] = attenQuarterSize[1] = attenQuarterWidth;
		float meterPerPixel = noiseDistMeter / noiseDistPixel;

		byte[] attenQuarterData = new byte[attenQuarterWidth
				* attenQuarterWidth];
		Arrays.fill(attenQuarterData, NoiseGridValueProvider.NO_DATA);
		for (int i = 0; i < attenQuarterWidth; i++) {
			for (int j = 0; j < attenQuarterWidth - i; j++) {
				float distCenterPixel = (float) Math.sqrt(Math.pow(
						attenQuarterWidth - i, 2) + Math.pow(j, 2));

				if (distCenterPixel > attenQuarterWidth) {
					continue;
				}

				float distCenterMeter = distCenterPixel * meterPerPixel;

				// Immission in distCenterMeter distance
				// [0, 120] * 2 -> byte [0, 240]
				// byte & 0xFF -> [0, 240]
				byte noiseDist = (byte) ((noise - 20f * Math
						.log10(distCenterMeter + 1)) * 2);
				// symmetry in quarter
				attenQuarterData[i * attenQuarterWidth + j] = noiseDist;
				attenQuarterData[(attenQuarterWidth - j - 1)
						* attenQuarterWidth + attenQuarterWidth - i - 1] = noiseDist;

			}
		}
		return attenQuarterData;
	}

	/**
	 * Gets distance between two noise values, if they result from the same
	 * source, derived from equation 4, p. 28
	 * 
	 * @param noise1
	 * @param noise2
	 * @return
	 */
	public static float getDistBetweenNoise(float noise1, float noise2) {
		return (float) Math.pow(10, (noise1 - noise2) / 20);
	}

	/**
	 * See chapter 4.5
	 * 
	 * @param cluster
	 * @param minZoom
	 * @return
	 */
	private static List<AkkumulatorPoint> getNoiseLocations(Cluster cluster,
			byte minZoom) {
		byte zoom = (byte) Math.max(Settings.MIN_ZOOM_MEASUREMENT_POSITION,
				minZoom);

		// get maximum bounds, that means bounding box around all points +
		// accuracy radius
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
		for (Measurement measurement : cluster.measurements) {
			Point locationTile = measurement.getLocationTile(zoom);
			int accuracyTile = measurement.getAccuracy(zoom);
			minX = Math.min(minX, locationTile.x - accuracyTile);
			maxX = Math.max(maxX, locationTile.x + accuracyTile);
			minY = Math.min(minY, locationTile.y - accuracyTile);
			maxY = Math.max(maxY, locationTile.y + accuracyTile);
		}

		// SIze
		int width = maxX - minX + 1;
		int height = maxY - minY + 1;

		// Reference for accumulation value
		int standardAcc = (int) (50 / Mercator.calculateGroundResolution(
				cluster.latitude, zoom));

		// Accumulator for probabilities
		float[] locationRatingMatrix = new float[width * height];

		// Normal distribution, saves values in DP sense
		NormalDistribution normDist = new NormalDistribution((short) 50);

		float maxRating = 0;
		for (Measurement measurement : cluster.measurements) {
			// For each cluster

			// cell coordinates
			Point locationTile = measurement.getLocationTile(zoom);
			int accuracyTile = measurement.getAccuracy(zoom);

			int offsetX = locationTile.x - minX;
			int offsetY = locationTile.y - minY;

			// raster quad describing accuracy
			for (int j = -accuracyTile; j <= accuracyTile; j++) {
				for (int i = -accuracyTile; i <= accuracyTile; i++) {
					int x = offsetX + j;
					int y = offsetY + i;
					float ratingInc = 0;
					if (accuracyTile != 0) {
						ratingInc = normDist.getProbability(j
								/ (float) accuracyTile, i
								/ (float) accuracyTile)
								* (standardAcc / (float) accuracyTile);
					} else {
						// if theere is no accuracy data
						ratingInc = standardAcc;
					}

					// increase accumulator and find maximum
					maxRating = Math.max(maxRating, locationRatingMatrix[y
							* width + x] += ratingInc);
				}
			}
		}

		// threshold for taking point
		float thresh = maxRating * 0.8f;
		// indicator cahce saving 3 rows of accumulator, true if > threshold,
		// false otherwise
		boolean[][] maxLocationCache = new boolean[3][width];

		// Returning points with indication if neighbor is also > threshold
		List<AkkumulatorPoint> maxRatingPoints = new ArrayList<AkkumulatorPoint>();

		for (int j = 0; j < height + 1; j++) { // extra row, because cache is
												// iterated with an offset of 1
			Arrays.fill(maxLocationCache[j % 3], false); // reset cache row

			for (int i = 0; i < width; i++) {
				if (j < height && locationRatingMatrix[j * width + i] >= thresh) {
					// update cache
					maxLocationCache[j % 3][i] = true;
				}
				int jRef = j - 1; // row for actual search for points
				if (jRef >= 0 && maxLocationCache[jRef % 3][i]) {
					AkkumulatorPoint newPoint = new AkkumulatorPoint(i + minX,
							jRef + minY, zoom);
					if (jRef - 1 >= 0) {
						newPoint.hasNeighborTop = maxLocationCache[(jRef - 1) % 3][i];
					}
					if (jRef + 1 < height) {
						newPoint.hasNeighborBottom = maxLocationCache[(jRef + 1) % 3][i];
					}
					if (i - 1 >= 0) {
						newPoint.hasNeighborLeft = maxLocationCache[jRef % 3][i - 1];
					}
					if (i + 1 < width) {
						newPoint.hasNeighborRight = maxLocationCache[jRef % 3][i + 1];
					}
					maxRatingPoints.add(newPoint);
				}
			}

		}

		locationRatingMatrix = null;
		maxLocationCache = null;
		return maxRatingPoints;
	}

	public static Bitmap interpolationToBitmap(MercatorRect bounds,
			byte[] interpolation, Bitmap reuseBmp) {
		return interpolationToBitmap(bounds.width(), bounds.height(),
				interpolation, reuseBmp);
	}

	/**
	 * Turns float array of extended byte into {@link Bitmap}
	 * 
	 * @param width
	 * @param height
	 * @param interpolation
	 * @param reuseBmp
	 * @return
	 */
	public static Bitmap interpolationToBitmap(int width, int height,
			byte[] interpolation, Bitmap reuseBmp) {
		if (reuseBmp == null) {
			reuseBmp = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
		}

		float maxNoise = 80 * 2;

		reuseBmp.eraseColor(Color.TRANSPARENT);
		int[] scanline = new int[width];

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				// calculate color graduation for every cell
				int noise = interpolation[i * width + j] & 0xFF;
				if (noise != NoiseGridValueProvider.NO_DATA) {
					scanline[j] = Color.argb(255,
							(int) (255 * (noise / maxNoise)),
							(int) (255 * (1 - noise / maxNoise)), 0);
				} else {
					scanline[j] = Color.TRANSPARENT;
				}
			}
			reuseBmp.setPixels(scanline, 0, width, 0, i, width, 1);
		}
		return reuseBmp;
	}

}
