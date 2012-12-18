package org.n52.android.view.geoar.gl;

import org.n52.android.newdata.SpatialEntity;
import org.n52.android.view.geoar.gl.mode.RenderFeature;

import android.location.Location;

public class ARObject {

	public interface Conditions {
		boolean nearVisualization();
		boolean farVisualization();
	}
	
	private float distanceTo;

	private RenderFeature renderFeature;
	private SpatialEntity entity;

	
	public ARObject(SpatialEntity entity){
		this.entity = entity;
	}
	
	public void onRender(float[] projectionMatrix, float[] viewMatrix, float[] parentMatrix){
		// TODO different visualizations
		if(renderFeature != null)
			renderFeature.onRender(projectionMatrix, viewMatrix, parentMatrix);
	}

	public void onLocationUpdate(Location location) {
		if (entity == null)
			return;

		final double longitude = entity.getLongitude();
		final double latitude = entity.getLatitude();
		int altitude = entity.getAltitude();
		
		/** calc the distance */
		final float[] x = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), latitude, longitude, x);
		distanceTo = x[0];
		
		// just want the distance -> length 1
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), location.getLatitude(), longitude, x);
		// just want the distance -> length 1
		final float[] z = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), latitude, location.getLongitude(), z);

		// correct the direction according to the poi location, because we just
		// get the distance in x and z direction
		if (location.getLongitude() < longitude)
			x[0] *= -1;
		if (location.getLatitude() < latitude)
			z[0] *= -1;
		if (altitude == 0)
			altitude = (int) location.getAltitude();
		// testen
		renderFeature
				.setPosition(new float[] { (float) x[0] / 10f,
						(float) (altitude - location.getAltitude()),
						(float) z[0] / 10f});
	}
}
