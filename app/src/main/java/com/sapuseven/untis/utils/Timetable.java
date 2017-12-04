package com.sapuseven.untis.utils;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Timetable {
	private UnitList[][] units;
	private int numberOfDays = -1;
	private int hoursPerDay = -1;
	private TimegridUnitManager unitManager;
	private int[][] offsets;

	public Timetable(JSONObject timetable, SharedPreferences prefs) throws IllegalArgumentException {
		long start = System.currentTimeMillis();
		try {
			unitManager = new TimegridUnitManager(timetable.getJSONObject("masterData")
					.getJSONObject("timeGrid").getJSONArray("days"));
			numberOfDays = unitManager.getNumberOfDays();
			hoursPerDay = unitManager.getMaxHoursPerDay();

			units = new UnitList[numberOfDays][hoursPerDay];

			offsets = new int[numberOfDays][hoursPerDay];

			boolean includeCancelled = !prefs.getBoolean("preference_timetable_hide_cancelled", true);

			addPeriods(timetable.getJSONObject("timetable").getJSONArray("periods"), includeCancelled);
		} catch (JSONException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid timetable object (" + e.getMessage() + ")");
		}
		long end = System.currentTimeMillis();
		Log.d("DebugTimer", "Setup duration: " + (end - start) + "ms");
	}

	private void addPeriods(JSONArray periods, boolean includeCancelled) {
		ArrayList<TimegridUnitManager.UnitData> units = unitManager.getUnits();

		for (int x = 0; x < this.units.length; x++)
			for (int y = 0; y < this.units[0].length; y++)
				this.units[x][y] = new UnitList();

		int hourIndex;

		for (int i = 0; i < periods.length(); i++) {
			try {
				JSONObject item = periods.getJSONObject(i);

				Calendar c = Calendar.getInstance();
				Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(item.getString("startDateTime").substring(0, 10));
				c.setTime(date);

				hourIndex = -1;
				while (hourIndex + 1 < hoursPerDay && Integer.parseInt(item.getString("startDateTime")
						.substring(11, 16).replace(":", "")) >=
						Integer.parseInt(units.get(hourIndex + 1).getStartTime()
								.replace(":", ""))) {
					hourIndex++;
				}

				TimetableItemData itemData = new TimetableItemData(item);
				if ((includeCancelled || !itemData.isCancelled()) && hourIndex > -1)
					this.units[c.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY][hourIndex].units.add(itemData);
			} catch (JSONException | ParseException e) {
				e.printStackTrace();
			}
		}
	}

	public List<TimetableItemData> getItems(int day, int hour) {
		return units[day][hour].units;
	}

	public int getNumberOfDays() {
		return numberOfDays;
	}

	public int getHoursPerDay() {
		return hoursPerDay;
	}

	public int getOffset(int day, int hour) {
		return offsets[day][hour];
	}

	public void addOffset(int day, int hour) {
		offsets[day][hour] += 1;
	}

	public String getStartDateTime(int day, int hour) {
		return getItems(day, hour).get(0).getStartDateTime();
	}

	public String getEndDateTime(int day, int hour) {
		return getItems(day, hour).get(0).getEndDateTime();
	}

	public boolean has(int day, int hour) {
		return getItems(day, hour).size() > 0;
	}

	private class UnitList {
		final ArrayList<TimetableItemData> units = new ArrayList<>();
	}
}
