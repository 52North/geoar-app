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
package org.n52.android.newdata.filter;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.n52.android.geoar.R;
import org.n52.android.newdata.Annotations.Filterable;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.Filter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

/**
 * Dialog to dynamically show and edit filter settings of a supplied data source
 * 
 */
public class FilterDialogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter_dialog2);

		Button cancelButton = (Button) findViewById(R.id.negativeButton);
		cancelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});

		DataSourceHolder dataSource = getIntent().getParcelableExtra(
				"dataSource");
		if (dataSource == null) {
			return;
		}
		final Filter currentFilter = dataSource.getCurrentFilter();

		ScrollView dialogView = (ScrollView) findViewById(R.id.dialogView);

		final Map<Field, FilterView<?>> fieldMap = new HashMap<Field, FilterView<?>>();

		// Find fields and create views for every annotated field
		for (Field field : dataSource.getFilterClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Filterable.class)) {
				fieldMap.put(field, createFilterViewFromField(field, this));
			}
		}

		// create table with labels and views for each field
		TableLayout table = new TableLayout(this);
		for (Entry<Field, FilterView<?>> entry : fieldMap.entrySet()) {
			Field field = entry.getKey();
			TableRow row = new TableRow(this);

			TextView label = new TextView(this);
			label.setText(field.getAnnotation(Filterable.class).value());
			row.addView(label, LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);

			View filterView = entry.getValue().getView();
			try {
				field.setAccessible(true);
				entry.getValue().setValueObject(field.get(currentFilter));
			} catch (Exception e) {

			}

			row.addView(filterView, new TableRow.LayoutParams(
					LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
			table.addView(row, LayoutParams.MATCH_PARENT,
					LayoutParams.WRAP_CONTENT);
		}

		dialogView.addView(table, LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);

		Button okButton = (Button) findViewById(R.id.positiveButton);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				boolean valid = true;
				// Validate all filter views
				for (FilterView<?> filterView : fieldMap.values()) {
					if (!filterView.validate()) {
						valid = false;
					}
				}

				if (valid) {
					// Set every field of currentFilter to new value
					for (Entry<Field, FilterView<?>> entry : fieldMap
							.entrySet()) {
						try {
							Field field = entry.getKey();
							field.setAccessible(true);
							field.set(currentFilter, entry.getValue()
									.getValue());
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					finish();
				}
			}
		});
	}

	/**
	 * Creates a FilterView object for the specified field
	 * 
	 * @param field
	 * @param context
	 * @return
	 */
	private FilterView<?> createFilterViewFromField(Field field, Context context) {
		Type fieldType = field.getType();
		if (fieldType.equals(float.class)) {
			return new NumberFilterView<Float>(context,
					InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL) {

				@Override
				public Float getValue() {
					return Float.parseFloat(getText().toString());
				}

				@Override
				public void setValue(Float value) {
					setText(value + "");
				}

				@Override
				public void setValueObject(Object object) {
					setValue((Float) object);
				}

			};
		} else if (fieldType.equals(double.class)) {
			return new NumberFilterView<Double>(context,
					InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL) {

				@Override
				public Double getValue() {
					return Double.parseDouble(getText().toString());
				}

				@Override
				public void setValue(Double value) {
					setText(value + "");
				}

				@Override
				public void setValueObject(Object object) {
					setValue((Double) object);
				}

			};
		}

		return null;
	}
}
