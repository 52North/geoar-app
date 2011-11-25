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
package org.n52.android.view.geoar.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL11;

/**
 * OpenGL geometry for a XZ plane
 * 
 * @author Holger Hopmann
 *
 */
public abstract class Rectangle {
	protected static FloatBuffer vertexBuffer;

	public float width;
	public float height;

	static {
		// [1]-----[3]
		// | # \ ### | ^
		// | ## \ ## | |
		// | ### \ # | z
		// [0]-----[2]
		// x ->
		// y = 0

		vertexBuffer = ByteBuffer.allocateDirect(4 * 3 * (Float.SIZE / 8))
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(new float[] { 0, 0, 0, // 0
				0, 0, 1, // 1
				1, 0, 0, // 2
				1, 0, 1 // 3
				});
		vertexBuffer.position(0);

	}

	public Rectangle() {
	}

	public Rectangle(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	public abstract void draw(GL11 gl11);

}
