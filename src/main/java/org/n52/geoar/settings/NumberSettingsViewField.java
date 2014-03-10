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

import org.n52.geoar.newdata.Annotations.Settings.Max;
import org.n52.geoar.newdata.Annotations.Settings.Min;
import org.n52.geoar.newdata.Annotations.Settings.NotNull;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

public abstract class NumberSettingsViewField<T extends Number> extends EditText
		implements SettingsViewField<T> {

	private boolean notNull;
	private T minValue;
	private T maxValue;
	private Field field;

	public NumberSettingsViewField(Context context, Field field, int inputType) {
		super(context);
		this.field = field;
		// Check annotations
		notNull = field.isAnnotationPresent(NotNull.class);

		if (field.isAnnotationPresent(Min.class)) {
			minValue = parseString(field.getAnnotation(Min.class).value());
		}
		if (field.isAnnotationPresent(Max.class)) {
			maxValue = parseString(field.getAnnotation(Max.class).value());
		}
		setInputType(inputType);
	}

	@Override
	public Field getField() {
		return field;
	}

	@Override
	public boolean validate() {
		if (getText().toString().isEmpty() && !notNull) {
			return true;
		}
		T value;
		try {
			value = getValue();
		} catch (NumberFormatException e) {
			setError("Invalid Number");
			return false;
		}

		if (minValue != null && minValue.doubleValue() > value.doubleValue()) {
			setError("Must be at least " + minValue);
			return false;
		}
		if (maxValue != null && maxValue.doubleValue() < value.doubleValue()) {
			setError("Must be less than " + maxValue);
			return false;
		}

		return true;
	}

	@Override
	public T getValue() {
		if (getText().toString().isEmpty() && !notNull)
			return null;

		return parseString(getText().toString());
	}

	protected abstract T parseString(String value);

	@Override
	public void setValue(T value) {
		setText(value != null ? value + "" : "");
	}

	@Override
	public void setValueObject(Object object) {
		setValue((T) object);
	}

	@Override
	public View getView() {
		return this;
	}
}
