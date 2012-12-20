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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.n52.android.GeoARApplication;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.WindowManager;

/**
 * A custom {@link Context} wrapping an existing Context, but returning a
 * special {@link Resources} instance reflecting the assets of a
 * {@link InstalledPluginHolder} as {@link Context#getResources()} return value.
 * 
 * Other relevant methods are overridden to return values reflecting a single
 * plugin instead of the wrapped Context.
 */
public class PluginContext extends ContextWrapper {
	private LayoutInflater mLayoutInflater;
	private Theme mTheme;
	private Resources mPluginResources;
	private InstalledPluginHolder mPlugin;

	/**
	 * Constructs a {@link Resources} instance by creating an
	 * {@link AssetManager} linked explicitly to this custom plugin using
	 * reflection to access the non-code assets of this plugin without relying
	 * on {@link Context#getResources()}, which is not available for manually
	 * loaded application packages
	 * 
	 * @return
	 */
	@Override
	public Resources getResources() {
		if (mPluginResources == null) {

			Constructor<AssetManager> constructor;
			try {
				// For unknown reasons, the AssetManager constructor is not
				// public
				constructor = AssetManager.class.getConstructor();
				constructor.setAccessible(true);
				AssetManager assetManager = constructor.newInstance();

				// For unknown reasons, the addAssetPath method is not public
				Method method = AssetManager.class.getMethod("addAssetPath",
						String.class);
				method.setAccessible(true);
				method.invoke(assetManager, mPlugin.getPluginFile()
						.getAbsolutePath());

				// get display metrics
				WindowManager windowManager = (WindowManager) GeoARApplication.applicationContext
						.getSystemService(Context.WINDOW_SERVICE);
				DisplayMetrics displayMetrics = new DisplayMetrics();
				windowManager.getDefaultDisplay().getMetrics(displayMetrics);

				// Create Resources object, no Configuration object required
				mPluginResources = new Resources(assetManager, displayMetrics,
						null);
			} catch (NoSuchMethodException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return mPluginResources;
	}

	public PluginContext(Context base, InstalledPluginHolder plugin) {
		super(base);

		this.mPlugin = plugin;
	}

	@Override
	public Object getSystemService(String name) {
		if (LAYOUT_INFLATER_SERVICE.equals(name)) {
			if (mLayoutInflater == null) {
				// Get and store a LayoutInflater for this special
				// plugin context and do not use the cached inflater
				mLayoutInflater = LayoutInflater.from(getBaseContext())
						.cloneInContext(this);
			}
			return mLayoutInflater;
		}
		return super.getSystemService(name);
	}

	@Override
	public String getPackageResourcePath() {
		return mPlugin.getPluginFile().getAbsolutePath();
	}

	@Override
	public ClassLoader getClassLoader() {
		return mPlugin.getPluginClassLoader();
	}

	@Override
	public Theme getTheme() {
		if (mTheme == null) {
			// Create special theme to include the custom Resources
			// instance for a plugin
			mTheme = getResources().newTheme();
			mTheme.setTo(getBaseContext().getTheme());
		}
		return mTheme;
	}

	@Override
	public String getPackageCodePath() {
		return mPlugin.getPluginFile().getAbsolutePath();
	}

}
