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
package org.n52.android.dialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.n52.android.data.MeasurementFilter;
import org.n52.android.data.MeasurementManager;
import org.n52.android.geoar.R;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Dialog to allow users to change measurement filter settings.
 * 
 * @author Holger Hopmann
 */
public class FilterDialog extends AlertDialog implements
		android.view.View.OnClickListener {

	// Filter gets altered in a local copy
	private MeasurementFilter tempFilter;

	// Listeners for the DateTimePicker Dialogs
	private OnDateSetListener startDateListener = new OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			tempFilter.getTimeFrom().set(year, monthOfYear, dayOfMonth);
			updateViews();
		}
	};
	private OnDateSetListener endDateListener = new OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			tempFilter.getTimeTo().set(year, monthOfYear, dayOfMonth);
			updateViews();
		}
	};
	private TextView startEditText;
	private TextView endEditText;
	private SimpleDateFormat dateFormat;

	/**
	 * Updates the views to reflect the tempFilter
	 */
	private void updateViews() {
		if (tempFilter.getTimeFrom() != null) {
			startEditText.setText(dateFormat.format(tempFilter.getTimeFrom()
					.getTime()));
		} else {
			startEditText.setText(R.string.na);
		}
		if (tempFilter.getTimeTo() != null) {
			endEditText.setText(dateFormat.format(tempFilter.getTimeTo()
					.getTime()));
		} else {
			endEditText.setText(R.string.na);
		}
	}

	public FilterDialog(Context context, final MeasurementFilter filter,
			final MeasurementManager measurementManager) {
		super(context);
		// create a copy of the filter to work on
		this.tempFilter = (MeasurementFilter) filter.clone();
		dateFormat = new SimpleDateFormat("dd.MM.yyyy");

		// Inflate Layout
		View layout = LayoutInflater.from(context).inflate(
				R.layout.filter_dialog, null);

		// Find Views
		startEditText = (TextView) layout.findViewById(R.id.textViewStartdate);
		endEditText = (TextView) layout.findViewById(R.id.textViewEnddate);
		Button startButton = (Button) layout.findViewById(R.id.buttonStartdate);
		Button endButton = (Button) layout.findViewById(R.id.buttonEnddate);
		Button deleteStartButton = (Button) layout
				.findViewById(R.id.buttonDeleteStartdate);
		Button deleteEndButton = (Button) layout
				.findViewById(R.id.buttonDeleteEnddate);

		List<String> hourValues = new ArrayList<String>();
		for (int i = 0; i <= 23; i++) {
			hourValues.add(i + ":00");
		}
		hourValues.add(context.getString(R.string.none));

		// Find Spinner
		ArrayAdapter<String> spinnerFromArrayAdapter = new ArrayAdapter<String>(
				getContext(), android.R.layout.simple_spinner_item, hourValues);
		spinnerFromArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		final Spinner hourFromSpinner = (Spinner) layout
				.findViewById(R.id.spinnerHourFrom);
		hourFromSpinner.setAdapter(spinnerFromArrayAdapter);

		ArrayAdapter<String> spinnerToArrayAdapter = new ArrayAdapter<String>(
				getContext(), android.R.layout.simple_spinner_item, hourValues);
		spinnerToArrayAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		final Spinner hourToSpinner = (Spinner) layout
				.findViewById(R.id.spinnerHourTo);
		hourToSpinner.setAdapter(spinnerToArrayAdapter);

		// Set spinner values
		hourFromSpinner
				.setSelection(tempFilter.getHourFrom() != null ? tempFilter
						.getHourFrom() : 24);
		hourToSpinner.setSelection(tempFilter.getHourTo() != null ? tempFilter
				.getHourTo() : 24);

		// Set listeners
		startButton.setOnClickListener(this);
		endButton.setOnClickListener(this);
		endButton.setOnClickListener(this);
		deleteStartButton.setOnClickListener(this);
		deleteEndButton.setOnClickListener(this);

		// Set Dialog Options
		setView(layout);
		setCancelable(true);
		setTitle(R.string.filter_source);
		setButton(BUTTON_POSITIVE, context.getString(R.string.ok),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// set the new filter options on OK
						tempFilter.setHourFrom(hourFromSpinner
								.getSelectedItemPosition() < 24 ? hourFromSpinner
								.getSelectedItemPosition() : null);
						tempFilter.setHourTo(hourToSpinner
								.getSelectedItemPosition() < 24 ? hourToSpinner
								.getSelectedItemPosition() : null);
						tempFilter.validate();
						filter.apply(tempFilter);
						measurementManager.setMeasurementFilter(filter);
					}
				});
		setButton(BUTTON_NEGATIVE, context.getString(R.string.cancel),
				(Message) null);

		updateViews();
	}

	public void onClick(View v) {
		Calendar now = new GregorianCalendar();
		switch (v.getId()) {
		case R.id.buttonEnddate:
			if (tempFilter.getTimeTo() == null) {
				// Set Calendar if there is no, no time
				tempFilter.setTimeTo(new GregorianCalendar(now
						.get(Calendar.YEAR), now.get(Calendar.MONTH), now
						.get(Calendar.DAY_OF_MONTH)));
			}
			Calendar timeTo = tempFilter.getTimeTo();
			new DatePickerDialog(getContext(), endDateListener,
					timeTo.get(Calendar.YEAR), timeTo.get(Calendar.MONTH),
					timeTo.get(Calendar.DAY_OF_MONTH)).show();
			break;
		case R.id.buttonStartdate:
			if (tempFilter.getTimeFrom() == null) {
				// Set Calendar if there is no, no time
				tempFilter.setTimeFrom(new GregorianCalendar(now
						.get(Calendar.YEAR), now.get(Calendar.MONTH), now
						.get(Calendar.DAY_OF_MONTH)));
			}
			Calendar timeFrom = tempFilter.getTimeFrom();
			new DatePickerDialog(getContext(), startDateListener,
					timeFrom.get(Calendar.YEAR), timeFrom.get(Calendar.MONTH),
					timeFrom.get(Calendar.DAY_OF_MONTH)).show();
			break;
		case R.id.buttonDeleteEnddate:
			tempFilter.setTimeTo(null);
			updateViews();
			break;
		case R.id.buttonDeleteStartdate:
			tempFilter.setTimeFrom(null);
			updateViews();
			break;
		}
	}

}