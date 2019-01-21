package de.perflyst.untis.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import de.perflyst.untis.R;
import de.perflyst.untis.activity.ActivityMain;

import java.util.Calendar;

public class NotificationReceiver extends BroadcastReceiver {

	private final String NEXT_LESSON_CHANNEL = "next_lesson";

	private static int CURRENT_INTERRUPTION_FILTER;
	private static int CURRENT_RINGER_MODE;

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
		final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		if (notificationManager == null) {
			Log.d("NotificationManager", "Failed to get notification manager (notificationManager == null)");
			return;
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
					NEXT_LESSON_CHANNEL,
					context.getString(R.string.preference_notifications_channel_title),
					NotificationManager.IMPORTANCE_DEFAULT);
			channel.setDescription(context.getString(R.string.preference_notifications_enable_desc));
			notificationManager.createNotificationChannel(channel);
		}
		boolean clear = intent.getBooleanExtra("clear", false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean setDoNotDisturb = prefs.getBoolean("preference_notifications_do_not_disturb", false);
		boolean showNextLesson = prefs.getBoolean("preference_notifications_enable", true);
		if ((!showNextLesson && !setDoNotDisturb) ||
				(System.currentTimeMillis() > intent.getLongExtra("endTime", 0) && !clear) ||
				(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.getActiveNotifications().length > 0 && !clear))
			return;

		if (clear) {
			if (showNextLesson) {
				Log.d("NotificationReceiver", "Attempting to cancel notification #" + intent.getIntExtra("id",
						(int) (System.currentTimeMillis() * 0.001)) + "...");
				notificationManager.cancel(intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)));
			}
			if (setDoNotDisturb) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					CURRENT_INTERRUPTION_FILTER = notificationManager.getCurrentInterruptionFilter();
					notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
				} else if (audioManager != null) {
					CURRENT_RINGER_MODE = audioManager.getRingerMode();
					audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				}
			}
		} else {
			if (setDoNotDisturb) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (notificationManager.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_NONE) {
						notificationManager.setInterruptionFilter(CURRENT_INTERRUPTION_FILTER);
					}
				} else if (audioManager != null) {
					if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
						audioManager.setRingerMode(CURRENT_RINGER_MODE);
					}
				}
			}
			if (showNextLesson) {
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
				notificationManager.notify(intent.getIntExtra("id", (int) (System.currentTimeMillis() * 0.001)), n);
			}
		}
	}
}
