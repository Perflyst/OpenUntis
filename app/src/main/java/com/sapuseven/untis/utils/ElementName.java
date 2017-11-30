package com.sapuseven.untis.utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
	private final List<String> names = new ArrayList<>();
	private final List<String> longNames = new ArrayList<>();
	private List<Integer> ids = new ArrayList<>();
	private int type;
	private JSONObject userData;

	ElementName() {
	}

	ElementName(JSONObject userDataList) {
		this.userData = userDataList;
	}

	public ElementName(int type) {
		this.type = type;
	}

	public static String getTypeName(int type) {
		switch (type) {
			case CLASS:
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

	public ElementName setUserDataList(JSONObject userDataList) {
		this.userData = userDataList;
		return this;
	}

	ElementName fromIdList(List<Integer> list, int elemType) {
		if (this.userData == null)
			throw new RuntimeException("You have to provide a unitList via setUserDataList()!");
		type = elemType;
		ids = list;
		for (int i : list)
			try {
				names.add((String) findFieldByValue("id", i, "name"));
			} catch (NoSuchElementException ignored) {
			}
		for (int i : list)
			try {
				longNames.add((String) findFieldByValue("id", i, "longName"));
			} catch (NoSuchElementException ignored) {
			}
		return this;
	}

	public Object findFieldByValue(String srcField, Object srcValue, String dstFieldName) {
		if (srcField == null || srcValue == null || dstFieldName == null)
			return null;
		for (int i = 0; i < userData.optJSONObject("masterData").optJSONArray(getTypeName(type)).length(); i++)
			if (userData.optJSONObject("masterData").optJSONArray(getTypeName(type)).optJSONObject(i).opt(srcField).equals(srcValue))
				return userData.optJSONObject("masterData").optJSONArray(getTypeName(type)).optJSONObject(i).opt(dstFieldName);
		throw new NoSuchElementException("Item not found");
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

	List<Integer> getIds() {
		return ids;
	}

	public List<String> getNames() {
		return names;
	}

	public List<String> getLongNames() {
		return longNames;
	}
}