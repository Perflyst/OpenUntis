package de.perflyst.untis.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class ElementName {
	public static final boolean FULL = true;
	public static final boolean SHORT = false;
	private final List<String> names = new ArrayList<>();
	private final List<String> longNames = new ArrayList<>();
	private ElementType type;
	private JSONObject userData;

	public ElementName() {
	}

	public ElementName(JSONObject userDataList) {
		this.userData = userDataList;
	}

	public ElementName(ElementType type, JSONObject userDataList) {
		this.type = type;
		this.userData = userDataList;
	}

	public static String getTypeName(ElementType type) {
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

	public ElementName fromIdList(List<Integer> list, ElementType elemType) {
		type = elemType;
		for (int i : list)
			try {
				names.add((String) findFieldByValue("id", i, "name"));
			} catch (NoSuchElementException | JSONException ignored) {
			}
		for (int i : list)
			try {
				longNames.add((String) findFieldByValue("id", i, "longName"));
			} catch (NoSuchElementException | JSONException ignored) {
			}
		return this;
	}

	public Object findFieldByValue(String srcField, Object srcValue, String dstFieldName) throws JSONException {
		if (srcField == null || srcValue == null || dstFieldName == null)
			return null;
		for (int i = 0; i < userData.getJSONObject("masterData").getJSONArray(getTypeName(type)).length(); i++)
			if (userData.getJSONObject("masterData").getJSONArray(getTypeName(type)).getJSONObject(i).get(srcField).equals(srcValue))
				return userData.getJSONObject("masterData").getJSONArray(getTypeName(type)).getJSONObject(i).get(dstFieldName);
		throw new JSONException("Data array empty");
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

	public List<String> getNames() {
		return names;
	}

	public List<String> getLongNames() {
		return longNames;
	}

	public enum ElementType {
		UNKNOWN(0),
		CLASS(1),
		TEACHER(2),
		SUBJECT(3),
		ROOM(4),
		STUDENT(5),
		HOLIDAY(6);

		public final int value;

		ElementType(int value) {
			this.value = value;
		}

		public static ElementType fromValue(int value) {
			for (ElementType result : values())
				if (result.value == value) return result;

			return UNKNOWN;
		}
	}
}