package com.sapuseven.untis.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import static android.graphics.Color.parseColor;
import static com.sapuseven.untis.utils.Constants.TimetableItem.CODE_CANCELLED;
import static com.sapuseven.untis.utils.ElementName.CLASS;
import static com.sapuseven.untis.utils.ElementName.HOLIDAY;
import static com.sapuseven.untis.utils.ElementName.ROOM;
import static com.sapuseven.untis.utils.ElementName.SUBJECT;
import static com.sapuseven.untis.utils.ElementName.TEACHER;

public class TimetableItemData {
	private final ArrayList<Integer> holidays = new ArrayList<>();
	private String startDateTime = "";
	private String endDateTime = "";
	private String info = "";
	private ArrayList<String> codes = new ArrayList<>();
	private ArrayList<Integer> classes = new ArrayList<>();
	private ArrayList<Integer> subjects = new ArrayList<>();
	private ArrayList<Integer> teachers = new ArrayList<>();
	private ArrayList<Integer> rooms = new ArrayList<>();
	private boolean free;
	private int backColor;

	public TimetableItemData() {
	}

	TimetableItemData(JSONObject itemData) {
		for (int i = 0; i < itemData.optJSONArray("elements").length(); i++) {
			switch (itemData.optJSONArray("elements").optJSONObject(i).optString("type")) {
				case "CLASS":
					addClass(itemData.optJSONArray("elements").optJSONObject(i).optInt("id"));
					break;
				case "TEACHER":
					addTeacher(itemData.optJSONArray("elements").optJSONObject(i).optInt("id"));
					break;
				case "SUBJECT":
					addSubject(itemData.optJSONArray("elements").optJSONObject(i).optInt("id"));
					break;
				case "ROOM":
					addRoom(itemData.optJSONArray("elements").optJSONObject(i).optInt("id"));
					break;
			}
			setStartDateTime(itemData.optString("startDateTime"));
			setEndDateTime(itemData.optString("endDateTime"));
			setCodes(itemData.optJSONArray("is"));
			setBackColor(itemData.optString("backColor", "#E0E0E0"));
			setInfo(itemData.optJSONObject("text").optString("lesson", "")); // TODO: Show all three texts
		}
	}

	public ElementName getClasses(JSONObject list) {
		if (classes.size() > 0)
			return new ElementName().setUserDataList(list).fromIdList(classes, CLASS);
		else
			return new ElementName();
	}

	private void addClass(int classId) {
		if (codes.contains(CODE_CANCELLED) && this.classes.size() > 0)
			this.classes.remove(this.classes.size() - 1);
		this.classes.add(classId);
	}

	public ElementName getSubjects(JSONObject list) {
		if (subjects.size() > 0)
			return new ElementName().setUserDataList(list).fromIdList(subjects, SUBJECT);
		else
			return new ElementName();
	}

	private void addSubject(int subject) {
		if (codes.contains(CODE_CANCELLED) && this.subjects.size() > 0)
			this.subjects.remove(this.subjects.size() - 1);
		this.subjects.add(subject);
	}

	public ElementName getTeachers(JSONObject list) {
		if (teachers.size() > 0)
			return new ElementName().setUserDataList(list).fromIdList(teachers, TEACHER);
		else
			return new ElementName();
	}

	private void addTeacher(int teacherId) {
		this.teachers.add(teacherId);
	}

	public ElementName getRooms(JSONObject list) {
		if (rooms.size() > 0)
			return new ElementName().setUserDataList(list).fromIdList(rooms, ROOM);
		else
			return new ElementName();
	}

	private void addRoom(int room) {
		if (codes.contains(CODE_CANCELLED) && this.rooms.size() > 0)
			this.rooms.remove(this.rooms.size() - 1);
		this.rooms.add(room);
	}

	public ElementName getHolidays(JSONObject list) {
		if (holidays.size() > 0)
			return new ElementName().setUserDataList(list).fromIdList(holidays, HOLIDAY);
		else
			return new ElementName();
	}

	public void addHoliday(int holiday) {
		this.holidays.add(holiday);
	}

	public String getStartDateTime() {
		return startDateTime;
	}

	private void setStartDateTime(String startDateTime) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() + startDateTime.length() < 4) {
			sb.append('0');
		}
		sb.append(startDateTime);
		this.startDateTime = sb.toString();
	}

	public String getEndDateTime() {
		return endDateTime;
	}

	public void setEndDateTime(String endDateTime) {
		StringBuilder sb = new StringBuilder();
		while (sb.length() + endDateTime.length() < 4) {
			sb.append('0');
		}
		sb.append(endDateTime);
		this.endDateTime = sb.toString();
	}

	public ArrayList<String> getCodes() {
		return codes;
	}

	private void setCodes(JSONArray codes) {
		for (int i = 0; i < codes.length(); i++)
			if (!this.codes.contains(codes.optString(i)))
				this.codes.add(codes.optString(i));
	}

	public boolean isEmpty(JSONObject list) {
		return getSubjects(list).isEmpty() && getTeachers(list).isEmpty() && getRooms(list).isEmpty() && !isFree();
	}

	void mergeWith(TimetableItemData timetableItemData2, JSONObject list) {
		if (this.getCodes().contains(CODE_CANCELLED)) {
			classes = timetableItemData2.getClasses(list).getIds();
			subjects = timetableItemData2.getSubjects(list).getIds();
			teachers = timetableItemData2.getTeachers(list).getIds();
			rooms = timetableItemData2.getRooms(list).getIds();
			codes = timetableItemData2.getCodes();
			return;
		}
		for (int i = 0; i < timetableItemData2.getClasses(list).getIds().size(); i++)
			if (!getClasses(list).getIds().contains(timetableItemData2.getClasses(list).getIds().get(i)))
				addClass(timetableItemData2.getClasses(list).getIds().get(i));
		for (int i = 0; i < timetableItemData2.getSubjects(list).getIds().size(); i++)
			if (!getSubjects(list).getIds().contains(timetableItemData2.getSubjects(list).getIds().get(i)))
				addSubject(timetableItemData2.getSubjects(list).getIds().get(i));
		for (int i = 0; i < timetableItemData2.getTeachers(list).getIds().size(); i++)
			if (!getTeachers(list).getIds().contains(timetableItemData2.getTeachers(list).getIds().get(i)))
				addTeacher(timetableItemData2.getTeachers(list).getIds().get(i));
		for (int i = 0; i < timetableItemData2.getRooms(list).getIds().size(); i++)
			if (!getRooms(list).getIds().contains(timetableItemData2.getRooms(list).getIds().get(i)))
				addRoom(timetableItemData2.getRooms(list).getIds().get(i));
	}

	public boolean isFree() {
		return free;
	}

	public void setFree() {
		this.free = true;
	}

	public int getBackColor() {
		return this.backColor;
	}

	private void setBackColor(String backColor) {
		this.backColor = parseColor(backColor);
	}

	public String getInfo() {
		return info;
	}

	private void setInfo(String info) {
		this.info = info;
	}
}