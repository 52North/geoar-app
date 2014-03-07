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
import java.util.Calendar;

import org.n52.geoar.newdata.Annotations.Settings.NotNull;
import org.n52.geoar.newdata.Annotations.Settings.Temporal;
import org.n52.geoar.newdata.Annotations.Settings.Temporal.TemporalType;
import org.n52.geoar.R;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TimePicker;

public abstract class DateTimeSettingsViewField<T> implements SettingsViewField<T>,
		OnDateSetListener, OnTimeSetListener {

	private EditText dateEditText;
	private boolean notNull;
	private TemporalType temporalType;
	private Calendar selectedValue;
	private EditText timeEditText;
	private Context context;
	private View filterView;
	private Field field;

	public DateTimeSettingsViewField(final Context context, Field field,
			TemporalType temporalType) {
		this.temporalType = temporalType;
		this.field = field;

		// Check annotations
		notNull = field.isAnnotationPresent(NotNull.class);

		if (temporalType == null && field.isAnnotationPresent(Temporal.class)) {
			this.temporalType = field.getAnnotation(Temporal.class).value();
		}

		if (this.temporalType == null)
			this.temporalType = TemporalType.DATETIME; // Apply default behavior
														// if temporal type
														// is unknown

		// Create Views

		if (this.temporalType == TemporalType.DATE
				|| this.temporalType == TemporalType.DATETIME) {
			// Date view required

			dateEditText = new EditText(context, null,
					android.R.attr.spinnerStyle);
			// EditText which looks like a spinner to visually indicate that
			// there
			// are more options on click

			dateEditText.setHint(R.string.date);
			dateEditText.setInputType(InputType.TYPE_NULL);
			dateEditText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (selectedValue == null)
						selectedValue = Calendar.getInstance();

					DatePickerDialog dialog = new DatePickerDialog(context,
							DateTimeSettingsViewField.this, selectedValue
									.get(Calendar.YEAR), selectedValue
									.get(Calendar.MONTH), selectedValue
									.get(Calendar.DAY_OF_MONTH));
					dialog.show();
				}
			});
		}

		if (this.temporalType == TemporalType.TIME
				|| this.temporalType == TemporalType.DATETIME) {
			// Time view required

			timeEditText = new EditText(context, null,
					android.R.attr.spinnerStyle);
			// EditText which looks like a spinner to visually indicate that
			// there
			// are more options on click

			timeEditText.setHint(R.string.time);
			timeEditText.setInputType(InputType.TYPE_NULL);
			timeEditText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (selectedValue == null)
						selectedValue = Calendar.getInstance();

					TimePickerDialog dialog = new TimePickerDialog(context,
							DateTimeSettingsViewField.this, selectedValue
									.get(Calendar.HOUR_OF_DAY), selectedValue
									.get(Calendar.MINUTE), true);
					dialog.show();
				}
			});
		}

		switch (this.temporalType) {
		case DATETIME:
			LinearLayout linearLayout = new LinearLayout(context);
			linearLayout.addView(dateEditText, new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
			linearLayout.addView(timeEditText, new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1));
			filterView = linearLayout;
			break;
		case TIME:
			filterView = timeEditText;
			break;
		case DATE:
			filterView = dateEditText;
			break;
		}

		this.context = context;
	}

	@Override
	public boolean validate() {
		if (notNull && selectedValue == null) {
			if (timeEditText != null)
				timeEditText.setError("Field is required");
			if (dateEditText != null)
				dateEditText.setError("Field is required");
			return false;
		}
		return true;
	}

	public Calendar getSelectedValue() {
		return selectedValue;
	}

	public void setSelectedValue(Calendar value) {
		selectedValue = value != null ? (Calendar) value.clone() : null;
		updateViews();
	}

	@Override
	public View getView() {
		return filterView;
	}

	@Override
	public void onDateSet(DatePicker view, int year, int month, int day) {
		if (selectedValue == null)
			selectedValue = Calendar.getInstance();
		selectedValue.set(year, month, day);
		updateViews();
	}

	@Override
	public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
		if (selectedValue == null)
			selectedValue = Calendar.getInstance();

		selectedValue.set(Calendar.HOUR_OF_DAY, hourOfDay);
		selectedValue.set(Calendar.MINUTE, minute);
		updateViews();
	}

	private void updateViews() {
		if (timeEditText != null)
			timeEditText.setText(selectedValue != null ? DateFormat
					.getTimeFormat(context).format(selectedValue.getTime())
					: "");

		if (dateEditText != null)
			dateEditText.setText(selectedValue != null ? DateFormat
					.getDateFormat(context).format(selectedValue.getTime())
					: "");
	}

	@Override
	public Field getField() {
		return field;
	}
}
