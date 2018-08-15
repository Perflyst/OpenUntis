package com.perflyst.untis.utils.timetable;

import com.perflyst.untis.utils.Constants;
import com.perflyst.untis.utils.ElementName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Color.parseColor;
import static com.perflyst.untis.utils.ElementName.ElementType.CLASS;
import static com.perflyst.untis.utils.ElementName.ElementType.HOLIDAY;
import static com.perflyst.untis.utils.ElementName.ElementType.ROOM;
import static com.perflyst.untis.utils.ElementName.ElementType.SUBJECT;
import static com.perflyst.untis.utils.ElementName.ElementType.TEACHER;

public class TimetableItemData {
	private final ArrayList<Integer> holidays = new ArrayList<>();
	private String startDateTime = "";
	private String endDateTime = "";
	private final List<String> codes = new ArrayList<>();
	private final List<Integer> classes = new ArrayList<>();
	private final List<Integer> subjects = new ArrayList<>();
	private final List<Integer> teachers = new ArrayList<>();
	private final List<Integer> rooms = new ArrayList<>();
	private final List<String> infos = new ArrayList<>();
	private boolean hidden;
	private int backColor;
	private boolean dummy = false;

	TimetableItemData(JSONObject itemData) {
		for (int i = 0; i < itemData.optJSONArray("elements").length(); i++) {
			switch (itemData.optJSONArray("elements").optJSONObject(i).optString("type")) { //  // id = current value | orgId = original value if for example the teacher has changed
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
		}
		try {
			setStartDateTime(itemData.getString("startDateTime"));
			setEndDateTime(itemData.getString("endDateTime"));
			setCodes(itemData.getJSONArray("is"));
			setBackColor(itemData.optString("backColor", "#E0E0E0"));
			setInfos(itemData.getJSONObject("text"));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private TimetableItemData() {
	}

	public static TimetableItemData combine(List<TimetableItemData> items,
	                                        String startDateTime, String endDateTime) {
		TimetableItemData newItem = new TimetableItemData();
		for (TimetableItemData item : items) {
			newItem.addClass(item.getClasses());
			newItem.addSubject(item.getSubjects());
			newItem.addTeacher(item.getTeachers());
			newItem.addRoom(item.getRooms());
			// TODO: Combine code as well
		}
		newItem.setStartDateTime(startDateTime);
		newItem.setEndDateTime(endDateTime);
		return newItem;
	}

	public ElementName getClasses(JSONObject userDataList) {
		if (classes.size() > 0)
			return new ElementName(userDataList).fromIdList(classes, CLASS);
		else
			return new ElementName();
	}

	private void addClass(int classId) {
		if (isCancelled() && this.classes.size() > 0)
			this.classes.remove(this.classes.size() - 1);
		this.classes.add(classId);
	}

	public ElementName getSubjects(JSONObject userDataList) {
		if (subjects.size() > 0)
			return new ElementName(userDataList).fromIdList(subjects, SUBJECT);
		else
			return new ElementName();
	}

	private void addSubject(int subject) {
		if (isCancelled() && this.subjects.size() > 0)
			this.subjects.remove(this.subjects.size() - 1);
		this.subjects.add(subject);
	}

	public ElementName getTeachers(JSONObject userDataList) {
		if (teachers.size() > 0)
			return new ElementName(userDataList).fromIdList(teachers, TEACHER);
		else
			return new ElementName();
	}

	private void addTeacher(int teacherId) {
		this.teachers.add(teacherId);
	}

	public ElementName getRooms(JSONObject userDataList) {
		if (rooms.size() > 0)
			return new ElementName(userDataList).fromIdList(rooms, ROOM);
		else
			return new ElementName();
	}

	private void addRoom(int room) {
		if (isCancelled() && this.rooms.size() > 0)
			this.rooms.remove(this.rooms.size() - 1);
		this.rooms.add(room);
	}

	public ElementName getHolidays(JSONObject userDataList) {
		if (holidays.size() > 0)
			return new ElementName(userDataList).fromIdList(holidays, HOLIDAY);
		else
			return new ElementName();
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

	public List<String> getCodes() {
		return codes;
	}

	private void setCodes(JSONArray codes) {
		for (int i = 0; i < codes.length(); i++)
			if (!this.codes.contains(codes.optString(i)))
				this.codes.add(codes.optString(i));
	}

	public boolean isHidden() {
		return hidden;
	}

	private void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public int getBackColor() {
		return this.backColor;
	}

	private void setBackColor(String backColor) {
		this.backColor = parseColor(backColor);
	}

	private void addInfo(String info) {
		this.infos.add(info);
	}

	public boolean isCancelled() {
		return codes.contains(Constants.TimetableItem.CODE_CANCELLED);
	}

	public boolean isIrregular() {
		return codes.contains(Constants.TimetableItem.CODE_IRREGULAR);
	}

	public boolean isExam() {
		return codes.contains(Constants.TimetableItem.CODE_EXAM);
	}

	public boolean mergeWith(ArrayList<TimetableItemData> items) {
		boolean merged = false;
		for (int i = 0; i < items.size(); i++) {
			TimetableItemData candidate = items.get(i);

			if (this.equalsIgnoreTime(candidate)) {
				setEndDateTime(candidate.getEndDateTime());
				candidate.setHidden(true);
				merged = true;
			}
		}
		return merged;
	}

	private List<Integer> getClasses() {
		return classes;
	}

	private List<Integer> getSubjects() {
		return subjects;
	}

	private List<Integer> getTeachers() {
		return teachers;
	}

	private List<Integer> getRooms() {
		return rooms;
	}

	public List<String> getInfos() {
		return infos;
	}

	private void setInfos(JSONObject infos) throws JSONException {
		if (infos.optString("lesson").length() > 0)
			addInfo(infos.getString("lesson"));
		else if (infos.optString("substitution").length() > 0)
			addInfo(infos.getString("substitution"));
		else if (infos.optString("info").length() > 0)
			addInfo(infos.getString("info"));
	}

	public boolean equalsIgnoreTime(TimetableItemData secondItem) {
		return getTeachers().equals(secondItem.getTeachers())
				&& getRooms().equals(secondItem.getRooms())
				&& getClasses().equals(secondItem.getClasses())
				&& getSubjects().equals(secondItem.getSubjects())
				&& getCodes().equals(secondItem.getCodes())
				&& getInfos().equals(secondItem.getInfos())
				&& !isHidden();
	}

	private void addClass(List<Integer> classes) {
		for (int singleClass : classes)
			addClass(singleClass);
	}

	private void addSubject(List<Integer> subjects) {
		for (int subject : subjects)
			addSubject(subject);
	}

	private void addTeacher(List<Integer> teachers) {
		for (int teacher : teachers)
			addTeacher(teacher);
	}

	private void addRoom(List<Integer> rooms) {
		for (int room : rooms)
			addRoom(room);
	}

	public boolean isDummy() {
		return dummy;
	}

	public void setDummy(boolean dummy) {
		this.dummy = dummy;
	}
}