package com.sapuseven.untis.utils;

import org.json.JSONArray;

import java.util.ArrayList;

public class TimegridUnitManager {
	private JSONArray data;
	private int length = -1;

	public void setList(JSONArray data) {
		this.data = data;
	}

	public int numberOfDays() {
		return data.length();
	}

	public ArrayList<UnitData> getUnits() {
		ArrayList<UnitData> list = new ArrayList<>();
		JSONArray units = data.optJSONObject(0).optJSONArray("units");
		for (int i = 0; i < units.length(); i++) {
			UnitData unitData = new UnitData();
			unitData.setIndex(Integer.toString(i + 1));
			unitData.setStartTime(units.optJSONObject(i).optString("startTime").substring(1));
			unitData.setEndTime(units.optJSONObject(i).optString("endTime").substring(1));

			list.add(unitData);
		}
		return list;
	}

	public int getUnitCount() {
		if (length < 0)
			length = data.optJSONObject(0).optJSONArray("units").length();
		return length;
	}

	public class UnitData {
		private String index;
		private String startTime;
		private String endTime;

		public String getDisplayEndTime() {
			String displayTime = endTime;
			while (displayTime.charAt(0) == '0')
				displayTime = displayTime.substring(1);
			return displayTime;
		}

		public String getEndTime() {
			return endTime;
		}

		void setEndTime(String endTime) {
			this.endTime = endTime;
		}

		public String getDisplayStartTime() {
			String displayTime = startTime;
			while (displayTime.charAt(0) == '0')
				displayTime = displayTime.substring(1);
			return displayTime;
		}

		public String getStartTime() {
			return startTime;
		}

		void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public String getIndex() {
			return index;
		}

		void setIndex(String index) {
			this.index = index;
		}
	}
}