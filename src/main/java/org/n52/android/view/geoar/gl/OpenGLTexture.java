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

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.graphics.Bitmap;
import android.opengl.GLUtils;

/**
 * Helper to deal with OpenGL textures. It binds them, sets its parameters and
 * loads the data to the graphics layer as needed.
 * 
 * @author Holger Hopmann
 * 
 */
public abstract class OpenGLTexture {

	private boolean reloadTexture;
	private Integer textureId;

	public OpenGLTexture() {
		super();
	}

	public OpenGLTexture(boolean autoload) {
		this();
		reloadTexture = true;
	}

	/**
	 * Has to get called when the OpenGL context loses its resources so that
	 * textures need to get reloaded
	 */
	public void reset() {
		reloadTexture = canBind();
		textureId = null;
	}

	/**
	 * Signal that a texture has to get reloaded on the next draw
	 */
	public void reload() {
		reloadTexture = true;
	}

	/**
	 * Indicates if this texture is ready to be used, that means it has already
	 * been used or {@link OpenGLTexture#reload()} was called
	 * 
	 * @return
	 */
	public boolean canBind() {
		return reloadTexture || textureId != null;
	}

	/**
	 * Binds this texture. Loads data to graphics layer and gets its own texture
	 * id if needed
	 * 
	 * @param gl11
	 */
	protected void bind(GL11 gl11) {
		if (reloadTexture) {
			if (textureId == null) {
				// Neue id
				int[] newIds = new int[1];
				gl11.glGenTextures(1, newIds, 0);
				textureId = newIds[0];
				gl11.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
				getTextureParams(gl11);
			} else {
				gl11.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
			}

			Bitmap tempBmp = getBitmap();
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, tempBmp, 0);
			tempBmp.recycle();

			reloadTexture = false;
		} else if (textureId != null) {
			gl11.glBindTexture(GL10.GL_TEXTURE_2D, textureId);
		}
	}

	/**
	 * Method to specify OpenGL Texture parameters
	 * 
	 * @param gl11
	 */
	protected void getTextureParams(GL11 gl11) {
		gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
				GL10.GL_CLAMP_TO_EDGE);
		gl11.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
				GL10.GL_CLAMP_TO_EDGE);
	}

	protected abstract Bitmap getBitmap();

}