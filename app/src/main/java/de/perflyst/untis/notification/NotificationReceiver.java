package de.perflyst.untis.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import de.perflyst.untis.R;
import de.perflyst.untis.activity.ActivityMain;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {
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
			if (prefs.getString("preference_notifications_visibility_subjects", "long").equals("long"))
				message.append(context.getString(R.string.notification_subjects, intent.getStringExtra("nextSubjectLong")));
			else if (prefs.getString("preference_notifications_visibility_subjects", "long").equals("short"))
				message.append(context.getString(R.string.notification_subjects, intent.getStringExtra("nextSubject")));

			if (prefs.getString("preference_notifications_visibility_rooms", "short").equals("long")) {
				if (message.length() > 0)
					message.append(" / ");
				message.append(context.getString(R.string.notification_rooms, intent.getStringExtra("nextRoomLong")));
			} else if (prefs.getString("preference_notifications_visibility_rooms", "short").equals("short")) {
				if (message.length() > 0)
					message.append(" / ");
				message.append(context.getString(R.string.notification_rooms, intent.getStringExtra("nextRoom")));
			}

			if (prefs.getString("preference_notifications_visibility_teachers", "short").equals("long")) {
				if (message.length() > 0)
					message.append(" / ");
				message.append(context.getString(R.string.notification_teachers, intent.getStringExtra("nextTeacherLong")));
			} else if (prefs.getString("preference_notifications_visibility_teachers", "short").equals("short")) {
				if (message.length() > 0)
					message.append(" / ");
				message.append(context.getString(R.string.notification_teachers, intent.getStringExtra("nextTeacher")));
			}

			StringBuilder longMessage = new StringBuilder();
			if (prefs.getString("preference_notifications_visibility_subjects", "long").equals("long"))
				longMessage.append(context.getString(R.string.notification_subjects, intent.getStringExtra("nextSubjectLong")));
			else if (prefs.getString("preference_notifications_visibility_subjects", "long").equals("short"))
				longMessage.append(context.getString(R.string.notification_subjects, intent.getStringExtra("nextSubject")));

			if (prefs.getString("preference_notifications_visibility_rooms", "short").equals("long")) {
				if (longMessage.length() > 0)
					longMessage.append('\n');
				longMessage.append(context.getString(R.string.notification_rooms, intent.getStringExtra("nextRoomLong")));
			} else if (prefs.getString("preference_notifications_visibility_rooms", "short").equals("short")) {
				if (longMessage.length() > 0)
					longMessage.append('\n');
				longMessage.append(context.getString(R.string.notification_rooms, intent.getStringExtra("nextRoom")));
			}

			if (prefs.getString("preference_notifications_visibility_teachers", "short").equals("long")) {
				if (longMessage.length() > 0)
					longMessage.append('\n');
				longMessage.append(context.getString(R.string.notification_teachers, intent.getStringExtra("nextTeacherLong")));
			} else if (prefs.getString("preference_notifications_visibility_teachers", "short").equals("short")) {
				if (longMessage.length() > 0)
					longMessage.append('\n');
				longMessage.append(context.getString(R.string.notification_teachers, intent.getStringExtra("nextTeacher")));
			}

			Notification n;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
				n = new Notification.Builder(context)
						.setContentTitle(title)
						.setContentText(message)
						.setSmallIcon(R.drawable.ic_stat_timetable)
						.setContentIntent(pIntent)
						.setStyle(new Notification.BigTextStyle().bigText(longMessage))
						.setAutoCancel(false)
						.setOngoing(true)
						.setCategory(Notification.CATEGORY_STATUS)
						.build();
				n.visibility = Notification.VISIBILITY_PUBLIC;
			} else {
				n = new Notification.Builder(context)
						.setContentTitle(title)
						.setContentText(message)
						.setSmallIcon(R.drawable.ic_stat_timetable)
						.setContentIntent(pIntent)
						.setStyle(new Notification.BigTextStyle().bigText(longMessage))
						.setAutoCancel(false)
						.setOngoing(true)
						.build();
			}
			if (notificationManager != null)
				notificationManager.notify(intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)), n);
			else
				Log.d("NotificationManager", "Failed to notify (notificationManager == null)");
		}
	}
}