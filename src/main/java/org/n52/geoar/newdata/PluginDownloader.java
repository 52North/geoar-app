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
import org.n52.geoar.GeoARApplication;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;

/**
 * 
 * @author Arne de Wall <a.dewall@52North.org>
 *
 */
public class PluginDownloader {

	public interface OnDataSourceResultListener {
		void onDataSourceResult(List<PluginDownloadHolder> dataSources);
	}

	private static DefaultHttpClient mHttpClient;
	private static DownloadManager mDownloadManager;
	private static List<Long> currentDownloads = new ArrayList<Long>(0);

	private static BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			synchronized (currentDownloads) {
				if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)
						&& !currentDownloads.isEmpty()) {
					// long downloadId = intent.getLongExtra(
					// DownloadManager.EXTRA_DOWNLOAD_ID, 0);
					Query query = new Query();

					long[] downloads = new long[currentDownloads.size()];
					for (int i = 0, len = currentDownloads.size(); i < len; i++) {
						downloads[i] = currentDownloads.get(i);
					}
					query.setFilterById(downloads);
					query.setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL);
					Cursor cursor = mDownloadManager.query(query);
					boolean pluginAdded = false;
					if (cursor.moveToFirst()) {
						int columnId = cursor
								.getColumnIndex(DownloadManager.COLUMN_ID);
						do {
							currentDownloads.remove(cursor.getLong(columnId));
							pluginAdded = true;
						} while (cursor.moveToNext());
						if (pluginAdded) {
							PluginLoader.reloadPlugins();
						}
					}

				}
			}
		}
	};

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

		mDownloadManager = (DownloadManager) GeoARApplication.applicationContext
				.getSystemService(Context.DOWNLOAD_SERVICE);
		GeoARApplication.applicationContext.registerReceiver(broadcastReceiver,
				new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	}

	// TODO externalize string
	static final String SERVER_URL = "http://geoviqua.dev.52north.org/geoar/codebase";

	private static final String CODEBASE_ID = "id";
	private static final String CODEBASE_NAME = "name";
	private static final String CODEBASE_DESCRIPTION = "description";
	private static final String CODEBASE_VERSION = "version";
	private static final String CODEBASE_PUBLISHER = "publisher";

	private static Set<OnDataSourceResultListener> mCurrentListeners = new HashSet<OnDataSourceResultListener>();

	private static Set<PluginDownloadHolder> mDownloadableDataSources = new HashSet<PluginDownloadHolder>();

	private static class DownloadTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPostExecute(Void result) {
			List<PluginDownloadHolder> resultList = new ArrayList<PluginDownloadHolder>();
			resultList.addAll(mDownloadableDataSources);
			for (OnDataSourceResultListener listener : mCurrentListeners) {
				listener.onDataSourceResult(resultList);
			}
			mCurrentListeners.clear();
		}

		@Override
		protected Void doInBackground(Void... params) {
			// TODO will not reflect removal of remote data sources

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
						PluginDownloadHolder holder = new PluginDownloadHolder();
						JSONObject currentObject = datasourceArray
								.getJSONObject(i);

						holder.setIdentifier(currentObject
								.getString(CODEBASE_ID));
						holder.setName(currentObject.getString(CODEBASE_NAME));
						holder.setVersion(currentObject
								.getLong(CODEBASE_VERSION));
						holder.setDescription(currentObject
								.getString(CODEBASE_DESCRIPTION));
						holder.setPublisher(currentObject
								.getString(CODEBASE_PUBLISHER));

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

	private static DownloadTask mDownloadTask = null;

	private static String convertStreamToString(InputStream is)
			throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();

		String line = null;
		while ((line = reader.readLine()) != null)
			sb.append(line);
		reader.close();
		is.close();
		return sb.toString();
	}

	public static void getDataSources(OnDataSourceResultListener listener,
			boolean force) {
		if (mDownloadTask == null
				|| mDownloadTask.getStatus() != Status.FINISHED || force) {
			mCurrentListeners.add(listener);

			if (mDownloadTask == null
					|| mDownloadTask.getStatus() == Status.FINISHED) {
				mDownloadTask = new DownloadTask();
				mDownloadTask.execute((Void) null);
			}
		} else {
			listener.onDataSourceResult(new ArrayList<PluginDownloadHolder>(
					mDownloadableDataSources));
		}
	}

	public static PluginHolder getPluginByIdentifier(String identifier) {
		for (PluginDownloadHolder plugin : mDownloadableDataSources) {
			if (plugin.getIdentifier().equals(identifier)) {
				return plugin;
			}
		}
		return null;
	}

	public static void getDataSources(OnDataSourceResultListener listener) {
		getDataSources(listener, false);
	}

	public static void downloadPlugin(PluginDownloadHolder dataSource) {

		Request request = new DownloadManager.Request(
				dataSource.getDownloadLink());
		request.setDestinationInExternalFilesDir(
				GeoARApplication.applicationContext, null,
				dataSource.getIdentifier() + ".apk");
		request.setTitle("GeoAR Data Souce Download");
		request.setMimeType("application/vnd.52north.datasources");
		request.setDescription(dataSource.getName());
		currentDownloads.add(mDownloadManager.enqueue(request));
	}

}
