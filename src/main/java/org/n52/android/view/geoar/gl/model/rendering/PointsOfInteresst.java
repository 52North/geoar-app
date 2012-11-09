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
package org.n52.android.view.geoar.gl.model.rendering;

import org.n52.android.view.geoar.gl.model.RenderNode;

public class PointsOfInteresst extends RenderNode {

	@Override
	public void setOpenGLPreRenderingSettings() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColor(int androidColor) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setColor(float[] colorArray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPreRender() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCreateInGLESThread() {
		// TODO Auto-generated method stub
		
	}
//implements
//		OnObservationUpdateListener, GeoLocationUpdateListener {
//
//	public enum SelectedRepresentation {
//		CUBE, SPHERE
//	}
//
//	private class POIHolder {
//		public Measurement measurement;
//		public RenderNode renderNode;
//
//		public POIHolder(Measurement measurement, RenderNode renderNode) {
//			this.measurement = measurement;
//			this.renderNode = renderNode;
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see java.lang.Object#equals(java.lang.Object)
//		 */
//		@Override
//		public boolean equals(Object o) {
//			return measurement.equals(o);
//		}
//
//		/*
//		 * (non-Javadoc)
//		 * 
//		 * @see java.lang.Object#hashCode() //
//		 */
//		@Override
//		public int hashCode() {
//			return measurement.hashCode();
//		}
//	}
//
//	class GenerateRenderingsRunnable implements Runnable {
//		final List<Measurement> measurements;
//
//		public GenerateRenderingsRunnable(List<Measurement> measurements) {
//			this.measurements = measurements;
//		}
//
//		@Override
//		public void run() {
//			// TODO Auto-generated method stub
//			glSurfaceView.queueEvent(new Runnable() {
//
//				@Override
//				public void run() {
//					// TODO Auto-generated method stub
//					for (Measurement measure : measurements) {
//						if (pois.contains(measure))
//							return;
//						RenderNode renderNode = new Cube(1);
//						renderNode.setRenderer(renderer);
//						// set position and coordinates
//						renderNode.setLatitude(measure.getLatitude());
//						renderNode.setLongitude(measure.getLongitude());
//						float[] relativePosition = LocationConverter
//								.getRelativePositionVec2(location,
//										measure.getLongitude(),
//										measure.getLatitude(), 0);
//						float[] color = PointsOfInteresst.getColor(
//								measure.getValue(), maxValue, minValue);
//
//						renderNode.setPosition(relativePosition);
//						POIHolder holder = new POIHolder(measure, renderNode);
//
//						pois.add(holder);
//						children.add(renderNode);
//					}
//
//				}
//
//			});
//		}
//
//	}
//
//	private static float[] getColor(float value, float maxValue, float minValue) {
//		float interval = maxValue - minValue;
//		float threshold = minValue + interval / 2;
//		float[] res = new float[4];
//		if (value <= threshold) { // grÃ¼n
//			res[1] = 1;
//			res[0] = 2 * (value - minValue) / interval;
//		}
//		if (value > threshold) {
//			res[0] = 1;
//			res[1] = 1 - 2 * ((value - minValue) / interval - 0.5f);
//		}
//		return res;
//	}
//
//	private Set<POIHolder> pois;
//
//	private float minValue;
//	private float maxValue;
//	private Class<?> renderRepresentation = Cube.class;
//
//	private GeoLocation location;
//
//	private final GLSurfaceView glSurfaceView;
//
//	public PointsOfInteresst(Renderer renderer, GLSurfaceView glSurfaceView) {
//		this.renderer = renderer;
//		this.pois = new HashSet<POIHolder>();
//		this.isComposition = true;
//		this.glSurfaceView = glSurfaceView;
//	}
//
//	@Override
//	protected void onPreRender() {
//		// TODO Auto-generated method stub
//
//	}
//
//	/*
//	 * (non-Javadoc)
//	 * 
//	 * @see org.n52.android.view.geoar.gl.model.RenderNode#onRender(float[],
//	 * float[], float[])
//	 */
//	@Override
//	public void onRender(float[] projectionMatrix, float[] viewMatrix,
//			float[] parentMatrix) {
//		for (RenderNode n : children) {
//			n.onRender(projectionMatrix, viewMatrix, parentMatrix);
//		}
//	}
//
//	@Deprecated
//	public void changeRenderingRepresentation(Class clazz) {
//		this.renderRepresentation = clazz;
//		try {
//			RenderNode rn = (RenderNode) renderRepresentation.newInstance();
//		} catch (InstantiationException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		}
//	}
//
//	/**
//	 * Sets the new observations
//	 * 
//	 * @param measurements
//	 *            list of measurements
//	 */
//	public void onObservationUpdate(MeasurementsCallback m) {
//		List<Measurement> newMeasurements = m.measurementBuffer;
//		float value, max = Float.MIN_VALUE, min = Float.MAX_VALUE;
//		// get max and min values for color interpolation
//		for (Measurement measure : newMeasurements) {
//			value = measure.getValue();
//			if (value > max)
//				max = value;
//			else if (value < min)
//				min = value;
//		}
//		this.minValue = min;
//		this.maxValue = max;
//
//		if (location == null)
//			return;
//
//		new GenerateRenderingsRunnable(m.measurementBuffer).run();
//	}
//
//	private void updateRelativePosition(GeoLocation locationUpdate) {
//		for (RenderNode rn : children) {
//			float[] newPosition = LocationConverter.getRelativePositionVec2(
//					locationUpdate, rn.getLongitude(), rn.getLatitude(),
//					rn.getAltitude());
//			rn.setPosition(newPosition);
//		}
//	}
//
//	@Override
//	public void onGeoLocationUpdate(GeoLocation g) {
//		this.location = g;
//		updateRelativePosition(g);
//	}

}
