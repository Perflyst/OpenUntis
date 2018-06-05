package com.sapuseven.untis.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.sapuseven.untis.utils.Constants;
import com.sapuseven.untis.utils.DateOperations;
import com.sapuseven.untis.utils.ElementName;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.connectivity.UntisRequest;
import com.sapuseven.untis.utils.timetable.TimegridUnitManager;
import com.sapuseven.untis.utils.timetable.Timetable;
import com.sapuseven.untis.utils.timetable.TimetableItemData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import static android.content.Context.ALARM_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.connectivity.UntisAuthentication.getAuthObject;

public class NotificationSetup extends BroadcastReceiver {
	private ListManager listManager;
	private Context context;
	private SessionInfo sessionInfo;
	private int startDateFromWeek;

	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d("NotificationSetup", "NotificationSetup received");
		this.context = context;

		performRequest();
	}

	private void performRequest() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("preference_notifications_enable", true))
			return;

		listManager = new ListManager(context);
		sessionInfo = new SessionInfo();

		sessionInfo.setDataFromJsonObject(ListManager.getUserData(listManager).optJSONObject("userData"));

		startDateFromWeek = Integer.parseInt(new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH)
				.format(DateOperations.getStartDateFromWeek(Calendar.getInstance(), 0).getTime()));

		prefs = context.getSharedPreferences("login_data", MODE_PRIVATE);

		UntisRequest api = new UntisRequest(context);

		UntisRequest.ResponseHandler handler = response -> {
			Log.d("NotificationSetup", "Request executed, response: " + response.toString().substring(0, Math.min(255, response.toString().length())));
			try {
				if (response.has("result")) {
					setup(response.getJSONObject("result"));
					String fileName = sessionInfo.getElemType() + "-" + sessionInfo.getElemId() + "-" + startDateFromWeek + "-" + addDaysToInt(startDateFromWeek, 4);
					listManager.saveList(fileName, response.toString(), true);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		};

		JSONObject params = new JSONObject();
		try {
			params
					.put("id", sessionInfo.getElemId())
					.put("type", sessionInfo.getElemType())
					.put("startDate", startDateFromWeek)
					.put("endDate", addDaysToInt(startDateFromWeek, 4))
					.put("masterDataTimestamp", System.currentTimeMillis())
					.put("auth", getAuthObject(prefs.getString("user", ""), prefs.getString("key", "")));
		} catch (JSONException e) {
			e.printStackTrace(); // TODO: Implement proper error handling
		}

		UntisRequest.UntisRequestQuery query = new UntisRequest.UntisRequestQuery();
		query.setMethod(Constants.UntisAPI.METHOD_GET_TIMETABLE);
		query.setParams(new JSONArray().put(params));
		query.setUrl(prefs.getString("url", null));
		query.setSchool(prefs.getString("school", null));

		api.setCachingMode(UntisRequest.CachingMode.LOAD_LIVE);
		api.setResponseHandler(handler).submit(query);
	}

	private void setup(JSONObject data) {
		Calendar c = Calendar.getInstance();

		JSONObject params = new JSONObject();
		int days = 4;
		try {
			days = ListManager.getUserData(listManager).getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days").length();
		} catch (JSONException e) {
			e.printStackTrace();
		}

		try {
			params
					.put("id", sessionInfo.getElemId())
					.put("type", sessionInfo.getElemType())
					.put("startDate", startDateFromWeek)
					.put("endDate", addDaysToInt(startDateFromWeek, days))
					.put("masterDataTimestamp", System.currentTimeMillis())
					.put("auth", getAuthObject(prefs.getString("user", ""), prefs.getString("key", "")));
		} catch (JSONException e) {
			e.printStackTrace(); // TODO: Implement proper error handling
		}

		UntisRequest.UntisRequestQuery query = new UntisRequest.UntisRequestQuery();
		query.setMethod(Constants.UntisAPI.METHOD_GET_TIMETABLE);
		query.setParams(new JSONArray().put(params));
		query.setUrl(prefs.getString("url", null));
		query.setSchool(prefs.getString("school", null));

		api.setResponseHandler(handler).submit(query);
	}

	private void setup(JSONObject data) {
		Calendar c = Calendar.getInstance();

		JSONObject userDataList = ListManager.getUserData(listManager);

		TimegridUnitManager unitManager;
		try {
			unitManager = new TimegridUnitManager(userDataList.getJSONObject("masterData").getJSONObject("timeGrid").getJSONArray("days"));
		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		int day;
		try {
			day = unitManager.getDayIndex(c);
		} catch (IndexOutOfBoundsException e) {
			return;
		}

		AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

		if (alarmManager == null)
			return; // TODO: Display a notification informing the user about this issue

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		Timetable timetable = new Timetable(data, prefs);

		int hoursPerDay = unitManager.getMaxHoursPerDay();
		ArrayList<TimetableItemData> result = new ArrayList<>();

		if (day >= hoursPerDay)
			return;

		for (int currentHourOfDay = 0; currentHourOfDay < hoursPerDay; currentHourOfDay++) {
			int hour = currentHourOfDay % hoursPerDay;

			if (timetable.getItems(day, hour).size() > 0)
				result.add(TimetableItemData.combine(timetable.getItems(day, hour), timetable.getStartDateTime(day, hour), timetable.getEndDateTime(day, hour)));
		}

		if (!prefs.getBoolean("preference_notifications_in_multiple", false))
			for (int x = 0; x < result.size(); x++) {
				while (x + 1 < result.size()
						&& result.get(x).equalsIgnoreTime(result.get(x + 1))) {

					result.get(x).setEndDateTime(result.get(x + 1).getEndDateTime());
					result.remove(x + 1);
				}
			}

		/*Set<String> unique = new HashSet<>();
		ArrayList<TimetableItemData> validLessons = new ArrayList<>();
		for (TimetableItemData itemData : result) {
			if (itemData.getEndDateTime().substring(4).replaceAll("[^0-9]", "").length() > 0) {
				int length = unique.size();
				unique.add(itemData.getEndDateTime());
				if (unique.size() > length) {
					validLessons.add(itemData);
				}
			}
		}*/

		for (int j = 0; j < result.size() - 1; j++) {
			Calendar c1 = Calendar.getInstance();
			String endDateTime = result.get(j).getEndDateTime();
			c1.set(Integer.parseInt(endDateTime.substring(0, 4)), Integer.parseInt(endDateTime.substring(5, 7)) - 1, Integer.parseInt(endDateTime.substring(8, 10)), Integer.parseInt(endDateTime.substring(11, 13)), Integer.parseInt(endDateTime.substring(14, 16)));

			Calendar c2 = Calendar.getInstance();
			String startDateTime = result.get(j + 1).getStartDateTime();
			c2.set(Integer.parseInt(startDateTime.substring(0, 4)), Integer.parseInt(startDateTime.substring(5, 7)) - 1, Integer.parseInt(startDateTime.substring(8, 10)), Integer.parseInt(startDateTime.substring(11, 13)), Integer.parseInt(startDateTime.substring(14, 16)));

			if (c2.getTimeInMillis() > System.currentTimeMillis()) {
				Intent i1 = new Intent(context, NotificationReceiver.class)
						.putExtra("id", (int) (c2.getTimeInMillis() * 0.001))
						.putExtra("startTime", c1.getTimeInMillis())
						.putExtra("endTime", c2.getTimeInMillis())
						.putExtra("nextSubject", result.get(j + 1).getSubjects(userDataList).getName(ElementName.FULL))
						.putExtra("nextSubjectLong", result.get(j + 1).getSubjects(userDataList).getLongName(ElementName.FULL))
						.putExtra("nextRoom", result.get(j + 1).getRooms(userDataList).getName(ElementName.FULL))
						.putExtra("nextRoomLong", result.get(j + 1).getRooms(userDataList).getLongName(ElementName.FULL))
						.putExtra("nextTeacher", result.get(j + 1).getTeachers(userDataList).getName(ElementName.FULL))
						.putExtra("nextTeacherLong", result.get(j + 1).getTeachers(userDataList).getLongName(ElementName.FULL))
						.putExtra("clear", false);
				PendingIntent pi1 = PendingIntent.getBroadcast(context, Integer.parseInt(result.get(j).getEndDateTime().substring(4).replaceAll("[^0-9]", "")), i1, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					alarmManager.setExact(AlarmManager.RTC_WAKEUP, c1.getTimeInMillis(), pi1);
				} else {
					alarmManager.set(AlarmManager.RTC_WAKEUP, c1.getTimeInMillis(), pi1);
				}
				Log.d("NotificationSetup", "notification scheduled at: " + c1.get(Calendar.HOUR_OF_DAY) + ":" + c1.get(Calendar.MINUTE) + "\nduration: " + (c2.getTimeInMillis() - c1.getTimeInMillis()) / 1000 / 60);

				Intent i2 = new Intent(context, NotificationReceiver.class)
						.putExtra("id", (int) (c2.getTimeInMillis() * 0.001))
						.putExtra("clear", true);
				PendingIntent pi2 = PendingIntent.getBroadcast(context, Integer.parseInt(result.get(j).getEndDateTime().substring(4).replaceAll("[^0-9]", "")) + 1, i2, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					alarmManager.setExact(AlarmManager.RTC_WAKEUP, c2.getTimeInMillis(), pi2);
				} else {
					alarmManager.set(AlarmManager.RTC_WAKEUP, c2.getTimeInMillis(), pi2);
				}
				Log.d("NotificationSetup", "notification will be cancelled at " + c2.get(Calendar.HOUR_OF_DAY) + ":" + c2.get(Calendar.MINUTE));
			} else
				Log.d("NotificationSetup", "notification not scheduled at: " + c1.get(Calendar.HOUR_OF_DAY) + ":" + c1.get(Calendar.MINUTE));
		}
	}
}