package org.n52.android.newdata.filter;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;

import org.n52.android.newdata.Annotations.Setting;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.text.InputType;

public class SettingsHelper {

	public static boolean hasSettings(Object settingsObject) {
		return hasSettings(settingsObject.getClass());
	}

	public static boolean hasSettings(Class<?> settingsClass) {
		for (Field field : settingsClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(Setting.class)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Creates a FilterView object for the specified field
	 * 
	 * @param field
	 * @param context
	 * @return
	 */
	public static FilterView<?> createFilterViewFromField(Field field,
			Context context) {
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
