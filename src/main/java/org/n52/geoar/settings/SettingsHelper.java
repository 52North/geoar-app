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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.n52.geoar.newdata.Annotations.DefaultSetting;
import org.n52.geoar.newdata.Annotations.DefaultSettingsSet;
import org.n52.geoar.newdata.Annotations.PostSettingsChanged;
import org.n52.geoar.newdata.Annotations.Setting;

import android.content.Context;
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
	 *            {@link Context} to use for creating views
	 * @return
	 */
	public static SettingsViewField<?> createFilterViewFromField(Field field,
			Context context) {
		Class<?> fieldType = field.getType();
		if (fieldType.equals(float.class) || fieldType.equals(Float.class)) {
			return new NumberSettingsViewField<Float>(context, field,
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
			return new NumberSettingsViewField<Double>(context, field,
					InputType.TYPE_CLASS_NUMBER
							| InputType.TYPE_NUMBER_FLAG_DECIMAL
							| InputType.TYPE_NUMBER_FLAG_SIGNED) {

				@Override
				protected Double parseString(String value) {
					return Double.parseDouble(value);
				}

			};
		} else if (Enum.class.isAssignableFrom(fieldType)) {
			return new SpinnerSettingsViewField<Enum<?>>(context, field, field
					.getType().getEnumConstants()) {

				@Override
				public void setValueObject(Object object) {
					setValue((Enum<?>) object);
				}
			};

		} else if (fieldType.equals(String.class)) {
			return new StringSettingsViewField(context, field,
					InputType.TYPE_CLASS_TEXT);
		} else if (Calendar.class.isAssignableFrom(fieldType)) {
			return new DateTimeSettingsViewField<Calendar>(context, field, null) {

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
			return new DateTimeSettingsViewField<Date>(context, field, null) {

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

	public static void storeSettings(ObjectOutputStream objectOutputStream,
			Object settingsObject) throws IOException {
		for (Field field : settingsObject.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Setting.class)) {
				try {
					field.setAccessible(true);
					objectOutputStream.writeObject(field.get(settingsObject));
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void restoreSettings(ObjectInputStream objectInputStream,
			Object settingsObject) throws IOException {
		for (Field field : settingsObject.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Setting.class)) {
				try {
					field.setAccessible(true);
					field.set(settingsObject, objectInputStream.readObject());
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void applyDefaultSettings(
			DefaultSettingsSet defaultSettingsSet, Object settingsObject) {
		Class<? extends Object> settingsClass = settingsObject.getClass();
		try {
			for (DefaultSetting defaultSetting : defaultSettingsSet.value()) {

				Field field = settingsClass.getDeclaredField(defaultSetting
						.name());
				field.setAccessible(true);
				field.set(settingsObject,
						getObjectFromStringValue(field, defaultSetting.value()));
			}

		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		notifySettingsChanged(settingsObject);
	}

	private static Object getObjectFromStringValue(Field field, String value) {
		if (String.class.isAssignableFrom(field.getType())) {
			return value;
		} else if (Integer.class.isAssignableFrom(field.getType())) {
			return Integer.parseInt(value);
		} else if (Float.class.isAssignableFrom(field.getType())) {
			return Float.parseFloat(value);
		} else if (Double.class.isAssignableFrom(field.getType())) {
			return Double.parseDouble(value);
		} else if (Date.class.isAssignableFrom(field.getType())) {
			return DateUtils.getDateFromISOString(value);
		} else if (Calendar.class.isAssignableFrom(field.getType())) {
			Calendar instance = Calendar.getInstance();
			instance.setTime(DateUtils.getDateFromISOString(value));
			return instance;
		}

		throw new RuntimeException(field.getType()
				+ " not supported for default setting");
	}

	static void notifySettingsChanged(Object settingsObject) {
		for (Method method : settingsObject.getClass().getDeclaredMethods()) {
			if (method.isAnnotationPresent(PostSettingsChanged.class)) {
				try {
					method.setAccessible(true);
					method.invoke(settingsObject);
				} catch (IllegalArgumentException e) {
					throw new SettingsException(e.getMessage(), e);
				} catch (IllegalAccessException e) {
					throw new SettingsException(e.getMessage(), e);
				} catch (InvocationTargetException e) {
					throw new SettingsException(e.getMessage(), e);
				}
			}
		}
	}

	static boolean isEqualSettings(Object settingsObject1,
			Object settingsObject2) {
		if (!settingsObject2.getClass().isAssignableFrom(
				settingsObject1.getClass())) {
			return false;
		}

		Class<? extends Object> settingsClass = settingsObject1.getClass();
		try {
			for (Field field : settingsClass.getDeclaredFields()) {
				field.setAccessible(true);
				if (!field.get(settingsObject1).equals(
						field.get(settingsObject2))) {
					return false;
				}
			}
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public static boolean isEqualSettings(
			DefaultSettingsSet defaultSettingsSet, Object settingsObject) {

		Class<? extends Object> settingsClass = settingsObject.getClass();
		try {
			for (DefaultSetting defaultSetting : defaultSettingsSet.value()) {

				Field field = settingsClass.getDeclaredField(defaultSetting
						.name());
				field.setAccessible(true);
				if (!field.get(settingsObject)
						.equals(getObjectFromStringValue(field,
								defaultSetting.value()))) {
					return false;
				}
			}
		} catch (IllegalAccessException e) {

		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}
}
