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
package org.n52.android.data;

import org.n52.android.GeoARActivity;
import org.n52.android.alg.InterpolationProvider;

import android.content.Context;
import android.content.Intent;

/**
 * Static 
 * @author FYIE
 *
 */
public final class DataSourceAdapter  {
	
	private static DataSourceAbstractFactory 	factory;
	private static PluginLoader 				pluginLoader;
	private static Context						mcontext;
	
	public static void initFactoryLoader(ClassLoader classLoader, Context context){
		pluginLoader = new PluginLoader(classLoader, context);
		mcontext = context;
	}

	public static void refreshPluginLoader(){
		pluginLoader.refresh();
	}
 
	public static void startDataSource(String id){
		factory = pluginLoader.getFactoryById(id);
		InterpolationProvider.setInterpolation(factory.getInterpolationProvider());
		Intent intent = new Intent(mcontext.getApplicationContext(), GeoARActivity.class);
		mcontext.startActivity(intent);
		
	}
	public static Measurement createMeasurement() {
		return factory.createMeasurement();
	}

	public static MeasurementManager createMeasurementManager() {
		return factory.createMeasurementManager();
	}
}
