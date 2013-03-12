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

import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;

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
