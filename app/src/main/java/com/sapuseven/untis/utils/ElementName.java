package com.sapuseven.untis.utils;

import org.json.JSONObject;

import java.util.ArrayList;

@SuppressWarnings("WeakerAccess")
public class ElementName {
	public static final int CLASS = 0x0001;
	public static final int TEACHER = 0x0002;
	public static final int SUBJECT = 0x0003;
	public static final int ROOM = 0x0004;
	public static final int STUDENT = 0x0005;
	public static final int HOLIDAY = 0x0006;
	public static final boolean FULL = true;
	public static final boolean SHORT = false;
	private final ArrayList<String> names = new ArrayList<>();
	private final ArrayList<String> longNames = new ArrayList<>();
	private ArrayList<Integer> ids = new ArrayList<>();
	private int type;
	private JSONObject list;

	ElementName() {
	}

	public ElementName(int type) {
		this.type = type;
	}

	public static String getTypeName(int type) {
		switch (type) {
			case CLASS:
				//noinspection SpellCheckingInspection
				return "klassen";
			case TEACHER:
				return "teachers";
			case SUBJECT:
				return "subjects";
			case ROOM:
				return "rooms";
			case STUDENT:
				return "students";
			case HOLIDAY:
				return "holidays";
			default:
				return null;
		}
	}

	public ElementName setUserDataList(JSONObject list) {
		this.list = list;
		return this;
	}

	ElementName fromIdList(ArrayList<Integer> list, int elemType) {
		if (this.list == null)
			throw new RuntimeException("You have to provide a list via setUserDataList()!");
		type = elemType;
		ids = list;
		for (int i : list)
			names.add((String) findFieldByValue("id", i, "name"));
		for (int i : list)
			longNames.add((String) findFieldByValue("id", i, "longName"));
		return this;
	}

	public Object findFieldByValue(String srcField, Object srcValue, String dstFieldName) {
		for (int i = 0; i < list.optJSONObject("masterData").optJSONArray(getTypeName(type)).length(); i++)
			if (list.optJSONObject("masterData").optJSONArray(getTypeName(type)).optJSONObject(i).opt(srcField).equals(srcValue))
				return list.optJSONObject("masterData").optJSONArray(getTypeName(type)).optJSONObject(i).opt(dstFieldName);
		return null;
	}

	public boolean isEmpty() {
		return names.isEmpty() || longNames.isEmpty();
	}

	public String getName(boolean full) {
		if (full) {
			StringBuilder sb = new StringBuilder();
			if (names.size() > 1) {
				sb.append(names.get(0));
				for (int i = 1; i < names.size(); i++)
					sb.append(", ").append(names.get(i));
				return sb.toString();
			} else if (names.size() == 1)
				return names.get(0);
			else
				return "";
		} else {
			if (names.size() > 1)
				return names.get(0) + ", …";
			else if (names.size() == 1)
				return names.get(0);
			else
				return "";
		}
	}

	public String getLongName(boolean full) {
		if (full) {
			StringBuilder sb = new StringBuilder();
			if (longNames.size() > 1) {
				sb.append(longNames.get(0));
				for (int i = 1; i < longNames.size(); i++)
					sb.append(", ").append(longNames.get(i));
				return sb.toString();
			} else if (longNames.size() == 1)
				return longNames.get(0);
			else
				return "";
		} else {
			if (longNames.size() > 1)
				return longNames.get(0) + ", …";
			else if (longNames.size() == 1)
				return longNames.get(0);
			else
				return "";
		}
	}

	ArrayList<Integer> getIds() {
		return ids;
	}

	public ArrayList<String> getNames() {
		return names;
	}

	public ArrayList<String> getLongNames() {
		return longNames;
	}
}