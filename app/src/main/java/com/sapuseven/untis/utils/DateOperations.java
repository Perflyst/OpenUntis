package com.sapuseven.untis.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateOperations {
	private static final String TAG = "DateOperations";

	private static final SimpleDateFormat FROM_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);

	public DateOperations() {
		throw new RuntimeException("Instantiation not allowed");
	}

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
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
			c.setTime(sdf.parse(Integer.toString(startDate)));
			c.add(Calendar.DATE, days);
			return Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).format(c.getTime()));
		} catch (ParseException e) {
			Log.w(TAG, e.getMessage());
			return startDate;
		}
	}

	public static String addDaysToInt(int startDate, int days, SimpleDateFormat targetFormat) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
			c.setTime(sdf.parse(Integer.toString(startDate)));
			c.add(Calendar.DATE, days);
			return targetFormat.format(c.getTime());
		} catch (ParseException e) {
			Log.w(TAG, e.getMessage());
			return null;
		}
	}

	public static String getStringDateFromInt(int date, Locale locale) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
			c.setTime(sdf.parse(Integer.toString(date)));
			return new SimpleDateFormat("d. MMM", locale).format(c.getTime());
		} catch (ParseException e) {
			Log.w(TAG, e.getMessage());
			return null;
		}
	}

	public static String getDayNameFromInt(int date, Locale locale) {
		try {
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
			c.setTime(sdf.parse(Integer.toString(date)));
			return new SimpleDateFormat("EEE", locale).format(c.getTime());
		} catch (ParseException e) {
			Log.w(TAG, e.getMessage());
			return null;
		}
	}

	public static Date parseFromISO(String dateTime) throws ParseException {
		return FROM_ISO_8601.parse(dateTime);
	}

	public static int getComparableTime(String dateTime) {
		Pattern pattern = Pattern.compile("[0-9]{2}:[0-9]{2}");
		Matcher matcher = pattern.matcher(dateTime);
		if (matcher.find())
			return Integer.parseInt((matcher.group(0).replace(":", "")));
		else
			throw new IllegalArgumentException("No time found");
	}
}