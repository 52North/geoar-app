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

import java.util.Calendar;

import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorPoint;

import android.location.Location;

/**
 * Simple class holding a measurement
 * 
 * @author Holger Hopmann
 * @author Arne de Wall
 */
public abstract class Measurement implements Cloneable {

	public double latitude;
	protected double longitude;
	
	protected float locationAccuracy;
	protected Calendar time;

	public void setTime(Calendar time) {
		this.time = time;
	}
	
	public abstract void setValue(float value);
	public abstract float getValue();

	public void setLocation(Location location) {
		this.latitude = location.getLatitude();
		this.longitude = location.getLongitude();
		this.locationAccuracy = location.getAccuracy();
	}

	/**
	 * Gets {@link MercatorPoint} of this measurement's location with specified
	 * zoom
	 * 
	 * @param zoom
	 * @return
	 */
	public MercatorPoint getLocationTile(byte zoom) {
		return new MercatorPoint((int) MercatorProj.transformLonToPixelX(longitude,
				zoom), (int) MercatorProj.transformLatToPixelY(latitude, zoom), zoom);
	}

	/**
	 * Get acciracy in pixel coordinates at the specified {@link MercatorProj} zoom
	 * level
	 * 
	 * @param zoom
	 * @return
	 */
	public int getAccuracy(byte zoom) {
		return (int) (locationAccuracy / MercatorProj.getGroundResolution(latitude,
				zoom));
	}

	public float getAccuracy() {
		return locationAccuracy;
	}

	public Calendar getTime() {
		return time;
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException{
		return super.clone();
	}
	
	public double getLatitude(){
		return latitude;
	}
	
	public double getLongitude(){
		return longitude;
	}
	
}
