package org.n52.android.settings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

	static SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault());

	public static String getISOString(Date date) {
		return dateFormatter.format(date);
	}

	public static String getISOString(Calendar calendar) {
		return dateFormatter.format(calendar.getTime());
	}

	public static Date getDateFromISOString(String string) {
		try {
			return dateFormatter.parse(string);
		} catch (ParseException e) {
			// TODO
			e.printStackTrace();
			return null;
		}
	}

}