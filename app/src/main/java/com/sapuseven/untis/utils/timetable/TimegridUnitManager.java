package com.sapuseven.untis.utils.timetable;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class TimegridUnitManager {
	private ArrayList<UnitData> unitList;
	private JSONArray days;
	private int numberOfDays = -1;
	private int maxHoursPerDay = -1;

	public TimegridUnitManager(JSONArray days) {
		if (days == null)
			this.days = new JSONArray();
		this.days = days;
	}

	public int getNumberOfDays() {
		if (numberOfDays < 0)
			calculateCounts();
		return numberOfDays;
	}

	private void calculateCounts() {
		numberOfDays = days.length();

		for (int i = 0; i < days.length(); i++)
			try {
				maxHoursPerDay = Math.max(maxHoursPerDay, days.getJSONObject(i)
						.getJSONArray("units").length());
			} catch (JSONException e) {
				e.printStackTrace();
			}
	}

	public int getMaxHoursPerDay() {
		if (maxHoursPerDay < 0)
			calculateCounts();
		return maxHoursPerDay;
	}

	public ArrayList<UnitData> getUnits() {
		if (unitList == null) {
			unitList = new ArrayList<>();
			try {
				JSONArray units = days.getJSONObject(0).getJSONArray("units");
				for (int i = 0; i < units.length(); i++) {
					UnitData unitData = new UnitData(
							units.getJSONObject(i).getString("startTime").substring(1),
							units.getJSONObject(i).getString("endTime").substring(1)
					);

					unitList.add(unitData);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return unitList;
	}

	public int getDayIndex(Calendar c) throws IndexOutOfBoundsException {
		try {
			SimpleDateFormat dayFormat = new SimpleDateFormat("E", Locale.ENGLISH);
			for (int i = 0; i < days.length(); i++) {
				String day = days.getJSONObject(i).getString("day");
				if (day.equalsIgnoreCase(dayFormat.format(c.getTime())))
					return i;
			}
			throw new IndexOutOfBoundsException("Day not in timetable");
		} catch (JSONException e) {
			throw new IndexOutOfBoundsException("Invalid userData!");
		}
	}

	public class UnitData {
		private final String startTime;
		private final String endTime;

		private UnitData(String startTime, String endTime) {
			this.startTime = startTime;
			this.endTime = endTime;
		}

		String getStartTime() {
			return startTime;
		}

		String getEndTime() {
			return endTime;
		}

		public String getDisplayStartTime() {
			String displayTime = startTime;
			while (displayTime.charAt(0) == '0')
				displayTime = displayTime.substring(1);
			return displayTime;
		}

		public String getDisplayEndTime() {
			String displayTime = endTime;
			while (displayTime.charAt(0) == '0')
				displayTime = displayTime.substring(1);
			return displayTime;
		}
	}
}