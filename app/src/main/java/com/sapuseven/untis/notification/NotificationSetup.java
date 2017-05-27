package com.sapuseven.untis.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.TimegridUnitManager;
import com.sapuseven.untis.utils.Timetable;
import com.sapuseven.untis.utils.TimetableItemData;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.sapuseven.untis.utils.Authentication.getAuthElement;
import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;

public class NotificationSetup extends BroadcastReceiver {
	private ListManager listManager;
	private Context context;
	private SessionInfo sessionInfo;
	private int startDateFromWeek;

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d("BetterUntis", "NotificationSetup received");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("preference_notifications_enable", true))
			return;
		listManager = new ListManager(context);
		this.context = context;
		sessionInfo = new SessionInfo();

		try {
			sessionInfo.setDataFromJsonObject(new JSONObject(listManager.readList("userData", false)).optJSONObject("userData"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), 0).getTime()));

		prefs = context.getSharedPreferences("login_data", MODE_PRIVATE);
		Request request = new Request(prefs.getString("url", null));
		request.setSchool(prefs.getString("school", null));
		request.setParams("[{\"id\":\"" + sessionInfo.getElemId() + "\"," +
				"\"type\":\"" + sessionInfo.getElemType() + "\"," +
				"\"startDate\":" + startDateFromWeek + "," +
				"\"endDate\":" + addDaysToInt(startDateFromWeek, 4) + "," +
				"\"masterDataTimestamp\":" + System.currentTimeMillis() + "," +
				getAuthElement(prefs.getString("user", ""), prefs.getString("key", "")) +
				"}]");
		Log.d("BetterUntis", "Executing request...");
		request.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void setup(JSONObject data) {
		Calendar c = Calendar.getInstance();
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		JSONObject userDataList = new JSONObject();
		try {
			userDataList = new JSONObject(listManager.readList("userData", false));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		Timetable timetable = new Timetable();
		timetable.setFromJsonObject(data.optJSONObject("timetable"));
		timetable.prepareData(userDataList);
		ArrayList<TimetableItemData> timetableData = timetable.getData();
		TimegridUnitManager unitManager = new TimegridUnitManager();
		try {
			unitManager.setList(new JSONObject(listManager.readList("userData", false)).getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days"));
		} catch (JSONException e) {
			e.printStackTrace();
		}

		int dim = unitManager.getUnitCount();
		ArrayList<TimetableItemData> result = new ArrayList<>();

		for (int x = 0; x < dim; x++) {
			startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.US)
					.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), 0).getTime()));

			Calendar hour = Calendar.getInstance();
			try {
				hour.setTime(new SimpleDateFormat("HH:mm", Locale.US).parse(unitManager.getUnits().get(x).getStartTime()));
				c.set(Calendar.HOUR_OF_DAY, hour.get(Calendar.HOUR_OF_DAY));
				c.set(Calendar.MINUTE, hour.get(Calendar.MINUTE));

				String startDateTime = DateOperations.formatToISO(c.getTime());
				int index = timetable.indexOf(startDateTime, unitManager);
				if (index > 0)
					result.add(timetableData.get(index));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		if (!prefs.getBoolean("preference_notifications_in_multiple", false))
			for (int x = 0; x < result.size(); x++) {
				int j = 1;
				while (x + j < result.size()
						&& result.get(x).getTeachers(userDataList).getNames().equals(result.get(x + j).getTeachers(userDataList).getNames())
						&& result.get(x).getRooms(userDataList).getNames().equals(result.get(x + j).getRooms(userDataList).getNames())
						&& result.get(x).getSubjects(userDataList).getNames().equals(result.get(x + j).getSubjects(userDataList).getNames())
						&& result.get(x).getCodes().equals(result.get(x + j).getCodes())
						&& (!result.get(x).isEmpty(userDataList)))
					j++;
				if (dim > x + j - 1 && result.get(x + j - 1).getEndDateTime() != null)
					result.get(x).setEndDateTime(result.get(x + j - 1).getEndDateTime());
			}

		Set<String> unique = new HashSet<>();
		ArrayList<TimetableItemData> validLessons = new ArrayList<>();
		for (TimetableItemData itemData : result) {
			if (itemData.getEndDateTime().substring(4).replaceAll("[^0-9]", "").length() > 0) {
				int length = unique.size();
				unique.add(itemData.getEndDateTime());
				if (unique.size() > length) {
					validLessons.add(itemData);
				}
			}
		}

		for (int j = 0; j < validLessons.size() - 1; j++) {
			Calendar c1 = Calendar.getInstance();
			String endDateTime = validLessons.get(j).getEndDateTime();
			c1.set(Integer.parseInt(endDateTime.substring(0, 4)), Integer.parseInt(endDateTime.substring(5, 7)) - 1, Integer.parseInt(endDateTime.substring(8, 10)), Integer.parseInt(endDateTime.substring(11, 13)), Integer.parseInt(endDateTime.substring(14, 16)));

			Calendar c2 = Calendar.getInstance();
			String startDateTime = validLessons.get(j + 1).getStartDateTime();
			c2.set(Integer.parseInt(startDateTime.substring(0, 4)), Integer.parseInt(startDateTime.substring(5, 7)) - 1, Integer.parseInt(startDateTime.substring(8, 10)), Integer.parseInt(startDateTime.substring(11, 13)), Integer.parseInt(startDateTime.substring(14, 16)));

			if (c2.getTimeInMillis() > System.currentTimeMillis()) {
				Intent i1 = new Intent(context, NotificationReceiver.class)
						.putExtra("id", (int) (c2.getTimeInMillis() * 0.001))
						.putExtra("startTime", c1.getTimeInMillis())
						.putExtra("endTime", c2.getTimeInMillis())
						.putExtra("nextSubject", validLessons.get(j + 1).getSubjects(userDataList).getName(ElementName.FULL))
						.putExtra("nextSubjectLong", validLessons.get(j + 1).getSubjects(userDataList).getLongName(ElementName.FULL))
						.putExtra("nextRoom", validLessons.get(j + 1).getRooms(userDataList).getName(ElementName.FULL))
						.putExtra("nextRoomLong", validLessons.get(j + 1).getRooms(userDataList).getLongName(ElementName.FULL))
						.putExtra("nextTeacher", validLessons.get(j + 1).getTeachers(userDataList).getName(ElementName.FULL))
						.putExtra("nextTeacherLong", validLessons.get(j + 1).getTeachers(userDataList).getLongName(ElementName.FULL))
						.putExtra("clear", false);
				PendingIntent pi1 = PendingIntent.getBroadcast(context, Integer.parseInt(validLessons.get(j).getEndDateTime().substring(4).replaceAll("[^0-9]", "")), i1, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					alarmManager.setExact(AlarmManager.RTC_WAKEUP, c1.getTimeInMillis(), pi1);
				} else {
					alarmManager.set(AlarmManager.RTC_WAKEUP, c1.getTimeInMillis(), pi1);
				}
				Log.d("BetterUntis", "notification scheduled at: " + c1.get(Calendar.HOUR_OF_DAY) + ":" + c1.get(Calendar.MINUTE) + "\nduration: " + (c2.getTimeInMillis() - c1.getTimeInMillis()) / 1000 / 60);

				Intent i2 = new Intent(context, NotificationReceiver.class)
						.putExtra("id", (int) (c2.getTimeInMillis() * 0.001))
						.putExtra("clear", true);
				PendingIntent pi2 = PendingIntent.getBroadcast(context, Integer.parseInt(validLessons.get(j).getEndDateTime().substring(4).replaceAll("[^0-9]", "")) + 1, i2, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					alarmManager.setExact(AlarmManager.RTC_WAKEUP, c2.getTimeInMillis(), pi2);
				} else {
					alarmManager.set(AlarmManager.RTC_WAKEUP, c2.getTimeInMillis(), pi2);
				}
				Log.d("BetterUntis", "notification will be cancelled at " + c2.get(Calendar.HOUR_OF_DAY) + ":" + c2.get(Calendar.MINUTE));
			} else
				Log.d("BetterUntis", "notification not scheduled at: " + c1.get(Calendar.HOUR_OF_DAY) + ":" + c1.get(Calendar.MINUTE));
		}
	}

	private class Request extends AsyncTask<Void, Void, String> {
		private static final String jsonrpc = "2.0";
		private final String method = "getTimetable2017";
		private String url = "";
		private String params = "{}";
		private String school = "";

		private String json;

		Request(String url) {
			this.url = "https://" + url + "/WebUntis/jsonrpc_intern.do";
		}

		void setSchool(String school) {
			this.school = school;
		}

		void setParams(String params) {
			this.params = params;
		}

		@SuppressWarnings("deprecation")
		@Override
		protected String doInBackground(Void... p1) {
			String fileName = sessionInfo.getElemType() + "-" + sessionInfo.getElemId() + "-" + startDateFromWeek + "-" + addDaysToInt(startDateFromWeek, 4);
			if (listManager.exists(fileName, true)) {
				return listManager.readList(fileName, true);
			}

			InputStream inputStream;
			String result;
			try {
				HttpClient httpclient = new DefaultHttpClient();
				String url = this.url;
				if (this.school.length() > 0)
					url += "?school=" + this.school;
				HttpPost httpPost = new HttpPost(url);

				json = "";
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", 0);
				jsonObject.put("method", this.method);
				jsonObject.put("params", new JSONArray(this.params));
				jsonObject.put("jsonrpc", jsonrpc);
				json = jsonObject.toString();

				StringEntity se = new StringEntity(json);
				httpPost.setEntity(se);
				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");

				HttpResponse httpResponse = httpclient.execute(httpPost);
				inputStream = httpResponse.getEntity().getContent();
				if (inputStream != null)
					result = inputStreamToString(inputStream);
				else
					result = "{}";

			} catch (Exception e) {
				result = "{\"id\":\"0\",\"error\":{\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}}";
			}
			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			Log.d("BetterUntis", "Request executed, result: " + result.substring(0, Math.min(255, result.length())));
			try {
				JSONObject jsonObj = new JSONObject(result);
				if (jsonObj.has("result")) {
					setup(jsonObj.getJSONObject("result"));
					String fileName = sessionInfo.getElemType() + "-" + sessionInfo.getElemId() + "-" + startDateFromWeek + "-" + addDaysToInt(startDateFromWeek, 4);
					listManager.saveList(fileName, result, true);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		private String inputStreamToString(InputStream inputStream) throws IOException {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = bufferedReader.readLine()) != null)
				result.append(line);

			inputStream.close();
			return result.toString();
		}
	}
}