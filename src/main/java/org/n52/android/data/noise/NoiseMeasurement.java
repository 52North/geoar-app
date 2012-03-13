package org.n52.android.data.noise;

import java.util.Calendar;

import org.n52.android.alg.proj.MercatorProj;
import org.n52.android.alg.proj.MercatorPoint;
import org.n52.android.data.Measurement;

import android.location.Location;

public class NoiseMeasurement extends Measurement{
	
	private float noise;
	
	@Override
	public void setTime(Calendar time) {
		this.time = time;
	}

	@Override
	public void setLocation(Location location) {
		this.latitude = location.getLatitude();
		this.longitude = location.getLongitude();
		this.locationAccuracy = location.getAccuracy();
	}

	@Override
	public void setValue(float value) {
		this.noise = value;
	}
	

	/**
	 * Gets {@link MercatorPoint} of this measurement's location with specified
	 * zoom
	 * 
	 * @param zoom
	 * @return
	 */
	@Override
	public MercatorPoint getLocationTile(byte zoom) {
		return new MercatorPoint((int) MercatorProj.transformLonToPixelX(longitude,
				zoom), (int) MercatorProj.transformLatToPixelY(latitude, zoom), zoom);
	}

	/**
	 * Get accuracy in pixel coordinates at the specified {@link MercatorProj} zoom
	 * level
	 * 
	 * @param zoom
	 * @return
	 */
	@Override
	public int getAccuracy(byte zoom) {
		return (int) (locationAccuracy / MercatorProj.getGroundResolution(latitude,
				zoom));
	}
	
	@Override
	public float getAccuracy() {
		return locationAccuracy;
	}

	@Override
	public Calendar getTime() {
		return time;
	}

	@Override
	public float getValue() {
		return noise;
	}


}
