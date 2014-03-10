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
package org.n52.geoar.tracking.location;


/**
 * 
 * Adaptive low pass filter for sensor data, adjusts alpha value based on
 * distance to previous values
 * 
 * @author Holger Hopmann
 * 
 */
public class AdaptiveLowPassSensorBuffer extends SensorBuffer {
	private float[] buffer;
	private float alphaLow;
	private float alphaHigh;
	private float thresholdLow;
	private float thresholdHigh;

	public AdaptiveLowPassSensorBuffer(int dataLength, float thresholdLow,
			float thresholdHigh, float alphaLow, float alphaHigh) {
		super(dataLength);
		this.alphaLow = alphaLow;
		this.alphaHigh = alphaHigh;
		this.thresholdLow = thresholdLow;
		this.thresholdHigh = thresholdHigh;
		buffer = new float[dataLength];
	}

	private boolean hasValues;

	public void put(float[] sensordata) {

		float dist = 0;
		for (int i = 0; i < dataLength; i++) {
			dist += Math.abs(buffer[i] - sensordata[i]);
		}
		dist /= (float) dataLength;

		float alpha;
		if (dist < thresholdLow) {
			alpha = alphaLow;
		} else if (dist > thresholdHigh) {
			alpha = alphaHigh;
		} else {
			float thresholdDiff = thresholdHigh - thresholdLow;
			float alphaDiff = alphaHigh - alphaLow;

			alpha = alphaLow + alphaDiff
					* ((thresholdHigh - dist) / thresholdDiff);
		}

		for (int i = 0; i < dataLength; i++) {
			buffer[i] = alpha * sensordata[i] + (1 - alpha) * buffer[i];
		}
		hasValues = true;
	}

	public float[] get() {
		return buffer;
	}

	public boolean hasValues() {
		return hasValues;
	}
}