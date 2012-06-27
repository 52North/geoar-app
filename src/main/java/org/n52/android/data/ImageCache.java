package org.n52.android.data;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;

public class ImageCache {

	private Map<String, Bitmap> cache = Collections.synchronizedMap(
			new LinkedHashMap<String, Bitmap>(10, 1.5f, true));
	private long size = 0;
	private long limit;
	
	public ImageCache(){
		limit = Runtime.getRuntime().maxMemory()/6;
	}
	
	public Bitmap getBitmap(String id){
		try{
			if(!cache.containsKey(id))
				return null;
			return cache.get(id);
		} catch(NullPointerException e){
			return null;
		}
	}
	
	public void put(String id, Bitmap bitmap){
		try{
			if(cache.containsKey(id))
				// image allready in cache,
				size -= getByteSizeOfBitmap(cache.get(id));
			size += getByteSizeOfBitmap(bitmap);
			cache.put(id, bitmap);
			checkCache();
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private long getByteSizeOfBitmap(Bitmap bitmap){
		if(bitmap == null)
			return 0;
		// multiplication of count of rowbytes with the bitmap height
		return bitmap.getRowBytes() * bitmap.getHeight();
	}
	
	/**
	 * checks the Cache size. If it is over the limit, then kill images
	 * until the size fits. Afterwards tell the VM that it would be a 
	 * good time to run the garbage collector.
	 */
	private void checkCache(){
		if(size>limit){
			Iterator<Entry<String, Bitmap>> i = cache.entrySet().iterator();
			while(i.hasNext()){
				Entry<String, Bitmap> entry = i.next();
				size -= getByteSizeOfBitmap(entry.getValue());
				i.remove();
				if(size <= limit)
					break;
			}
			// call garbage collection
			System.gc();
		}
	}
}
