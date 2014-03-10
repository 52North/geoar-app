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

import java.nio.FloatBuffer;

/**
 * 
 * @author Arne de Wall
 *
 */
public class BoundingBox {
	
	private float[][] boundingPoints;
	
	public BoundingBox(float[] vertices){
		this.boundingPoints = new float[8][3];
		
		float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] max = { Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE };
		
		float[] vertex = new float[3];
		
		for(int i = 0; i < vertices.length; i+=3){
			vertex[0] = vertices[i];
			vertex[1] = vertices[i+1];
			vertex[2] = vertices[i+2];
			
			min[0] = Math.min(min[0], vertex[0]);
			max[0] = Math.max(max[0], vertex[0]);
			min[1] = Math.min(min[1], vertex[1]);
			max[1] = Math.max(max[1], vertex[1]);
			min[2] = Math.min(min[2], vertex[2]);
			max[2] = Math.max(max[2], vertex[2]);
		}
		
		boundingPoints[0] = new float[] { min[0], min[1], min[2] }; // -x,-y,-z
		boundingPoints[1] = new float[] { min[0], min[1], max[2] }; // -x,-y,z
		boundingPoints[2] = new float[] { max[0], min[1], max[2] }; // x,-y,z
		boundingPoints[3] = new float[] { max[0], min[1], min[2] }; // x,-y,-z

		boundingPoints[4] = new float[] { min[0], max[1], min[2] }; // -x, y, -z
		boundingPoints[5] = new float[] { min[0], max[1], max[2] }; // -x, y, z
		boundingPoints[6] = new float[] { max[0], max[1], max[2] }; // ...
		boundingPoints[7] = new float[] { max[0], max[1], min[2] };
	}

	@Deprecated
	private void generateBoundingBox() {
		// get vertices of geometry
		FloatBuffer vertices = null; // geometry.getVerticesBuffer();
		vertices.rewind(); // set position to zero

		float[] min = { Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE };
		float[] max = { Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE };

		float[] vertex = new float[3];

		while (vertices.hasRemaining()) {
			vertex[0] = vertices.get(); // x
			vertex[1] = vertices.get(); // y
			vertex[2] = vertices.get(); // z

			min[0] = Math.min(min[0], vertex[0]);
			max[0] = Math.max(max[0], vertex[0]);
			min[1] = Math.min(min[1], vertex[1]);
			max[1] = Math.max(max[1], vertex[1]);
			min[2] = Math.min(min[2], vertex[2]);
			max[2] = Math.max(max[2], vertex[2]);
		}

		boundingPoints[0] = new float[] { min[0], min[1], min[2] }; // -x,-y,-z
		boundingPoints[1] = new float[] { min[0], min[1], max[2] }; // -x,-y,z
		boundingPoints[2] = new float[] { max[0], min[1], max[2] }; // x,-y,z
		boundingPoints[3] = new float[] { max[0], min[1], min[2] }; // x,-y,-z

		boundingPoints[4] = new float[] { min[0], max[1], min[2] }; // -x, y, -z
		boundingPoints[5] = new float[] { min[0], max[1], max[2] }; // -x, y, z
		boundingPoints[6] = new float[] { max[0], max[1], max[2] }; // ...
		boundingPoints[7] = new float[] { max[0], max[1], min[2] };
	}

}
