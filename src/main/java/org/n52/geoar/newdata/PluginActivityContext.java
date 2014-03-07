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

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * Context to wrap a {@link PluginContext} while extending it by a specific
 * activity context. Using this wrapped context will ensure that requests for
 * {@link WindowManager}s return the window service of the supplied activity
 * context, whereas all other requests are handled by the {@link PluginContext}
 * resulting in plugin-specific resources etc. This is required for the creation
 * of some {@link View}s within plugins on Android 2.3.3.
 */

// TODO XXX Maybe better/safer/cleaner to wrap activity context directly but return plugin
// context
// resources?
public class PluginActivityContext extends ContextWrapper {

	private Context mActivityContext;
	private LayoutInflater mLayoutInflater;

	public PluginActivityContext(Context base, Context activity) {
		super(base);
		mActivityContext = activity;
	}

	@Override
	public Object getSystemService(String name) {
		if (LAYOUT_INFLATER_SERVICE.equals(name)) {
			if (mLayoutInflater == null) {
				// Even create own LayoutInflater, since base context's inflater
				// will point to the base context, not this wrapper
				// Get and store a LayoutInflater for this special
				// plugin context and do not use the cached inflater
				mLayoutInflater = LayoutInflater.from(getBaseContext())
						.cloneInContext(this);
			}
			return mLayoutInflater;
		} else if (WINDOW_SERVICE.equals(name)) {
			return mActivityContext.getSystemService(name);
		}
		return super.getSystemService(name);
	}

}
