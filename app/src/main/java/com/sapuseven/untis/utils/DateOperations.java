package com.sapuseven.untis.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateOperations {
	private static final SimpleDateFormat FROM_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
	private static final SimpleDateFormat TO_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.US);

	public static Calendar getStartDateFromWeek(Calendar week, int offset, boolean resetTime) {
		week.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		week.add(Calendar.DAY_OF_MONTH, offset);
		if (resetTime) {
			week.set(Calendar.HOUR_OF_DAY, 0);
			week.set(Calendar.MINUTE, 0);
			week.set(Calendar.SECOND, 0);
			week.set(Calendar.MILLISECOND, 0);
		}
		return week;
	}

	public static Calendar getStartDateFromWeek(Calendar week, int offset) {
		return getStartDateFromWeek(week, offset, false);
	}

	public static int addDaysToInt(int startDate, int days) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
			c.setTime(sdf.parse(Integer.toString(startDate)));
			c.add(Calendar.DATE, days);
			return Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US).format(c.getTime()));
		} catch (ParseException e) {
			e.printStackTrace();
			return startDate;
		}
	}

	public static String addDaysToInt(int startDate, int days, SimpleDateFormat targetFormat) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
			c.setTime(sdf.parse(Integer.toString(startDate)));
			c.add(Calendar.DATE, days);
			return targetFormat.format(c.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getStringDateFromInt(int date) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
			c.setTime(sdf.parse(Integer.toString(date)));
			return new SimpleDateFormat("d. MMM", Locale.getDefault()).format(c.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String getDayNameFromInt(int date) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
			c.setTime(sdf.parse(Integer.toString(date)));
			return new SimpleDateFormat("EEE", Locale.getDefault()).format(c.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static Date parseFromISO(String dateTime) throws ParseException {
		return FROM_ISO_8601.parse(dateTime);
	}
}