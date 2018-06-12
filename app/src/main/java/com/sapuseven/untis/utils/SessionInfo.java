package com.sapuseven.untis.utils;

import org.json.JSONObject;

import static com.sapuseven.untis.utils.ElementName.ElementType;
import static com.sapuseven.untis.utils.ElementName.ElementType.CLASS;
import static com.sapuseven.untis.utils.ElementName.ElementType.ROOM;
import static com.sapuseven.untis.utils.ElementName.ElementType.STUDENT;
import static com.sapuseven.untis.utils.ElementName.ElementType.TEACHER;
import static com.sapuseven.untis.utils.ElementName.ElementType.UNKNOWN;

public class SessionInfo {
	private int elemId;
	private String elemType;
	private String displayName;

	public SessionInfo() {
		elemId = -1;
		elemType = "";
		displayName = "";
	}

	public static String getElemTypeName(ElementType type) {
		switch (type) {
			case STUDENT:
				return "STUDENT";
			case CLASS:
				return "CLASS";
			case TEACHER:
				return "TEACHER";
			case ROOM:
				return "ROOM";
			default:
				return "";
		}
	}

	public static ElementType getElemTypeId(String type) {
		switch (type) {
			case "STUDENT":
				return STUDENT;
			case "CLASS":
				return CLASS;
			case "TEACHER":
				return TEACHER;
			case "ROOM":
				return ROOM;
			default:
				return UNKNOWN;
		}
	}

	public void setDataFromJsonObject(JSONObject data) {
		elemId = data.optInt("elemId", -1);
		elemType = data.optString("elemType", "");
		displayName = data.optString("displayName", "BetterUntis");
	}

	public int getElemId() {
		return elemId;
	}

	public void setElemId(int elemId) {
		this.elemId = elemId;
	}

	public String getElemType() {
		return elemType;
	}

	public void setElemType(String elemType) {
		this.elemType = elemType;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}