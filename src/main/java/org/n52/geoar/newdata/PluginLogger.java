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

import org.n52.geoar.utils.DataSourceLoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class PluginLogger implements DataSourceLoggerFactory.Logger {

	private org.slf4j.Logger logger;

	public PluginLogger(PluginHolder plugin, Class<?> clazz) {
		logger = LoggerFactory.getLogger(plugin.getIdentifier() + "-"
				+ clazz.getSimpleName());
	}

	public PluginLogger(Class<?> clazz) {
		logger = LoggerFactory.getLogger(clazz);
	}

	@Override
	public void warn(String message) {
		logger.warn(message);
	}

	@Override
	public void error(String message) {
		logger.error(message);
	}

	@Override
	public void info(String message) {
		logger.info(message);
	}

	@Override
	public void warn(String message, Throwable ex) {
		logger.warn(message, ex);
	}

	@Override
	public void error(String message, Throwable ex) {
		logger.error(message, ex);
	}

	@Override
	public void info(String message, Throwable ex) {
		logger.info(message, ex);
	}

}
