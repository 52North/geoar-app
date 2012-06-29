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
package org.n52.android.tracking.location;


public class LocationVector {
	public float x;
	public float y;
	public float z;
	
	
	public LocationVector(){
		this.x = this.y = this.z = 0;
	}
	
	public LocationVector(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void set(float x, float y, float z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void add(LocationVector vec){
		add(vec.x, vec.y, vec.z);
	}
	
	public void sub(LocationVector vec){
		sub(-vec.x, -vec.y, -vec.z);
	}
	
	public void add(float x, float y, float z){
		this.x += x;
		this.y += y;
		this.z += z;
	}
	
	public void sub(float x, float y, float z){
		this.x -= x;
		this.y -= y;
		this.z -= z;
	}
	
	public void mult(float scalar){
		this.x *= scalar;
		this.y *= scalar;
		this.z *= scalar;
	}
	
	public void matrixMult(float[] m){
		float x = m[1] * this.x + m[2] * this.y + m[3] * this.z;
		float y = m[5] * this.x + m[6] * this.y + m[7] * this.z;
		float z = m[9] * this.x + m[10] * this.y + m[11] * this.z;
		
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void divide(float scalar){
		this.x /= scalar;
		this.y /= scalar;
		this.z /= scalar;
	}
	
	public float length(){
		return (float) Math.sqrt(x*x + y*y + z*z);
	}
	
	public void norm(){
		divide(length());
	}
}
