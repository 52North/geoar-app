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
package org.n52.android.view.geoar.gl.model.primitives;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.n52.android.view.geoar.gl.model.RenderNode;


public class Sphere extends RenderNode{
    
    	private class SphereKey{
    	    public float radius;
    	    public int segmentsW;
    	    public int segmentsH;
    	    public int verticesCount;
    	    
    	    public SphereKey(float radius, int segmentsW, int segmentsH){
    		this.radius = radius;
    		this.segmentsW = segmentsW;
    		this.segmentsH = segmentsH;
    		this.verticesCount = (segmentsW + 1) * (segmentsH + 1);
    	    }

	    /* (non-Javadoc)
	     * @see java.lang.Object#hashCode()
	     */
	    @Override
	    public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result;
		result = prime * result + Float.floatToIntBits(radius);
		result = prime * result + segmentsH;
		result = prime * result + segmentsW;
		return result;
	    }

	    /* (non-Javadoc)
	     * @see java.lang.Object#equals(java.lang.Object)
	     */
	    @Override
	    public boolean equals(Object obj) {
		if (this == obj)
		    return true;
		if (obj == null)
		    return false;
		if (!(obj instanceof SphereKey))
		    return false;
		SphereKey other = (SphereKey) obj;
		if (Float.floatToIntBits(radius) != Float.floatToIntBits(other.radius))
		    return false;
		if (segmentsH != other.segmentsH)
		    return false;
		if (segmentsW != other.segmentsW)
		    return false;
		return true;
	    }
    	}
    
	private class SphereGeometry{
		private float radius;
		private int segmentsW; 
		private int segmentsH;
		
		public float[] vertices;
		public float[] normals;
		public int[] indices;
		
		public SphereGeometry(SphereKey sphereKey){
		    	this.radius = sphereKey.radius; 
		    	this.segmentsH = sphereKey.segmentsH; 
		    	this.segmentsW = sphereKey.segmentsW;
		    	
			int verticesCount 	= (segmentsW + 1) * (segmentsH + 1);
			int indicesCount 	= 2 * segmentsW * (segmentsH - 1) * 3;
			
			vertices = new float[verticesCount * 3];
			normals = new float[verticesCount * 3];

			indices = new int[indicesCount];
			
			int vertIndex = 0, index = 0;
			
			for(int j=0; j <= segmentsH; ++j){
				float horAngle = (float) (Math.PI * j / segmentsH);
				float z = radius * (float)Math.cos(horAngle);
				float ringRadius = radius * (float)Math.sin(horAngle);
				
				for(int i=0; i < segmentsW; ++i){
					float verAngle = (float) (2.0f * Math.PI * i / segmentsW);
					float x = ringRadius * (float) Math.cos(verAngle);
					float y = ringRadius * (float) Math.sin(verAngle);
					float normalLength = 1.0f / (float) Math.sqrt(x*x+y*y+z*z);
					
					normals[vertIndex] = x * normalLength;
					vertices[vertIndex++] = x;
					normals[vertIndex] = -z * normalLength;
					vertices[vertIndex++] = -z;
					normals[vertIndex] = y * normalLength;
					vertices[vertIndex++] = y;
					
					if(i > 0 && j > 0){
						int a = (segmentsW + 1) * j + i;
						int b = (segmentsW + 1) * j + i - 1;
						int c = (segmentsW + 1) * (j - 1) + i - 1;
						int d = (segmentsW + 1) * (j - 1) + i;
						
						if(j == segmentsH){
							indices[index++] = a;
							indices[index++] = c;
							indices[index++] = d;
						} else if(j == 1){
							indices[index++] = a;
							indices[index++] = b;
							indices[index++] = c;
						} else {
							indices[index++] = a;
							indices[index++] = b;
							indices[index++] = c;
							indices[index++] = a;
							indices[index++] = c;
							indices[index++] = d;
						}
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result;
			result = prime * result + Float.floatToIntBits(radius);
			result = prime * result + segmentsH;
			result = prime * result + segmentsW;
			return result;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof SphereGeometry))
				return false;
			SphereGeometry other = (SphereGeometry) obj;
			if (Float.floatToIntBits(radius) != Float
					.floatToIntBits(other.radius))
				return false;
			if (segmentsH != other.segmentsH)
				return false;
			if (segmentsW != other.segmentsW)
				return false;
			return true;
		}
	}
	
	private static Map<SphereKey, SphereGeometry> spheres = 
		Collections.synchronizedMap(new WeakHashMap<SphereKey, SphereGeometry>());
	
	private SphereKey sphereKey;
	
	public Sphere(float radius, int segmentsW, int segmentsH){
		super();
		this.sphereKey = new SphereKey(radius, segmentsW, segmentsH);
		SphereGeometry geometry = spheres.get(sphereKey);
		if(geometry == null)
		    geometry = new SphereGeometry(sphereKey);
		    spheres.put(sphereKey, geometry);
		

		float[] colors = new float[sphereKey.verticesCount * 4];
		for(int colorCount = sphereKey.verticesCount * 4, i = 0; i < colorCount; i += 4){
			colors[i] = 1.0f;
			colors[i + 1] = 0;
			colors[i + 2] = 0;
			colors[i + 3] = 1.0f;
		}
		
		setRenderObjectives(geometry.vertices, colors, geometry.normals, geometry.indices);
	}


	@Override
	protected void onPreRender() {
		// TODO Auto-generated method stub
		
	}
}
