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

import java.util.Arrays;

/**
 * class to calculate normal distribution. Keeps array of previously calculated
 * values
 * 
 * @author Holger Hopmann
 * 
 */
public class NormalDistribution {

	private float[] gaussValues;
	private float constFactor;
	private short precision;

	private static float sd = 1;
	private static float kernelRadius = 3 * sd;
	private static float variance = sd * sd;

	public NormalDistribution(short precision) {
		gaussValues = new float[precision];
		this.precision = precision;
		Arrays.fill(gaussValues, -1);
		constFactor = (float) (1 / (Math.sqrt(2 * Math.PI) * sd));
	}

	public float getProbability(float xNorm) {
		xNorm = Math.abs(xNorm);
		short index = (short) (xNorm * (precision - 1));
		if (gaussValues[index] == -1) {
			float x = xNorm * kernelRadius;
			// Calculation of normal distribution
			gaussValues[index] = (float) (constFactor * Math.exp(-x * x
					/ (2 * variance)));
		}
		return gaussValues[index];
	}

	public float getProbability(float xNorm, float yNorm) {
		return getProbability(xNorm) * getProbability(yNorm);
	}

}
