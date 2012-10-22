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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.n52.android.data.PluginLoader.AddPluginCallback;
import org.n52.android.data.PluginLoader.PluginHolder;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.ImageView;

/**
 * 
 * @author Arne de Wall
 * 
 */
public final class CodebaseDownloader {

    private static final String SERVER_URL = "http://geoviqua.dev.52north.org/geoar/codebase";
    private static final String EXT_PATH = Environment.getExternalStorageDirectory() + "/GeoAR/";
    private static DefaultHttpClient httpClient;

    static {
	initHttpClient();
    }

    private static void initHttpClient() {
	SchemeRegistry registry = new SchemeRegistry();
	registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

	HttpParams httpParameters = new BasicHttpParams();
	HttpConnectionParams.setSoTimeout(httpParameters, 10000);
	HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
	ClientConnectionManager cm = new ThreadSafeClientConnManager(httpParameters, registry);
	httpClient = new DefaultHttpClient(cm, httpParameters);
    }

    protected class CodebaseHolder extends PluginHolder {
	String name;
	String imageLink;
	String downloadLink;
    }

    public static final String CODEBASE_ID = "id";
    public static final String CODEBASE_NAME = "name";
    public static final String CODEBASE_DESCRIPTION = "description";
    public static final String CODEBASE_IMAGELINK = "imageLink";
    public static final String CODEBASE_DOWNLOADLINK = "downloadLink";
    public static final String CODEBASE_VERSION = "version";

    public void availableDatasources(final AddPluginCallback callback) {

	new AsyncTask<String, CodebaseHolder, Void>() {

	    /*
	     * (non-Javadoc)
	     * 
	     * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	     */
	    @Override
	    protected void onProgressUpdate(CodebaseHolder... holder) {
		callback.addPlugin(holder[0]);
	    }

	    @Override
	    protected void onPostExecute(Void result) {
		callback.updateListener();
		super.onPostExecute(result);
	    }

	    @Override
	    protected Void doInBackground(String... params) {
		try {
		    HttpGet request = new HttpGet(SERVER_URL);
		    request.setHeader("Accept", "application/json");
		    request.setHeader("Content-Type", "application/json");
		    HttpResponse response = httpClient.execute(request);

		    HttpEntity entity = response.getEntity();
		    if (entity != null) {
			// read JSON response
			InputStream inStream = entity.getContent();
			// convert Stream to String
			String res = convertStreamToString(inStream);
			// create JSONObject
			JSONObject json = new JSONObject(res);
			// parse JSOBObject
			JSONArray datasourceArray = json.getJSONArray("datasources");
			for (int i = 0; i < datasourceArray.length(); i++) {
			    CodebaseHolder holder = new CodebaseHolder();
			    JSONObject currentObject = datasourceArray.getJSONObject(i);

			    holder.identification = currentObject.getString(CODEBASE_ID);
			    holder.name = currentObject.getString(CODEBASE_NAME);
			    holder.version = currentObject.getLong(CODEBASE_VERSION);
			    holder.description = currentObject.getString(CODEBASE_DESCRIPTION);
			    holder.downloadLink = currentObject.getString(CODEBASE_DOWNLOADLINK);
			    holder.imageLink = currentObject.getString(CODEBASE_IMAGELINK);
			    // callback.addPlugin(holder);
			    publishProgress(holder);
			}
		    }
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (JSONException e) {
		    e.printStackTrace();
		}

		return null;
	    }
	}.execute("not needed ;)");
    }

    /**
     * Downloader method for the Image of the datasource
     */
    public static void downloadDatasourceImage(final ImageView imageView, final String downloadUrl) {

	class AsyncDownloader extends AsyncTask<String, Void, Bitmap> {

	    @Override
	    protected Bitmap doInBackground(String... urls) {
		return downloadDatasourceImage(urls[0]);
	    }

	    @Override
	    protected void onPostExecute(Bitmap result) {
		super.onPostExecute(result);
		imageView.setImageBitmap(result);
	    }
	}

	(new AsyncDownloader()).execute(downloadUrl);
    }

    public static Bitmap downloadDatasourceImage(final String url) {
	try {
	    HttpGet request = new HttpGet(url);
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setSoTimeout(params, 60000);
	    request.setParams(params);
	    HttpResponse response = httpClient.execute(request);
	    byte[] image = EntityUtils.toByteArray(response.getEntity());
	    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

	    return bitmap;
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * Downloader method for the Image of the datasource
     * 
     * @param urls
     */
    protected static Bitmap downloadImage(String... urls) {
	try {
	    HttpGet request = new HttpGet(urls[0]);
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setSoTimeout(params, 60000);
	    request.setParams(params);
	    HttpResponse response = httpClient.execute(request);
	    byte[] image = EntityUtils.toByteArray(response.getEntity());
	    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);

	    return bitmap;
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return null;
    }

    /**
     * Downloader method for the datasource
     * 
     * @param urls
     */
    protected static void downloadDatasource(String fileName, String... urls) {
	File file = new File(EXT_PATH + fileName);
	try {
	    HttpGet request = new HttpGet(urls[0]);
	    HttpParams params = new BasicHttpParams();
	    HttpConnectionParams.setSoTimeout(params, 60000);
	    request.setParams(params);
	    HttpResponse response = httpClient.execute(request);

	    HttpEntity entity = response.getEntity();
	    if (entity != null) {
		BufferedInputStream bis = new BufferedInputStream(entity.getContent());
		ByteArrayBuffer baf = new ByteArrayBuffer(50);
		int current = 0;
		while ((current = bis.read()) != -1) {
		    baf.append((byte) current);
		}

		FileOutputStream fos = new FileOutputStream(file);
		fos.write(baf.toByteArray());
		fos.close();
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static String convertStreamToString(InputStream is) {
	BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	StringBuilder sb = new StringBuilder();

	String line = null;
	try {
	    while ((line = reader.readLine()) != null)
		sb.append(line + "\n");
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    try {
		is.close();
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	}
	return sb.toString();
    }
}
