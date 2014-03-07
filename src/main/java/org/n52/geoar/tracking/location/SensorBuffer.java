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
 * Superclass for sensor data filtering classes.
 * 
 * @author Holger Hopmann
 *
 */
public abstract class SensorBuffer {

	protected int dataLength;

	public SensorBuffer(int dataLength) {
		this.dataLength = dataLength;
	}

	public abstract void put(float[] sensordata);

	public abstract float[] get();

	public abstract boolean hasValues();

}