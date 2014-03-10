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
 * Simple low pass filter for sensor data
 * 
 * @author Holger Hopmann
 * 
 */
public class LowPassSensorBuffer extends SensorBuffer {
	private float[] buffer;

	public LowPassSensorBuffer(int dataLength, float alpha) {
		super(dataLength);
		this.alpha = alpha;
		buffer = new float[dataLength];
	}

	private float alpha;
	private boolean hasValues;

	public void put(float[] sensordata) {
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