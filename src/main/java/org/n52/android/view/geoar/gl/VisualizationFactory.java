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
package org.n52.android.view.geoar.gl;

import org.n52.android.newdata.RenderingFactory;
import org.n52.android.newdata.gl.primitives.RenderLoader;
import org.n52.android.newdata.gl.primitives.DataSourceRenderable;
import org.n52.android.view.geoar.gl.model.RenderNode;
import org.n52.android.view.geoar.gl.model.primitives.Cube;

import android.opengl.GLSurfaceView;

public class VisualizationFactory implements RenderingFactory {

	private GLSurfaceView glSurfaceView;

	/**
	 * Static Methods
	 */
	public static VisualizationFactory INSTANCE;

	/**
	 * Constructor
	 */
	private VisualizationFactory() {
		this.INSTANCE = new VisualizationFactory();
	}

	/**
	 * Enqueues runnable to be run on the GL rendering thread of the
	 * {@link GLSurfaceView}. It is not possible to allocate GPU-memory for the
	 * rendering objectives in any thread.
	 * 
	 * @param renderNode
	 *            The node that has to allocate memory on the GPU.
	 */
	private void queueRenderable(final RenderNode renderNode) {
		this.glSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
				renderNode.onCreateInGLESThread();
			}
		});
	}

	@Override
	public DataSourceRenderable createCube() {
		return new Cube();
	}

	@Override
	public DataSourceRenderable createSphere() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DataSourceRenderable createRenderable(RenderLoader renderLoader) {
		// TODO Auto-generated method stub
		return null;
	}

}
