package org.n52.android.data;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import android.util.Log;


public class DataSourceHandler {
	public @interface DataSource{
		String value();
	}
	
	public void isannot(Class o){
		for(Field f : o.getFields()){
			if(f.isAnnotationPresent(DataSource.class))
				Log.d("sollte", "so passen");
		}
		Package.getPackage("org.n52.android.data.noise");
	}
	
	private Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
		
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()){
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		
		ArrayList<Class> classes = new ArrayList<Class>();
		for(File directory : dirs){
			classes.addAll(findClasses(directory, packageName));
		}
		
		return classes.toArray(new Class[classes.size()]);
	}
	
	
	private List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		// if directory not exists
		if(!directory.exists())
			return classes;
		
		File[] files = directory.listFiles();
		for(File file : files){
			if(file.isDirectory()){
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file, packageName + "." + file.getName()));
			}
			else if (file.getName().endsWith(".class")){
				classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
	}
}
