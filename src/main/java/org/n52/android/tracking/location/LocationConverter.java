package org.n52.android.tracking.location;

import org.osmdroid.util.GeoPoint;

import android.location.Location;
import android.util.Log;

public class LocationConverter {
	
	
	public static LocationVector getRelativePositionVec(GeoPoint cameraPosition, double longitude, double latitude, double altitude) {
		// Calculating the direction vector of the "user" Location to the poi Location
		
		// just want the distance -> length 1
		float[] x = new float[1]; 
		Location.distanceBetween(cameraPosition.getLatitudeE6() / 1E6f, cameraPosition.getLongitudeE6()  / 1E6f, cameraPosition.getLatitudeE6()  / 1E6f, longitude, x);
		// just want the distance -> length 1
		float[] z = new float[1];
		Location.distanceBetween(cameraPosition.getLatitudeE6() / 1E6f, cameraPosition.getLongitudeE6() / 1E6f, latitude, cameraPosition.getLongitudeE6() / 1E6f, z);
		
		// correct the direction according to the poi location, because we just get the distance in x and z direction
		if(cameraPosition.getLongitudeE6() / 1E6f < longitude)
			x[0] *= -1;
		if(cameraPosition.getLatitudeE6() / 1E6f < latitude)
			z[0] *= -1;
		if(altitude == 0)
			altitude = cameraPosition.getAltitude();
		 // testen
		return new LocationVector((float)x[0]/10f, (float)(altitude - cameraPosition.getAltitude()), (float)z[0]/10f);
	}
}
