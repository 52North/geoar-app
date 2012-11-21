package org.n52.android.view.map;

import java.lang.reflect.Field;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;

import android.app.Activity;
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
			// The private _final_ method getApplication has to return the
			// fragment's activity's application, so use reflection to get
			// the mApplication field and change it to that value
			Field applicationField = Activity.class
					.getDeclaredField("mApplication"); // Could change...
			applicationField.setAccessible(true);
			applicationField.set(this, activity.getApplication());

		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
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
}