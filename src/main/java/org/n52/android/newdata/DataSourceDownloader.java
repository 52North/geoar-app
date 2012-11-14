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
package org.n52.android.newdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.AsyncTask.Status;

public class DataSourceDownloader {

	public interface OnDataSourceResultListener {
		void onDataSourceResult(List<DataSourceDownloadHolder> dataSources);
	}

	public static class DataSourceDownloadHolder {
		// TODO
		String identification;
		private String name;
		protected long version;
		protected String description;
		protected String downloadLink;
		protected String imageLink;

		public String getName() {
			return name;
		}

	}

	private static DefaultHttpClient mHttpClient;

	static {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		registry.register(new Scheme("https", SSLSocketFactory
				.getSocketFactory(), 443));

		HttpParams httpParameters = new BasicHttpParams();
		HttpConnectionParams.setSoTimeout(httpParameters, 10000);
		HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
		ClientConnectionManager cm = new ThreadSafeClientConnManager(
				httpParameters, registry);
		mHttpClient = new DefaultHttpClient(cm, httpParameters);
	}

	private static final String SERVER_URL = "http://geoviqua.dev.52north.org/geoar/codebase";

	private static final String CODEBASE_ID = "id";
	private static final String CODEBASE_NAME = "name";
	private static final String CODEBASE_DESCRIPTION = "description";
	private static final String CODEBASE_IMAGELINK = "imageLink";
	private static final String CODEBASE_DOWNLOADLINK = "downloadLink";
	private static final String CODEBASE_VERSION = "version";

	private static Set<OnDataSourceResultListener> mCurrentListeners = new HashSet<OnDataSourceResultListener>();

	private static List<DataSourceDownloadHolder> mDownloadableDataSources = new ArrayList<DataSourceDownloadHolder>();

	private static AsyncTask<Void, Void, Void> mDownloadTask = new AsyncTask<Void, Void, Void>() {

		@Override
		protected void onPostExecute(Void result) {
			for (OnDataSourceResultListener listener : mCurrentListeners) {
				listener.onDataSourceResult(mDownloadableDataSources);
			}
			mCurrentListeners.clear();
		}

		@Override
		protected Void doInBackground(Void... params) {
			mDownloadableDataSources = new ArrayList<DataSourceDownloadHolder>();

			HttpGet request = new HttpGet(SERVER_URL);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-Type", "application/json");

			try {
				HttpResponse response = mHttpClient.execute(request);

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// convert Stream to String
					String res = convertStreamToString(entity.getContent());
					// create JSONObject
					JSONObject json = new JSONObject(res);
					// parse JSOBObject
					JSONArray datasourceArray = json
							.getJSONArray("datasources");
					for (int i = 0; i < datasourceArray.length(); i++) {
						DataSourceDownloadHolder holder = new DataSourceDownloadHolder();
						JSONObject currentObject = datasourceArray
								.getJSONObject(i);

						holder.identification = currentObject
								.getString(CODEBASE_ID);
						holder.name = currentObject.getString(CODEBASE_NAME);
						holder.version = currentObject
								.getLong(CODEBASE_VERSION);
						holder.description = currentObject
								.getString(CODEBASE_DESCRIPTION);
						holder.downloadLink = currentObject
								.getString(CODEBASE_DOWNLOADLINK);
						holder.imageLink = currentObject
								.getString(CODEBASE_IMAGELINK);

						mDownloadableDataSources.add(holder);
					}
				}
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

	};

	private static String convertStreamToString(InputStream is)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null)
			sb.append(line);
		reader.close();
		return sb.toString();
	}

	public static void getDataSources(OnDataSourceResultListener listener) {
		if (mDownloadTask.getStatus() != Status.FINISHED) {
			mCurrentListeners.add(listener);

			if (mDownloadTask.getStatus() != Status.RUNNING) {
				mDownloadTask.execute((Void) null);
			}
		} else {
			listener.onDataSourceResult(mDownloadableDataSources);
		}
	}

}
