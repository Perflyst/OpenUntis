package com.sapuseven.untis.utils;

import org.json.JSONObject;

import static com.sapuseven.untis.utils.ElementName.CLASS;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.STUDENT;
import static com.sapuseven.untis.utils.ElementName.TEACHER;

/**
 * This class is used to store a session object
 * for comfortable access.
 *
 * @author paul
 * @version 1.0
 * @since 2016-09-25
 */

public class SessionInfo {
	private int elemId;
	private String elemType;
	private String displayName;

	public SessionInfo() {
		elemId = -1;
		elemType = "";
		displayName = "";
	}

	public static String getElemTypeName(int type) {
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

	public static int getElemTypeId(String type) {
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
				return -1;
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