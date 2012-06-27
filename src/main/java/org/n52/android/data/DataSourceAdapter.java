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
