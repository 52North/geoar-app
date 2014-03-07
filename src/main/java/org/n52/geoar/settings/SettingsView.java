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
package org.n52.geoar.settings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.n52.geoar.newdata.Annotations.PostSettingsChanged;
import org.n52.geoar.newdata.Annotations.Setting;
import org.n52.geoar.newdata.Annotations.Settings.Group;
import org.n52.geoar.newdata.Annotations.Settings.Name;
import org.n52.geoar.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsView extends LinearLayout {

	private Object settingsObject;
	private LayoutInflater mInflater;
	// Assigning Fields to groups
	final Map<String, List<SettingsViewField<?>>> groupFieldMap = new TreeMap<String, List<SettingsViewField<?>>>();
	private Context mStringsContext;

	public SettingsView(Context context) {
		super(context);
		init();
	}

	public SettingsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	private void init() {
		mInflater = LayoutInflater.from(getContext());
		setOrientation(LinearLayout.VERTICAL);
		mStringsContext = getContext();
	}

	public void setStringsContext(Context stringsContext) {
		this.mStringsContext = stringsContext;
	}

	public void setSettingsObject(Object settingsObject) {
		this.settingsObject = settingsObject;
		updateViews();
	}

	public void updateViews() {
		removeAllViews();
		if (settingsObject == null) {
			throw new IllegalStateException();
		}

		TypedArray typedArray = getContext().obtainStyledAttributes(
				R.style.formView,
				new int[] { android.R.attr.paddingLeft,
						android.R.attr.paddingRight });
		int paddingLeft = typedArray.getDimensionPixelSize(0, 0);
		int paddingRight = typedArray.getDimensionPixelSize(1, 0);
		typedArray.recycle();

		groupFieldMap.clear();

		// Find fields and create views for every annotated field
		for (Field field : settingsObject.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Setting.class)) {
				SettingsViewField<?> filterView = SettingsHelper
						.createFilterViewFromField(field, getContext());
				if (filterView != null) {

					// get group
					String groupName = "";
					Group annotation = field.getAnnotation(Group.class);
					if (annotation != null) {
						groupName = annotation.value();
					}

					List<SettingsViewField<?>> viewList = groupFieldMap
							.get(groupName);
					if (viewList == null) {
						viewList = new ArrayList<SettingsViewField<?>>();
						groupFieldMap.put(groupName, viewList);
					}
					viewList.add(filterView);
				}
			}
		}

		// create table with labels and views for each field

		for (Entry<String, List<SettingsViewField<?>>> entry : groupFieldMap
				.entrySet()) {
			String group = entry.getKey();
			if (group != null && !group.isEmpty()) {
				// TextView groupView = new TextView(this, null,
				// R.style.formGroup);
				// Since defStyle is ignored, layout inflater is needed
				// http://code.google.com/p/android/issues/detail?id=12683
				TextView groupView = (TextView) mInflater.inflate(
						R.layout.textview_group, null);

				groupView.setText(group);
				addView(groupView, LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
			}

			List<SettingsViewField<?>> viewList = entry.getValue();

			for (SettingsViewField<?> filterView : viewList) {
				Field field = filterView.getField();
				Name nameAnnotation = field.getAnnotation(Name.class);
				if (nameAnnotation != null) {

					// TextView labelView = new TextView(this, null,
					// R.style.formLabel);
					// Since defStyle is ignored, layout inflater is needed
					// http://code.google.com/p/android/issues/detail?id=12683

					TextView labelView = (TextView) mInflater.inflate(
							R.layout.textview_label, null);
					if (nameAnnotation.resId() >= 0) {
						labelView.setText(mStringsContext
								.getString(nameAnnotation.resId()));
					} else {
						labelView.setText(nameAnnotation.value());
					}
					addView(labelView, LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT);
				}

				try {
					field.setAccessible(true);
					filterView.setValueObject(field.get(settingsObject));
				} catch (Exception e) {
					// TODO
				}

				// Set padding
				filterView.getView()
						.setPadding(paddingLeft, 0, paddingRight, 0);
				addView(filterView.getView(), LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
			}
		}

	}

	public boolean validate() {
		boolean valid = true;
		for (List<SettingsViewField<?>> viewList : groupFieldMap.values())
			for (SettingsViewField<?> filterView : viewList) {
				if (!filterView.validate()) {
					valid = false;
				}
			}

		return valid;
	}

	public void updateObject() {
		if (settingsObject == null) {
			throw new IllegalStateException();
		}
		// Set every field of settingsObject to new value
		for (List<SettingsViewField<?>> viewList : groupFieldMap.values())
			for (SettingsViewField<?> filterView : viewList) {
				try {
					Field field = filterView.getField();
					field.setAccessible(true);
					field.set(settingsObject, filterView.getValue());
				} catch (IllegalArgumentException e) {
					throw new SettingsException(e.getMessage(), e);
				} catch (IllegalAccessException e) {
					throw new SettingsException(e.getMessage(), e);
				}
			}

		SettingsHelper.notifySettingsChanged(settingsObject);
	}

	public boolean isEmpty() {
		return groupFieldMap.isEmpty();
	}

}
