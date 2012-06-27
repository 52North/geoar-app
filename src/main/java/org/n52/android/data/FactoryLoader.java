package org.n52.android.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.n52.android.data.PluginLoader.AddPluginCallback;
import org.n52.android.data.PluginLoader.PluginHolder;

import android.content.Context;
import android.os.Environment;
import dalvik.system.DexClassLoader;

/**
 * 
 * @author Arne de Wall
 *
 */
public class FactoryLoader {
	
	private static final class OnlyAPK implements FilenameFilter{
		final String apk = ".apk";
		
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(apk);
		} 
	}
	
	class DatasourceHolder extends PluginHolder {
		DataSourceAbstractFactory factory;
		
		public DatasourceHolder(String name){
			String[] split = name.split("\\.")[0].split("\\-");
			this.identification = split[0];
			if(split.length > 1)
				version = Long.parseLong(split[1]);
		}
		
		public DatasourceHolder(String name, DataSourceAbstractFactory factory){
			this(name);
			this.factory = factory;
		}
	}
	

	private static final String 	SECONDARY_DEX_NAME 		= "52n-geoar-ds-noise-0.0.1-SNAPSHOT.jar";
	private static final String		PLUGIN_DESCRIPTION_DIR	= "assets/description";
	private static final int 		BUF_SIZE 				= 8*1024;
	private static final String 	PLUGIN_PATH 			= Environment.getExternalStorageDirectory() 
																			+ "/GeoAR/";
	private Context 		context;
	private DexClassLoader 	dexClassLoader;

	public FactoryLoader(ClassLoader classLoader, Context context){
        this.context 			= context;
	}
	
	public void reloadFactorys(AddPluginCallback callback){
		loadPluginHolder(callback);
	}
	

	private void loadPluginHolder(AddPluginCallback callback){
		final File tmpDir = context.getDir("dex", 0);

		String[] apksInDirectory = (new File(PLUGIN_PATH).list(new OnlyAPK()));
//		final String[] pluginFileNames = getAllPluginDirectorys();
		if(apksInDirectory.length == 0)
			throw new RuntimeException("No Datasource APKs Found in GeoAR directory");
		
		try{
			for(String pluginFileName : apksInDirectory){
				String pluginDirectory = PLUGIN_PATH + pluginFileName;
				// Check if the Plugin exists 
				assert (new File(pluginDirectory)).exists() : "Directory not Found: " + pluginDirectory;
				
				// create ClassLoader
				dexClassLoader = new DexClassLoader(pluginDirectory, tmpDir.getAbsolutePath(), 
						null, this.getClass().getClassLoader());
				
				// Check if Plugin Description exists
				if(dexClassLoader.getResource(PLUGIN_DESCRIPTION_DIR) == null)
					throw new FileNotFoundException(PLUGIN_DESCRIPTION_DIR + " in " + pluginDirectory + " not found");
				
				final InputStream apkDescription = dexClassLoader
						.getResourceAsStream(PLUGIN_DESCRIPTION_DIR);
				BufferedReader reader = new BufferedReader(new InputStreamReader(apkDescription));
				
				// load Plugins into cache
				String strLine; 
				while((strLine = reader.readLine()) != null){ 
		            // Cast the return object to the library interface so that the
		            // caller can directly invoke methods in the interface.
					DataSourceAbstractFactory fac = (DataSourceAbstractFactory) 
							dexClassLoader.loadClass(strLine).newInstance();
					// Add all abstract factorys to the input set
					callback.addPlugin((PluginHolder) new DatasourceHolder(pluginFileName, fac));
				}
			}
		// Handle exception gracefully here.
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private boolean prepareDex(File dexInternalSToragePath){
		BufferedInputStream 	bis = null;
		OutputStream 			dexWriter = null;
		
		try{
			bis = new BufferedInputStream(context.getAssets().open(SECONDARY_DEX_NAME));
			dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalSToragePath));
            byte[] buf = new byte[BUF_SIZE];
            int len;
            while((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                dexWriter.write(buf, 0, len); 
            }
            dexWriter.close();
            bis.close();
            return true;
        } catch (IOException e) {
            if (dexWriter != null) {
                try {
                    dexWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            return false;
        }
	}
}
