package com.perflyst.untis.test;

import com.perflyst.untis.utils.ElementName;
import com.perflyst.untis.utils.SessionInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class SessionInfoTest {
	@Test
	public void sessionInfo_constructor() {
		SessionInfo sessionInfo = new SessionInfo();

		assertThat(sessionInfo.getElemId(), is(-1));
		assertThat(sessionInfo.getElemType(), is(""));
		assertThat(sessionInfo.getDisplayName(), is(""));
	}

	@Test
	public void sessionInfo_set_get() {
		SessionInfo sessionInfo = new SessionInfo();

		sessionInfo.setElemId(10);
		sessionInfo.setElemType("Test");
		sessionInfo.setDisplayName("Test");

		assertThat(sessionInfo.getElemId(), is(10));
		assertThat(sessionInfo.getElemType(), is("Test"));
		assertThat(sessionInfo.getDisplayName(), is("Test"));
	}

	@Test
	public void sessionInfo_setDataFromJsonObject() throws JSONException {
		SessionInfo sessionInfo = new SessionInfo();

		JSONObject data = new JSONObject();

		sessionInfo.setDataFromJsonObject(data);

		assertThat(sessionInfo.getElemId(), is(-1));
		assertThat(sessionInfo.getElemType(), is(""));
		assertThat(sessionInfo.getDisplayName(), is("OpenUntis"));

		data.put("elemId", 10);
		data.put("elemType", "Test");
		data.put("displayName", "Test");

		sessionInfo.setDataFromJsonObject(data);

		assertThat(sessionInfo.getElemId(), is(10));
		assertThat(sessionInfo.getElemType(), is("Test"));
		assertThat(sessionInfo.getDisplayName(), is("Test"));
	}

	@Test
	public void sessionInfo_getElemTypeName() {
		assertThat(SessionInfo.getElemTypeName(ElementName.ElementType.STUDENT), is("STUDENT"));
		assertThat(SessionInfo.getElemTypeName(ElementName.ElementType.CLASS), is("CLASS"));
		assertThat(SessionInfo.getElemTypeName(ElementName.ElementType.TEACHER), is("TEACHER"));
		assertThat(SessionInfo.getElemTypeName(ElementName.ElementType.ROOM), is("ROOM"));
		assertThat(SessionInfo.getElemTypeName(ElementName.ElementType.UNKNOWN), is(""));
	}

	@Test
	public void sessionInfo_getElemTypeId() {
		assertThat(SessionInfo.getElemTypeId("STUDENT"), is(ElementName.ElementType.STUDENT));
		assertThat(SessionInfo.getElemTypeId("CLASS"), is(ElementName.ElementType.CLASS));
		assertThat(SessionInfo.getElemTypeId("TEACHER"), is(ElementName.ElementType.TEACHER));
		assertThat(SessionInfo.getElemTypeId("ROOM"), is(ElementName.ElementType.ROOM));
		assertThat(SessionInfo.getElemTypeId(""), is(ElementName.ElementType.UNKNOWN));
	}
}
