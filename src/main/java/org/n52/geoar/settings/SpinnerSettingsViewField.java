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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.n52.geoar.newdata.Annotations.Settings.NoValue;
import org.n52.geoar.newdata.Annotations.Settings.NotNull;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public abstract class SpinnerSettingsViewField<T> extends Spinner implements
		SettingsViewField<T>, android.widget.AdapterView.OnItemSelectedListener {

	private String noValueText = "No Value";
	private boolean notNull = false;
	private ArrayAdapter<Object> adapter;
	private T selectedValue;

	// Special object for null values
	private Object noValueObject = new Object() {

		@Override
		public String toString() {
			return noValueText;
		}
	};
	private Field field;

	public SpinnerSettingsViewField(Context context, Field field, List<Object> items) {
		super(context);
		this.field = field;
		// Init adapter
		adapter = new ArrayAdapter<Object>(context,
				android.R.layout.simple_spinner_item, items);
		setAdapter(adapter);
		setOnItemSelectedListener(this);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Check annotations
		notNull = field.isAnnotationPresent(NotNull.class);

		if (!notNull) {
			// Null values allowed
			NoValue noValueAnnotation = field.getAnnotation(NoValue.class);
			if (noValueAnnotation != null) {
				noValueText = noValueAnnotation.value();
			}
			adapter.add(noValueObject);
		}
	}

	public SpinnerSettingsViewField(Context context, Field field, Object[] items) {
		this(context, field, new ArrayList<Object>(Arrays.asList(items)));
	}

	@Override
	public boolean validate() {
		if (notNull && selectedValue == null)
			return false;

		return true;
	}

	@Override
	public T getValue() {
		return selectedValue;
	}

	@Override
	public void setValue(T value) {
		selectedValue = value;
		if (value != null) {
			setSelection(adapter.getPosition(value));
		} else {
			setSelection(adapter.getPosition(noValueObject));
		}
	}

	@Override
	public View getView() {
		return this;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		Object item = adapter.getItem(position);
		if (noValueObject == item) {
			selectedValue = null;
		} else {
			selectedValue = (T) item;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		selectedValue = null;
	}

	@Override
	public Field getField() {
		return field;
	}

}
