package com.perflyst.untis.test;

import com.perflyst.untis.utils.ElementName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ElementNameTest {
	@Rule
	public ExpectedException exceptionGrabber = ExpectedException.none();

	@Test
	public void elementName_constructor() {
		ElementName elementName = new ElementName();
		assertThat(elementName, notNullValue());
		//elementName.fromIdList(new ArrayList<>(), ElementName.ElementType.UNKNOWN);
	}

	@Test
	public void elementName_getTypeName() {
		assertThat(ElementName.getTypeName(ElementName.ElementType.UNKNOWN), nullValue());
		assertThat(ElementName.getTypeName(ElementName.ElementType.CLASS), is("klassen"));
		assertThat(ElementName.getTypeName(ElementName.ElementType.TEACHER), is("teachers"));
		assertThat(ElementName.getTypeName(ElementName.ElementType.SUBJECT), is("subjects"));
		assertThat(ElementName.getTypeName(ElementName.ElementType.ROOM), is("rooms"));
		assertThat(ElementName.getTypeName(ElementName.ElementType.STUDENT), is("students"));
		assertThat(ElementName.getTypeName(ElementName.ElementType.HOLIDAY), is("holidays"));
	}

	@Test
	public void elementName_elementType_fromValue() {
		assertThat(ElementName.ElementType.fromValue(0), is(ElementName.ElementType.UNKNOWN));
		assertThat(ElementName.ElementType.fromValue(1), is(ElementName.ElementType.CLASS));
		assertThat(ElementName.ElementType.fromValue(2), is(ElementName.ElementType.TEACHER));
		assertThat(ElementName.ElementType.fromValue(3), is(ElementName.ElementType.SUBJECT));
		assertThat(ElementName.ElementType.fromValue(4), is(ElementName.ElementType.ROOM));
		assertThat(ElementName.ElementType.fromValue(5), is(ElementName.ElementType.STUDENT));
		assertThat(ElementName.ElementType.fromValue(6), is(ElementName.ElementType.HOLIDAY));
		assertThat(ElementName.ElementType.fromValue(7), is(ElementName.ElementType.UNKNOWN));
	}

	@Test
	public void elementName_constructorWithUserData_fromIdList() throws JSONException {
		JSONObject userData = generateExampleUserData();

		ElementName elementName = new ElementName(userData);
		assertThat(elementName, notNullValue());
	}

	@Test
	public void elementName_constructorWithTypeAndUserData_findFieldByValue() throws JSONException {
		JSONObject userData = generateExampleUserData();

		ElementName elementName = new ElementName(ElementName.ElementType.TEACHER, userData);
		assertThat(elementName, notNullValue());

		assertThat(elementName.findFieldByValue(null, 123, "firstName"), nullValue());
		assertThat(elementName.findFieldByValue("id", null, "firstName"), nullValue());
		assertThat(elementName.findFieldByValue("id", 123, null), nullValue());
		assertThat(elementName.findFieldByValue("id", 123, "firstName"), is("Better"));
	}

	@Test
	public void elementName_constructorWithTypeAndUserData_findFieldByValue_exception1() throws JSONException {
		JSONObject userData = generateExampleUserData();

		ElementName elementName = new ElementName(ElementName.ElementType.TEACHER, userData);

		exceptionGrabber.expect(JSONException.class);
		exceptionGrabber.expectMessage("JSONObject[\"notExisting\"] not found.");
		elementName.findFieldByValue("notExisting", 123, "firstName");
	}

	@Test
	public void elementName_constructorWithTypeAndUserData_findFieldByValue_exception2() throws JSONException {
		JSONObject userData = generateExampleUserData();

		ElementName elementName = new ElementName(ElementName.ElementType.TEACHER, userData);

		exceptionGrabber.expect(JSONException.class);
		exceptionGrabber.expectMessage("JSONObject[\"notExisting\"] not found.");
		elementName.findFieldByValue("id", 123, "notExisting");
	}

	@Test
	public void elementName_constructorWithTypeAndUserData_findFieldByValue_exception3() throws JSONException {
		JSONObject userData = generateExampleUserData();

		userData.getJSONObject("masterData").getJSONArray("teachers").remove(0);

		ElementName elementName = new ElementName(ElementName.ElementType.TEACHER, userData);

		exceptionGrabber.expect(JSONException.class);
		exceptionGrabber.expectMessage("Data array empty");
		elementName.findFieldByValue("id", 123, "firstName");
	}

	private JSONObject generateExampleUserData() throws JSONException {
		JSONObject userData = new JSONObject();
		JSONObject masterData = new JSONObject();
		JSONArray teacherArray = new JSONArray();
		JSONObject teacherObject = new JSONObject();

		teacherObject.put("id", 123);
		teacherObject.put("name", "UNTB");
		teacherObject.put("firstName", "Better");
		teacherObject.put("lastName", "Untis");
		teacherObject.put("departmentIds", new JSONArray("[10]"));

		teacherArray.put(teacherObject);
		masterData.put("teachers", teacherArray);
		userData.put("masterData", masterData);
		return userData;
	}
}
