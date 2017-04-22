package com.sapuseven.untis.utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Timetable {
	private final ArrayList<TimetableItemData> timetableItems = new ArrayList<>();
	private ArrayList<TimegridUnitManager.UnitData> units;

	private void addItem(TimetableItemData itemData) {
		timetableItems.add(itemData);
	}

	public void prepareData(JSONObject userDataList) {
		Collections.sort(timetableItems, new TimetableItemComparator());
		int size = timetableItems.size();
		for (int i = 0; i < size; i++) {
			try {
				while (timetableItems.get(i).getStartDateTime().equals(timetableItems.get(i + 1).getStartDateTime()) &&
						timetableItems.get(i).getEndDateTime().equals(timetableItems.get(i + 1).getEndDateTime())) {
					timetableItems.get(i).mergeWith(timetableItems.get(i + 1), userDataList);
					timetableItems.remove(i + 1);
					size--;
				}
			} catch (IndexOutOfBoundsException ignored) {
			}
		}
	}

	public ArrayList<TimetableItemData> getData() {
		return timetableItems;
	}

	public void setFromJsonObject(JSONObject data) {
		for (int i = 0; i < data.optJSONArray("periods").length(); i++) {
			addItem(new TimetableItemData(data.optJSONArray("periods").optJSONObject(i)));
		}
	}

	public int indexOf(String startDateTime, TimegridUnitManager unitManager) {
		try {
			if (units == null)
				units = unitManager.getUnits();
			for (int i = 0; i < timetableItems.size(); i++) {
				if (timetableItems.get(i).getStartDateTime().equals(String.valueOf(startDateTime))) {
					String in = timetableItems.get(i).getStartDateTime();
					if (in.equals(startDateTime))
						return i;
				}
			}
		} catch (IndexOutOfBoundsException ignored) {
		}
		return -1;
	}

	private class TimetableItemComparator implements Comparator<TimetableItemData> {
		public int compare(TimetableItemData left, TimetableItemData right) {
			return (left.getStartDateTime()).compareTo(right.getStartDateTime());
		}
	}
}
