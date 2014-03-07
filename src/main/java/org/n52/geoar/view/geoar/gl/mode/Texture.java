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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class Texture {
	private AtomicInteger textureHandle;
	private Callable<Bitmap> bitmapCallback;

	private static int MAX_TEXTURES = 10;
	private static Map<Object, Texture> TEXTURE_CACHE = new HashMap<Object, Texture>();
	private static Queue<AtomicInteger> HANDLE_CACHE = new LinkedList<AtomicInteger>();

	private Texture(Callable<Bitmap> bitmapCallback) {
		this.bitmapCallback = bitmapCallback;
	}

	public static void resetTextures() {
		for (AtomicInteger handle : HANDLE_CACHE) {
			handle.set(-1);
		}
		HANDLE_CACHE.clear();
	}

	public static Texture createInstance(Callable<Bitmap> bitmapCallback) {
		Texture cachedTexture = TEXTURE_CACHE.get(bitmapCallback);
		if (cachedTexture == null) {
			cachedTexture = new Texture(bitmapCallback);
			TEXTURE_CACHE.put(bitmapCallback, cachedTexture);
		}

		return cachedTexture;
	}

	private static AtomicInteger createTextureHandle() {
		int newHandle = -1;
		if (HANDLE_CACHE.size() >= MAX_TEXTURES) {
			AtomicInteger reusedHandle = HANDLE_CACHE.poll();
			newHandle = reusedHandle.getAndSet(-1);
		}

		if (newHandle == -1) {
			int[] textures = new int[1];
			GLES20.glGenTextures(1, textures, 0);
			newHandle = textures[0];
		}
		AtomicInteger result = new AtomicInteger(newHandle);
		HANDLE_CACHE.add(result);

		return result;
	}

	private void initTexture() throws Exception {
		textureHandle = createTextureHandle();

		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle.get());

		// Set filtering
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
				GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE);

		// Load the bitmap into the bound texture.
		Bitmap bitmap = bitmapCallback.call();
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		bitmap.recycle();
	}

	public void bindTexture() {
		if (textureHandle == null || textureHandle.get() == -1) {
			try {
				initTexture();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle.get());
		}
	}
}
