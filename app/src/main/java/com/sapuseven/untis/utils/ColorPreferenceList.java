package com.sapuseven.untis.utils;

import java.util.ArrayList;

public class ColorPreferenceList {
	private ArrayList<String> keys = new ArrayList<>();

	public void add(String key) {
		keys.add(key);
	}

	public int size() {
		return keys.size();
	}

	public String getKey(int index) {
		return keys.get(index);
	}
}