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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.n52.android.R;
import org.n52.android.newdata.Annotations.Setting;
import org.n52.android.newdata.Annotations.Settings.Group;
import org.n52.android.newdata.Annotations.Settings.Name;
import org.n52.android.newdata.DataSourceHolder;
import org.n52.android.newdata.Filter;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
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

		LayoutInflater inflater = LayoutInflater.from(this);

		setContentView(R.layout.filter_dialog);
		TypedArray typedArray = obtainStyledAttributes(R.style.formView,
				new int[] { android.R.attr.paddingLeft,
						android.R.attr.paddingRight });
		int paddingLeft = typedArray.getDimensionPixelSize(0, 0);
		int paddingRight = typedArray.getDimensionPixelSize(1, 0);

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

		final Map<String, List<FilterView<?>>> groupFieldMap = new TreeMap<String, List<FilterView<?>>>();

		// Find fields and create views for every annotated field
		for (Field field : dataSource.getFilterClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Setting.class)) {
				FilterView<?> filterView = createFilterViewFromField(field,
						this);
				if (filterView != null) {

					// get group
					String groupName = "";
					Group annotation = field.getAnnotation(Group.class);
					if (annotation != null) {
						groupName = annotation.value();
					}

					List<FilterView<?>> viewList = groupFieldMap.get(groupName);
					if (viewList == null) {
						viewList = new ArrayList<FilterView<?>>();
						groupFieldMap.put(groupName, viewList);
					}
					viewList.add(filterView);
				}
			}
		}

		// create table with labels and views for each field
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		for (Entry<String, List<FilterView<?>>> entry : groupFieldMap
				.entrySet()) {
			String group = entry.getKey();
			if (group != null && !group.isEmpty()) {
				// TextView groupView = new TextView(this, null,
				// R.style.formGroup);
				// Since defStyle is ignored, layout inflater is needed
				// http://code.google.com/p/android/issues/detail?id=12683
				TextView groupView = (TextView) inflater.inflate(
						R.layout.textview_group, null);

				groupView.setText(group);
				layout.addView(groupView, LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
			}

			List<FilterView<?>> viewList = entry.getValue();

			for (FilterView<?> filterView : viewList) {
				Field field = filterView.getField();
				if (field.getAnnotation(Name.class) != null) {

					// TextView labelView = new TextView(this, null,
					// R.style.formLabel);
					// Since defStyle is ignored, layout inflater is needed
					// http://code.google.com/p/android/issues/detail?id=12683

					TextView labelView = (TextView) inflater.inflate(
							R.layout.textview_label, null);
					labelView.setText(field.getAnnotation(Name.class).value());
					layout.addView(labelView, LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT);
				}

				try {
					field.setAccessible(true);
					filterView.setValueObject(field.get(currentFilter));
				} catch (Exception e) {
					// TODO
				}

				// Set padding
				filterView.getView()
						.setPadding(paddingLeft, 0, paddingRight, 0);
				layout.addView(filterView.getView(), LayoutParams.MATCH_PARENT,
						LayoutParams.WRAP_CONTENT);
			}
		}

		dialogView.addView(layout, LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);

		Button okButton = (Button) findViewById(R.id.positiveButton);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				boolean valid = true;
				// Validate all filter views
				for (List<FilterView<?>> viewList : groupFieldMap.values())
					for (FilterView<?> filterView : viewList) {
						if (!filterView.validate()) {
							valid = false;
						}
					}

				if (valid) {
					// Set every field of currentFilter to new value
					for (List<FilterView<?>> viewList : groupFieldMap.values())
						for (FilterView<?> filterView : viewList) {
							try {
								Field field = filterView.getField();
								field.setAccessible(true);
								field.set(currentFilter, filterView.getValue());
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
		Class<?> fieldType = field.getType();
		if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
			return new NumberFilterView<Float>(context, field,
					InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL
							| InputType.TYPE_NUMBER_FLAG_SIGNED) {

				@Override
				protected Float parseString(String value) {
					return Float.parseFloat(value);
				}

			};
		} else if (fieldType.equals(double.class)
				|| fieldType.equals(Double.class)) {
			return new NumberFilterView<Double>(context, field,
					InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL
							| InputType.TYPE_NUMBER_FLAG_SIGNED) {

				@Override
				protected Double parseString(String value) {
					return Double.parseDouble(value);
				}

			};
		} else if (Enum.class.isAssignableFrom(fieldType)) {
			return new SpinnerFilterView<Enum<?>>(context, field, field
					.getType().getEnumConstants()) {

				@Override
				public void setValueObject(Object object) {
					setValue((Enum<?>) object);
				}
			};

		} else if (fieldType.equals(String.class)) {
			return new StringFilterView(context, field,
					InputType.TYPE_CLASS_TEXT);
		} else if (Calendar.class.isAssignableFrom(fieldType)) {
			return new DateTimeFilterView<Calendar>(context, field, null) {

				@Override
				public Calendar getValue() {
					return getSelectedValue();
				}

				@Override
				public void setValue(Calendar value) {
					setSelectedValue(value);
				}

				@Override
				public void setValueObject(Object object) {
					setValue((Calendar) object);
				}

			};
		} else if (fieldType.equals(Date.class)) {
			return new DateTimeFilterView<Date>(context, field, null) {

				@Override
				public Date getValue() {
					Calendar selectedValue = getSelectedValue();
					if (selectedValue != null) {
						return selectedValue.getTime();
					}

					return null;
				}

				@Override
				public void setValue(Date value) {
					if (value != null) {
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(value);
						setSelectedValue(calendar);
					} else {
						setSelectedValue(null);
					}
				}

				@Override
				public void setValueObject(Object object) {
					setValue((Date) object);
				}

			};
		}
		return null;
	}
}
