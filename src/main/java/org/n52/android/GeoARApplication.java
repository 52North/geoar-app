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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;
import android.content.Context;

public class GeoARApplication extends Application {
	private static final Logger LOG = LoggerFactory
			.getLogger(GeoARApplication.class);

	public static Context applicationContext;

	public static final String PREFERENCES_FILE = "GeoAR";

	private UncaughtExceptionHandler defaultUncaughtExceptionHandler;

	@Override
	public void onCreate() {
		applicationContext = getApplicationContext();
		defaultUncaughtExceptionHandler = Thread
				.getDefaultUncaughtExceptionHandler();

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {

			@Override
			public void uncaughtException(Thread thread, Throwable ex) {
				LOG.error("Uncaught exception in thread " + thread.getName(),
						ex);
				try {
					FileOutputStream fos = openFileOutput("stack.trace",
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
		super.onCreate();
	}
}
