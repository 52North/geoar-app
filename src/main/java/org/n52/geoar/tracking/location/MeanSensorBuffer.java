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
 * A simple mean filter for sensor data
 * 
 * @author Holger Hopmann
 * 
 */
public class MeanSensorBuffer extends SensorBuffer {
	private int size;
	private int nextIndex = 0;
	private boolean hasValues = false;
	private float[] buffer;

	public MeanSensorBuffer(int dataLength, int size) {
		super(dataLength);
		buffer = new float[size * dataLength];
		this.size = size;
	}

	public void put(float[] sensordata) {
		System.arraycopy(sensordata, 0, buffer, nextIndex * dataLength,
				dataLength);
		nextIndex = (nextIndex + 1) % size;
		if (!hasValues && nextIndex == 0) {
			hasValues = true;
		}
	}

	public float[] get() {
		float[] sum = new float[dataLength];
		if (!hasValues) {
			// Nullvektor zur�ckgeben
			return sum;
		}
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < dataLength; j++) {
				sum[j] += buffer[i * dataLength + j];
			}
		}

		for (int j = 0; j < dataLength; j++) {
			sum[j] = sum[j] / size;
		}
		return sum;
	}

	public boolean hasValues() {
		return hasValues;
	}
}