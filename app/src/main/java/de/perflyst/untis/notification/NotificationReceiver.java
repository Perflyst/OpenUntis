package de.perflyst.untis.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import de.perflyst.untis.R;
import de.perflyst.untis.activity.ActivityMain;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

	private final String NEXT_LESSON_CHANNEL = "next_lesson";

	@Override
	public void onReceive(Context context, final Intent intent) {
		Log.d("NotificationReceiver", "NotificationReceiver received. Extras:");
		if (intent.getExtras() != null)
			for (String key : intent.getExtras().keySet()) {
				Object value = intent.getExtras().get(key);
				if (value != null)
					Log.d("NotificationReceiver", String.format(" - %s=%s (%s)", key, value.toString(), value.getClass().getName()));
			}

		final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					NEXT_LESSON_CHANNEL,
					context.getString(R.string.preference_notifications_channel_title),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(context.getString(R.string.preference_notifications_enable_desc));
			notificationManager.createNotificationChannel(channel);
		}
		boolean clear = intent.getBooleanExtra("clear", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (!prefs.getBoolean("preference_notifications_enable", true) ||
				(System.currentTimeMillis() > intent.getLongExtra("endTime", 0) && !clear) ||
				(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null && notificationManager.getActiveNotifications().length > 0 && !clear))
			return;

		if (clear) {
			Log.d("NotificationReceiver", "Attempting to cancel notification #" + intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)) + "...");
			if (notificationManager != null)
				notificationManager.cancel(intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)));
			else
				Log.d("NotificationManager", "Failed to cancel notification (notificationManager == null)");
		} else {
			PendingIntent pIntent = PendingIntent.getActivity(context, 0, new Intent(context, ActivityMain.class), 0);
			Calendar endTime = Calendar.getInstance();
			endTime.setTimeInMillis(intent.getLongExtra("endTime", System.currentTimeMillis()));
			String title = context.getString(R.string.notification_title, endTime.get(Calendar.HOUR_OF_DAY), endTime.get(Calendar.MINUTE));
			Log.d("NotificationReceiver", "notification delivered: Break until " + endTime.get(Calendar.HOUR_OF_DAY) + ":" + endTime.get(Calendar.MINUTE));

			StringBuilder message = new StringBuilder();
			StringBuilder longMessage = new StringBuilder();

			appendInformation(message, longMessage, prefs, "preference_notifications_visibility_subjects", intent,
					"nextSubject", context, R.string.notification_subjects);

			appendInformation(message, longMessage, prefs, "preference_notifications_visibility_rooms", intent,
					"nextRoom", context, R.string.notification_rooms);

			appendInformation(message, longMessage, prefs, "preference_notifications_visibility_teachers", intent,
					"nextTeacher", context, R.string.notification_teachers);

			Notification n = new NotificationCompat.Builder(context, NEXT_LESSON_CHANNEL)
					.setContentTitle(title)
					.setContentText(message)
					.setSmallIcon(R.drawable.ic_stat_timetable)
					.setContentIntent(pIntent)
					.setStyle(new NotificationCompat.BigTextStyle().bigText(longMessage))
					.setAutoCancel(false)
					.setOngoing(true)
					.setCategory(NotificationCompat.CATEGORY_STATUS)
					.build();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				n.visibility = NotificationCompat.VISIBILITY_PUBLIC;
			if (notificationManager != null)
				notificationManager.notify(intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)), n);
			else
				Log.d("NotificationManager", "Failed to notify (notificationManager == null)");
		}
	}

	private void appendInformation(StringBuilder message, StringBuilder longMessage,
								   SharedPreferences prefs, String prefKey, Intent intent, String intentKey,
								   Context context, int formatResource) {
		String s = null;
		if (prefs.getString(prefKey, "short").equals("long")) {
			s = intent.getStringExtra(intentKey.concat("Long"));
		} else if (prefs.getString(prefKey, "short").equals("short")) {
			s = intent.getStringExtra(intentKey);
		}
		if (!TextUtils.isEmpty(s)) {
			if (message.length() > 0) {
				message.append(" / ");
			}
			if (longMessage.length() > 0) {
				longMessage.append('\n');
			}
			String text = context.getString(formatResource, s);
			message.append(text);
			longMessage.append(text);
		}
	}
}
