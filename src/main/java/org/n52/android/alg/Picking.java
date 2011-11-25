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
package org.n52.android.alg;

import org.n52.android.view.geoar.NoiseCamera;

import android.graphics.PointF;
import android.opengl.GLU;

/**
 * Class to calculate intersection of viewing frustum with interpolation plane.
 * See 5.3.1
 * 
 * @author Holger Hopmann
 * 
 */
public class Picking {

	private final static int INTERSECTION_BEFORE_SECTION = 1;
	private final static int INTERSECTION_AFTER_SECTION = 2;
	private final static int INTERSECTION_IN_SECTION = 3;
	private final static int NO_INTERSECTION = 4;

	/**
	 * Gibt den Schnittpunkt mit der Interpolationsebene zur�ck.
	 * Parameterbezeichnung f�r Fall der oberen Grenzlinie
	 * 
	 * @param model
	 *            Model-View Matrix
	 * @param project
	 *            Projektionsmatrix
	 * @param view
	 *            Viewport-Matrix
	 * @param winY
	 *            Y Koordinate ausgehen derer Schnittpunkt gesucht wird
	 * @param winYOpp
	 *            Y Koordinate vertikal der gegen�berliegenden Ecke
	 * @param winX
	 *            X Koordinate ausgehen derer Schnittpunkt gesucht wird
	 * @param winXOpp
	 *            X Koordinate horizontal der gegen�berliegenden Ecke
	 * @return
	 */
	public static PointF getGridPoint(float[] model, float[] project,
			int[] view, float winY, float winYOpp, float winX, float winXOpp) {
		// Oberer Punkt an far plane
		float[] pickingPointTopBack = getPickingPoint(model, project, view,
				winX, winY, 1);
		PointF gridPoint = new PointF();
		if (getGridPointFromLine(
				getPickingPoint(model, project, view, winX, winY, 0), // Top
																		// near
				pickingPointTopBack, gridPoint) == INTERSECTION_IN_SECTION) {
			// Von near zu far plane
			return gridPoint;
		} else {
			// Seitlich entlang far plane
			float[] pickingPointBottomBack = getPickingPoint(model, project,
					view, winX, winYOpp, 1);
			int returnCode = getGridPointFromLine(pickingPointTopBack,
					pickingPointBottomBack, gridPoint);
			if (returnCode == INTERSECTION_IN_SECTION) {
				return gridPoint;
			} else if (returnCode == INTERSECTION_AFTER_SECTION) {
				// Unten entlang far plane
				if (getGridPointFromLine(
						pickingPointBottomBack,
						getPickingPoint(model, project, view, winXOpp, winYOpp,
								1), gridPoint) == INTERSECTION_IN_SECTION) {
					return gridPoint;
				}
			} else if (returnCode == INTERSECTION_BEFORE_SECTION) {
				// Oben entlang far plane
				if (getGridPointFromLine(
						pickingPointTopBack,
						getPickingPoint(model, project, view, winXOpp, winY, 1),
						gridPoint) == INTERSECTION_IN_SECTION) {
					return gridPoint;
				}
			}
		}
		// Kein Schnitt
		return null;
	}

	public static int getGridPointFromLine(float[] p1, float[] p2, PointF result) {
		return getGridPointFromLine(p1, 0, p2, 0, result);
	}

	/**
	 * Calculates distance from reference point to intersection with
	 * interpolation plane
	 * 
	 * @param p1
	 * @param p1Offset
	 * @param p2
	 * @param p2Offset
	 * @param result
	 * @return
	 */
	public static int getGridPointFromLine(float[] p1, int p1Offset,
			float[] p2, int p2Offset, PointF result) {
		if (p2[p2Offset + 1] - p1[p1Offset + 1] == 0) {
			return NO_INTERSECTION;
		}

		float t = (-NoiseCamera.height - p1[p1Offset + 1])
				/ (p2[p2Offset + 1] - p1[p1Offset + 1]);
		result.x = (p2[p2Offset + 0] * t) + (1 - t) * p1[p1Offset + 0];
		result.y = (p2[p2Offset + 2] * t) + (1 - t) * p1[p1Offset + 2];
		if (t > 1) {
			// Schnitt nach p1p2
			return INTERSECTION_AFTER_SECTION;
		} else if (t < 0) {
			// Schnitt vor p1p2
			return INTERSECTION_BEFORE_SECTION;
		} else {
			return INTERSECTION_IN_SECTION;
		}
	}

	/**
	 * Unprojects point from window coordiantes to object coordinates in OpenGL
	 * context
	 * 
	 * @param model
	 * @param project
	 * @param view
	 * @param winX
	 * @param winY
	 * @param winZ
	 * @return
	 */
	public static float[] getPickingPoint(float[] model, float[] project,
			int[] view, float winX, float winY, float winZ) {
		float[] temp = new float[4];
		// Unproject
		GLU.gluUnProject(winX, winY, winZ, model, 0, project, 0, view, 0, temp,
				0);
		float[] result = new float[3];
		// Perspective division
		result[0] = temp[0] / temp[3];
		result[1] = temp[1] / temp[3];
		result[2] = temp[2] / temp[3];

		return result;
	}

	public static float[] getPickingRay(float[] model, float[] project,
			int[] view, float winX, float winY) {
		float[] result = new float[6];
		System.arraycopy(getPickingPoint(model, project, view, winX, winY, 0),
				0, result, 0, 3);
		System.arraycopy(getPickingPoint(model, project, view, winX, winY, 1),
				0, result, 3, 3);
		return result;
	}
}
