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
package org.n52.geoar.newdata;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import org.n52.geoar.newdata.Filter;

/**
 * Specialized {@link ObjectInputStream} allowing to set the {@link ClassLoader}
 * to use for resolving classes. This is used for instance to load
 * {@link Filter} object states saved by different data sources. These
 * {@link Filter} classes are only accessible by the corresponding plugin
 * {@link ClassLoader}.
 * 
 */
public class PluginStateInputStream extends ObjectInputStream {

	private ClassLoader pluginClassLoader;

	public PluginStateInputStream(InputStream inputStream) throws IOException {
		super(inputStream);
	}

	@Override
	protected Class<?> resolveClass(ObjectStreamClass osClass)
			throws IOException, ClassNotFoundException {
		if (pluginClassLoader != null) {
			return pluginClassLoader.loadClass(osClass.getName());
		} else {
			return super.resolveClass(osClass);
		}
	}

	public void setPluginClassLoader(ClassLoader pluginClassLoader) {
		this.pluginClassLoader = pluginClassLoader;
	}

}
