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
package org.n52.geoar.map.view;

import java.lang.reflect.Field;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.Resources.Theme;

/**
 * Special {@link MapActivity} extension which wraps an existing
 * {@link Activity} and passes all relevant {@link Context} calls to the
 * implementations of that activity. This activity has to make sure that
 * {@link MapActivityContext#destroy()}, {@link MapActivityContext#resume()} and
 * {@link MapActivityContext#pause()} get called.
 * 
 * This allows to safely use a {@link MapView} even if it is not added to a
 * {@link MapActivity}.
 * 
 * @author Holger Hopmann
 * 
 */
public class MapActivityContext extends MapActivity {

	private final Activity activity;

	public MapActivityContext(Activity activity) {
		this.activity = activity;

		try {
			// try to set base context as passed activity for the case of calls
			// to getters which were not overridden explicitly by require mBase field
			attachBaseContext(activity);
		} catch (IllegalStateException e) {

		}

		try {

			// The private _final_ method getApplication has to return the
			// fragment's activity's application, so use reflection to get
			// the mApplication field and change it to that value
			Field applicationField = Activity.class
					.getDeclaredField("mApplication"); // Could change...
			applicationField.setAccessible(true);
			applicationField.set(this, activity.getApplication());

		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public void destroy() {
		onDestroy();
	}

	public void pause() {
		onPause();
	}

	public void resume() {
		onResume();
	}

	@Override
	public Resources getResources() {
		return activity.getResources();
	}

	@Override
	public ApplicationInfo getApplicationInfo() {
		return activity.getApplicationInfo();
	}

	@Override
	public Theme getTheme() {
		return activity.getTheme();
	}

	@Override
	public Context getBaseContext() {
		return activity.getBaseContext();
	}

	@Override
	public Object getSystemService(String name) {
		return activity.getSystemService(name);
	}

	@Override
	public SharedPreferences getSharedPreferences(String name, int mode) {
		return activity.getSharedPreferences(name, mode);
	}

	@Override
	public ContentResolver getContentResolver() {
		return activity.getContentResolver();
	}
}