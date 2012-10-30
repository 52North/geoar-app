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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.n52.android.geoar.R;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

/**
 * 
 * @author Arne de Wall
 *
 */
public class ImageLoader {
	
	private static final String 	PLUGIN_PATH = Environment
			.getExternalStorageDirectory() + "/GeoAR/";
		
	private class AsyncLoader implements Runnable{
		final String id;
		final String url;
		final ImageView imageView;
		
		AsyncLoader(String id, String url, ImageView imageView){
			this.id = id;
			this.imageView = imageView;
			this.url = url;
		}
		
		@Override
		public void run() {
			String vID = imageViews.get(imageView);
			if(vID == null || !vID.equals(id))
				return;
			Bitmap bitmap = loadBitmap(id, url);
			Activity activity = (Activity) imageView.getContext();
			activity.runOnUiThread(new AsyncMainthreadDisplay(id, bitmap, imageView));
		}
	}
	
	private class AsyncMainthreadDisplay implements Runnable{
		Bitmap bitmap;
		String id;
		ImageView imageView;
		
		public AsyncMainthreadDisplay(String id, Bitmap bitmap, ImageView imageView){
			this.bitmap = bitmap;
			this.id = id;
			this.imageView = imageView;
		}
		
		@Override
		public void run() {
			String vID = imageViews.get(imageView);
			if(vID == null || !vID.equals(id))
				return;
			if(bitmap != null)
				imageView.setImageBitmap(bitmap);
			else
				imageView.setImageResource(R.drawable.icon);
		}
	}
	
	private ImageCache imageCache = new ImageCache();
	private Map<ImageView, String> imageViews = Collections.synchronizedMap(
			new WeakHashMap<ImageView, String>());
	private final ExecutorService executor;
	
	private static ImageLoader instance;
	
	
	private ImageLoader(){
		executor = Executors.newFixedThreadPool(1);
	}
	
	public static ImageLoader getInstance(){
		if(instance == null){
			instance = new ImageLoader();
		}
		return instance;
	}
	
	public void displayImage(String id, ImageView imageView){
		displayImage(id, null, imageView);
	}
	
	public void displayImage(String id, String url, ImageView imageView){
		imageViews.put(imageView, id);
		Bitmap bitmap = imageCache.getBitmap(id);
		if(bitmap == null){
			submitImageQueue(id, url, imageView);
			imageView.setImageResource(R.drawable.icon);
			return;
		}
		imageView.setImageBitmap(bitmap);
	}
	
	private void submitImageQueue(String id, String url, ImageView imageView){
		executor.submit(new AsyncLoader(id, url, imageView));
	}

	private static Bitmap loadBitmapFromFile(String id){
        FileInputStream in;
        BufferedInputStream buf;
        try {
       	    in = new FileInputStream(PLUGIN_PATH + id + ".png");
       	    buf = new BufferedInputStream(in);
            Bitmap bitmap = BitmapFactory.decodeStream(buf);
            
            if (in != null) 
            	in.close();
            
            if (buf != null) 
            	buf.close();
            
            return bitmap;
        } catch (Exception e) {
            Log.e("Error reading file", e.toString());
        }
		return null;
	}
	
	private Bitmap loadBitmap(String id, String url){
		
		Bitmap bitmap = loadBitmapFromFile(id);
		if(bitmap == null && url != null){
		//	bitmap = CodebaseDownloader.downloadDatasourceImage(url);
			// TODO
		}
		imageCache.put(id, bitmap);
		return bitmap;
		
	}

}
