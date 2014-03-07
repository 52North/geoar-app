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
package org.n52.geoar.view.geoar.gl.mode;

import org.n52.geoar.ar.view.gl.GLESCamera;


public abstract class Spatial {

	protected double longitude;
	protected double latitude;
	@Deprecated
	protected double altitude;

	protected final float[] position = new float[]{0.0f, -1.6f, 0.0f, 1.0f};
	protected final float[] rotation = new float[]{0.0f, 0.0f, 0.0f, 1.0f};
	protected final float[] scale = new float[]{1.0f, 1.0f, 1.0f};

	public Spatial() {
		float[] cameraPosition = GLESCamera.cameraPosition;
		position[0] = -cameraPosition[0];
		position[1] = -cameraPosition[1];
		position[2] = -cameraPosition[2];

		position[3] = 1;
	}

	public float[] getPosition() {
		return position;
	}

	public void setRelativePosition(float[] position) {
		this.position[0] = position[0]; // - GLESCamera.cameraPosition[0];
		this.position[1] = position[1] - GLESCamera.cameraPosition[1];
		this.position[2] = position[2]; // - GLESCamera.cameraPosition[2];
	}
	
	public float getMinimumYCoordinate(){
		
		return 0;
	}

	public float getX() {
		return position[0];
	}

	public float getY() {
		return position[1];
	}

	public float getZ() {
		return position[2];
	}

	public void setX(float x) {
		position[0] = x;
	}

	public void setY(float y) {
		position[1] = y;
	}

	public void setZ(float z) {
		position[2] = z;
	}

	public void setScale(float scaleX, float scaleY, float scaleZ) {
		scale[0] = scaleX;
		scale[1] = scaleY;
		scale[2] = scaleZ;
	}

	public float[] getScale() {
		return scale;
	}

	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @param longitude
	 *            the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @param latitude
	 *            the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @return the altitude
	 */
	public double getAltitude() {
		return altitude;
	}

	/**
	 * @param altitude
	 *            the altitude to set
	 */
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
}
