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

import org.n52.geoar.newdata.Annotations.Settings.NotNull;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

public class StringSettingsViewField extends EditText implements SettingsViewField<String> {

	private boolean notNull;
	private Field field;

	public StringSettingsViewField(Context context, Field field, int inputType) {
		super(context);
		this.field = field;

		// Check annotations
		notNull = field.isAnnotationPresent(NotNull.class);

		setInputType(inputType);
	}

	@Override
	public boolean validate() {
		if (getText().toString().isEmpty() && notNull) {
			setError("Field is required");
			return false;
		}
		return true;
	}

	@Override
	public String getValue() {
		if (getText().toString().isEmpty() && !notNull)
			return null;

		return getText().toString();
	}

	protected String parseString(String value) {
		return value;
	}

	@Override
	public void setValue(String value) {
		setText(value != null ? value + "" : "");
	}

	@Override
	public void setValueObject(Object object) {
		setValue(object.toString());
	}

	@Override
	public View getView() {
		return this;
	}

	@Override
	public Field getField() {
		return field;
	}
}
