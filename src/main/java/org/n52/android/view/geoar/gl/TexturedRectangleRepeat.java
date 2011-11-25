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

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 * OpenGL plane in XZ which has additional texture coordinates to receive a
 * texture. Texture coordinates are not normalized, but range from 0 to width re
 * height
 * 
 * @author Holger Hopmann
 * 
 */
public class TexturedRectangleRepeat extends Rectangle {
	private FloatBuffer textureBuffer;

	private OpenGLTexture texture;

	public TexturedRectangleRepeat(OpenGLTexture texture, int width, int height) {
		super(width, height);
		this.texture = texture;
		setTextureBuffer();
	}

	public TexturedRectangleRepeat(OpenGLTexture texture) {
		super();
		this.texture = texture;
		setTextureBuffer();
	}

	private void setTextureBuffer() {
		// [1]-----[3]
		// | # \ ### | ^
		// | ## \ ## | |
		// | ### \ # | z
		// [0]-----[2]
		// x ->
		// y = 0

		textureBuffer = ByteBuffer.allocateDirect(4 * 2 * (Float.SIZE / 8))
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureBuffer.put(new float[] { 0, 0, // 0
				0, height, // 1
				width, 0, // 2
				width, height // 3
				});
		textureBuffer.position(0);
	}

	@Override
	public void setHeight(float height) {
		super.setHeight(height);
		setTextureBuffer();
	}

	@Override
	public void setWidth(float width) {
		super.setWidth(width);
		setTextureBuffer();
	}

	@Override
	public void draw(GL11 gl11) {
		if (texture.canBind()) {
			// hat Textur

			// Vertexbuffer
			gl11.glEnableClientState(GL10.GL_VERTEX_ARRAY);
			gl11.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);

			// Textur
			gl11.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl11.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
			texture.bind(gl11);

			// Zeichnen
			gl11.glPushMatrix();
			gl11.glScalef(width, 1, height);
			gl11.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
			gl11.glPopMatrix();

			// Buffer deaktivieren
			gl11.glDisableClientState(GL10.GL_VERTEX_ARRAY);
			gl11.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}
	}
}
