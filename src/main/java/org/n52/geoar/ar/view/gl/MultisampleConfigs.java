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
package org.n52.geoar.ar.view.gl;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

import android.opengl.GLSurfaceView;
import android.util.Log;

public class MultisampleConfigs implements GLSurfaceView.EGLConfigChooser {

	private final int[] res = new int[1];
	private boolean usesCoverage;
	private int[] config;
	
	@Override
	public EGLConfig chooseConfig(EGL10 egl10, EGLDisplay display) {

		//@formatter:off
        /** Try to get rgb565 standard configurations */
        int[] config = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_RENDERABLE_TYPE, 4,
                EGL10.EGL_SAMPLE_BUFFERS, 1,
                EGL10.EGL_SAMPLES, 2,
                EGL10.EGL_NONE
        };
        //@formatter:on

		/**
		 * returns a list of all EGL frame buffer configurations that match the
		 * attributes of config
		 */
		if (!egl10.eglChooseConfig(display, config, null, 0, res)) {
			throw new IllegalArgumentException("eglChooseConfig failed");
		}

		if (res[0] <= 0) {
			/**
			 * try to find a multisampling configuration with coverage sampling
			 * (case for tegra2)
			 */
			final int EGL_COVERAGE_BUFFERS_NV = 0x30E0;
			final int EGL_COVERAGE_SAMPLES_NV = 0x30E1;

			//@formatter:off
			config = new int[] { 
					EGL10.EGL_RED_SIZE, 8, 
					EGL10.EGL_GREEN_SIZE, 8, 
					EGL10.EGL_BLUE_SIZE, 8, 
					EGL10.EGL_ALPHA_SIZE, 8,
					EGL10.EGL_DEPTH_SIZE, 16,
					EGL10.EGL_RENDERABLE_TYPE, 4,
					EGL_COVERAGE_BUFFERS_NV, 1,
					EGL_COVERAGE_SAMPLES_NV, 2, 
					EGL10.EGL_NONE 
			};
			//@formatter:on

			/**
			 * returns a list of all EGL frame buffer configurations that match
			 * the attributes of config
			 */
			if (!egl10.eglChooseConfig(display, config, null, 0, res)) {
				throw new IllegalArgumentException("eglChooseConfig failed");
			}

			if (res[0] <= 0) {
				/** no multisampling matched - try without. */
				//@formatter:off
				config = new int[] { 
						EGL10.EGL_RED_SIZE, 8,
						EGL10.EGL_GREEN_SIZE, 8, 
						EGL10.EGL_BLUE_SIZE, 8,
						EGL10.EGL_ALPHA_SIZE, 8,
						EGL10.EGL_DEPTH_SIZE, 16, 
						EGL10.EGL_RENDERABLE_TYPE, 4, 
						EGL10.EGL_NONE 
				};
				//@formatter:on

				if (!egl10.eglChooseConfig(display, config, null, 0, res)) {
					throw new IllegalArgumentException(
							"3rd eglChooseConfig failed");
				}

				if (res[0] <= 0)
					throw new IllegalArgumentException("No configs matched");
			} else {
				usesCoverage = true;
			}

		}

		int configSize = res[0];

		/** get matched configurations */
		EGLConfig[] eglConfigs = new EGLConfig[configSize];
		if (!egl10
				.eglChooseConfig(display, config, eglConfigs, configSize, res)) {
			throw new IllegalArgumentException("data eglChooseConfig failed");
		}

		int index = 0;
//		for (int i = 0; i < eglConfigs.length; ++i) {
//			if(egl10.eglGetConfigAttrib(display, eglConfigs[i], EGL10.EGL_RED_SIZE, res)){
//				if(res[0] == 5){
//					index = i;
//					break;
//				}
//			}
//		}
//		
		EGLConfig eglConfig = null;
		if(eglConfigs.length > 0 && index != -1){
			eglConfig = eglConfigs[index];
		} else {
			Log.w("MultisampleConfig", "no configs found");
		}
		
		return eglConfig;
	}


	public boolean usesCoverage() {
		return usesCoverage;
	}

}
