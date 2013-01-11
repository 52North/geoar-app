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
package org.n52.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.n52.android.newdata.PluginLogger;
import org.n52.android.utils.DataSourceLoggerFactory;
import org.n52.android.utils.DataSourceLoggerFactory.LoggerCallable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class GeoARApplication extends Application {
	private static Logger LOG;

	public static Context applicationContext;

	public static final String PREFERENCES_FILE = "GeoAR";
	public static final String STACKTRACE_FILENAME = "stacktrace.log";
	public static final String LOG_FILENAME = "logfile.log";
	public static final String REPORT_EMAIL = "h.hopmann@52north.org";

	private UncaughtExceptionHandler defaultUncaughtExceptionHandler;

	private static File stacktraceFile;
	private static File logFile;

	@Override
	public void onCreate() {
		applicationContext = getApplicationContext();
		stacktraceFile = new File(applicationContext.getFilesDir(),
				STACKTRACE_FILENAME);
		logFile = new File(applicationContext.getFilesDir()
				+ "/logs/logfile.log");
		System.setProperty("LOG_FILE", logFile.getAbsolutePath());

		// Do not use LoggerFactory before this point
		LOG = LoggerFactory.getLogger(GeoARApplication.class);

		defaultUncaughtExceptionHandler = Thread
				.getDefaultUncaughtExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				LOG.error("Uncaught exception in thread " + thread.getName(),
						ex);
				try {
					FileOutputStream fos = openFileOutput(STACKTRACE_FILENAME,
							Context.MODE_PRIVATE);
					ex.printStackTrace(new PrintStream(fos));
					fos.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

				// fall back to standard handler
				if (defaultUncaughtExceptionHandler != null) {
					defaultUncaughtExceptionHandler.uncaughtException(thread,
							ex);
				}
			}
		});
		DataSourceLoggerFactory.setLoggerCallable(new LoggerCallable() {
			@Override
			public org.n52.android.utils.DataSourceLoggerFactory.Logger call(
					Class<?> clazz) {
				return new PluginLogger(clazz);
			}
		});

		super.onCreate();
	}

	/**
	 * Checks if there is a recorded stacktrace
	 */
	public static boolean checkAppFailed() {
		return stacktraceFile.exists();
	}

	/**
	 * Deletes latest stacktrace record
	 */
	public static void clearAppFailed() {
		if (!checkAppFailed())
			return;

		new File(applicationContext.getFilesDir(), STACKTRACE_FILENAME)
				.delete();
	}

	/**
	 * Creates an {@link Intent#ACTION_SEND_MULTIPLE} Intent to create an email
	 * with last stacktrace and current logfiles as attachments.
	 * 
	 * @param context
	 */
	public static void sendFailMail(Activity context) {
		Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, "GeoAR Error Report");
		intent.putExtra(Intent.EXTRA_TEXT,
				applicationContext.getString(R.string.text_error_report));
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] { REPORT_EMAIL });

		List<File> attachments = new ArrayList<File>();
		if (checkAppFailed()) {
			attachments.add(stacktraceFile);
		}

		if (logFile.exists()) {
			attachments.add(logFile);
		}

		try {
			intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,
					getFailAttachments(attachments));
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(context, R.string.could_not_find_email_application,
					Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Copies specified {@link File}s to globally readable locations and returns
	 * their {@link Uri}s
	 * 
	 * @param attachments
	 * @return
	 * @throws IOException
	 */
	private static ArrayList<Uri> getFailAttachments(List<File> attachments)
			throws IOException {
		ArrayList<Uri> copiesUris = new ArrayList<Uri>(attachments.size());
		for (int i = 0, attachmentsSize = attachments.size(); i < attachmentsSize; i++) {

			InputStream inputStream = new FileInputStream(attachments.get(i));
			File fileCopy = new File(
					applicationContext.getExternalFilesDir(null), "fail_" + i);
			copiesUris.add(Uri.fromFile(fileCopy));
			OutputStream outputStream = new FileOutputStream(fileCopy);

			byte[] buf = new byte[4096];
			int len;
			while ((len = inputStream.read(buf)) > 0) {
				outputStream.write(buf, 0, len);
			}
			inputStream.close();
			outputStream.close();
		}

		return copiesUris;
	}
}
