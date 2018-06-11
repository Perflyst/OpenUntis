package com.sapuseven.untis.activity;

import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.danielnilsson9.colorpickerview.dialog.ColorPickerDialogFragment;
import com.github.danielnilsson9.colorpickerview.preference.ColorPreference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sapuseven.untis.BuildConfig;
import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.BetterToast;
import com.sapuseven.untis.utils.ColorPreferenceList;
import com.sapuseven.untis.utils.DisplayChangelog;
import com.sapuseven.untis.utils.ListManager;

import java.util.List;

import static com.sapuseven.untis.utils.ThemeUtils.restartApplication;
import static com.sapuseven.untis.utils.ThemeUtils.setupBackground;
import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityPreferences extends com.sapuseven.untis.activity.appcompat.ActivityPreferences
		implements ColorPickerDialogFragment.ColorPickerDialogListener {

	private static StylingFragment sStylingFragment;

	private static void restartOnExit(Context context) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = sharedPrefs.edit();
		editor.putBoolean("restart", true);
		editor.apply();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setupActionBar();
		setupBackground(this);
	}

	private void setupActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		loadHeadersFromResource(R.xml.prefs_headers, target);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false)
				&& sharedPrefs.getBoolean("preference_dark_theme", false))
			getListView().setBackgroundColor(Color.BLACK);
	}

	protected boolean isValidFragment(String fragmentName) {
		return PreferenceFragment.class.getName().equals(fragmentName)
				|| StylingFragment.class.getName().equals(fragmentName)
				|| NotificationsFragment.class.getName().equals(fragmentName)
				|| RoomFinderFragment.class.getName().equals(fragmentName)
				|| TimetableFragment.class.getName().equals(fragmentName)
				|| AccountFragment.class.getName().equals(fragmentName)
				|| AboutFragment.class.getName().equals(fragmentName);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			if (!super.onMenuItemSelected(featureId, item))
				finish();
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onColorSelected(int dialogId, int color) {
		sStylingFragment.onColorSelected(dialogId, color);
	}

	@Override
	public void onDialogDismissed(int dialogId) {
		sStylingFragment.onDialogDismissed(dialogId);
	}

	public static class StylingFragment extends PreferenceFragment
			implements ColorPickerDialogFragment.ColorPickerDialogListener {
		private BetterToast toast;
		private Preference.OnPreferenceClickListener resetColorsListener;
		private ColorPreferenceList colorPrefs;

		public StylingFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			toast = new BetterToast(this.getActivity());
			addPreferencesFromResource(R.xml.prefs_styling);
			sStylingFragment = this;
			resetColorsListener = preference -> {
				SharedPreferences.Editor editor = PreferenceManager
						.getDefaultSharedPreferences(StylingFragment.this.getActivity()).edit();
				editor.remove("preference_background_regular").apply();
				editor.remove("preference_background_regular_past").apply();
				editor.remove("preference_background_exam").apply();
				editor.remove("preference_background_exam_past").apply();
				editor.remove("preference_background_irregular").apply();
				editor.remove("preference_background_irregular_past").apply();
				editor.remove("preference_background_cancelled").apply();
				editor.remove("preference_background_cancelled_past").apply();
				editor.remove("preference_background_free").apply();
				editor.remove("preference_marker").apply();
				toast.showToast(R.string.toast_colors_reset, Toast.LENGTH_LONG);
				setPreferenceScreen(null);
				addPreferencesFromResource(R.xml.prefs_styling);
				setupEverything();
				restartOnExit(getActivity());
				return true;
			};
			setupEverything();
		}

		private void setupEverything() {
			setupEnabledItemsOnThemeBackground(
					getPreferenceManager().getSharedPreferences()
							.getBoolean("preference_use_theme_background",
									getResources().getBoolean(
											getResources().getIdentifier("preference_use_theme_background_default", "bool", getActivity().getPackageName())
									)
							)
			);
			setupChangeListeners();
		}

		private void setupChangeListeners() {
			setupColorPickers();

			String[] preferencesNeedingRefresh = new String[]{
					"preference_alternating_days",
					"preference_alternating_hours",
					"preference_alternating_colors_use_custom",
					"preference_timetable_item_text_light",
					"preference_use_default_background",
					"preference_use_theme_background",
					"preference_timetable_colors_reset",
					"preference_timetable_hide_cancelled",
					"preference_theme",
					"preference_dark_theme",
					"preference_dark_theme_amoled"
			};

			for (String prefKey : preferencesNeedingRefresh)
				findPreference(prefKey).setOnPreferenceChangeListener(
						(preference, newValue) -> {
							restartOnExit(getActivity());
							return true;
						});

			findPreference("preference_use_theme_background").setOnPreferenceChangeListener(
					(preference, newValue) -> {
						setupEnabledItemsOnThemeBackground((Boolean) newValue);
						return true;
					});
		}

		private void setupEnabledItemsOnThemeBackground(boolean preferenceStatus) {
			findPreference("preference_background_regular").setEnabled(!preferenceStatus);
			findPreference("preference_background_regular_past").setEnabled(!preferenceStatus);
		}

		private void setupColorPickers() {
			colorPrefs = new ColorPreferenceList();
			colorPrefs.add("preference_background_regular");
			colorPrefs.add("preference_background_regular_past");
			colorPrefs.add("preference_background_irregular");
			colorPrefs.add("preference_background_irregular_past");
			colorPrefs.add("preference_background_cancelled");
			colorPrefs.add("preference_background_cancelled_past");
			colorPrefs.add("preference_background_exam");
			colorPrefs.add("preference_background_exam_past");
			colorPrefs.add("preference_background_free");
			colorPrefs.add("preference_marker");
			colorPrefs.add("preference_alternating_color");

			for (int i = 0; i < colorPrefs.size(); i++) {
				final String key = colorPrefs.getKey(i);
				final int id = i;
				((ColorPreference) findPreference(key)).setOnShowDialogListener(
						(title, currentColor) -> new ColorPickerDialogFragment.Builder(id, currentColor)
								.title(title)
								.showHexadecimalInput()
								.build()
								.show(getFragmentManager(), key + "_dialog"));
			}

			Preference reset = findPreference("preference_timetable_colors_reset");
			reset.setOnPreferenceClickListener(resetColorsListener);
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}

		@Override
		public void onColorSelected(int dialogId, int color) {
			ColorPreference pref = (ColorPreference) findPreference(colorPrefs.getKey(dialogId));

			if (pref != null) {
				pref.saveValue(color);
				restartOnExit(getActivity());
			}
		}

		@Override
		public void onDialogDismissed(int dialogId) {
			// No changes
		}
	}

	public static class NotificationsFragment extends PreferenceFragment {

		public NotificationsFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_notifications);

			findPreference("preference_notifications_enable").setOnPreferenceClickListener(
					preference -> {
						if (!preference.isEnabled()) {
							NotificationManager notificationManager = ((NotificationManager) getActivity()
									.getSystemService(Context.NOTIFICATION_SERVICE));
							if (notificationManager != null)
								notificationManager.cancelAll();
						}
						return true;
					});

			findPreference("preference_notifications_clear").setOnPreferenceClickListener(
					preference -> {
						if (!preference.isEnabled()) {
							NotificationManager notificationManager = ((NotificationManager) getActivity()
									.getSystemService(Context.NOTIFICATION_SERVICE));
							if (notificationManager != null)
								notificationManager.cancelAll();
						}
						return true;
					});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}

	public static class RoomFinderFragment extends PreferenceFragment {
		public RoomFinderFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_roomfinder);

			String[] roomList = ActivityRoomFinder.getRooms(getActivity(), true)
					.toArray(new String[0]);
			((ListPreference) findPreference("preference_room_to_display_in_free_lessons"))
					.setEntries(roomList);
			((ListPreference) findPreference("preference_room_to_display_in_free_lessons"))
					.setEntryValues(roomList);

			findPreference("preference_room_to_display_in_free_lessons")
					.setOnPreferenceChangeListener((preference, newValue) -> {
						restartOnExit(getActivity());
						return true;
					});

			findPreference("preference_room_to_display_in_free_lessons_trim")
					.setOnPreferenceChangeListener((preference, newValue) -> {
						restartOnExit(getActivity());
						return true;
					});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}

			return super.onOptionsItemSelected(item);
		}
	}

	public static class AccountFragment extends PreferenceFragment {
		private BetterToast toast;

		public AccountFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			toast = new BetterToast(this.getActivity());
			addPreferencesFromResource(R.xml.prefs_account);

			final SharedPreferences prefs = getActivity()
					.getSharedPreferences("login_data", MODE_PRIVATE);
			Preference prefKey = findPreference("preference_account_access_key");
			prefKey.setSummary(prefs.getString("key", "UNKNOWN"));
			prefKey.setOnPreferenceClickListener(preference -> {
				ClipboardManager clipboard = (ClipboardManager) getActivity()
						.getSystemService(Context.CLIPBOARD_SERVICE);
				if (clipboard != null) {
					ClipData clip = ClipData.newPlainText(getString(R.string.preference_account_access_key),
							prefs.getString("key", "UNKNOWN"));
					clipboard.setPrimaryClip(clip);
					toast.showToast(R.string.key_copied, Toast.LENGTH_SHORT);
					return true;
				} else {
					toast.showToast(R.string.key_copy_failed, Toast.LENGTH_SHORT);
					return false;
				}
			});

			Preference prefFirebaseKey = findPreference("preference_account_firebase_key");
			if (BuildConfig.ENABLE_FIREBASE) {
				prefFirebaseKey.setSummary(FirebaseInstanceId.getInstance().getToken());
			} else {
				prefFirebaseKey.setSummary("(disabled)");
			}
			prefFirebaseKey.setOnPreferenceClickListener(preference -> {
				ClipboardManager clipboard = (ClipboardManager) getActivity()
						.getSystemService(Context.CLIPBOARD_SERVICE);
				if (clipboard != null) {
					ClipData clip = ClipData.newPlainText(getString(R.string.firebase_key),
							FirebaseInstanceId.getInstance().getToken());
					clipboard.setPrimaryClip(clip);
					toast.showToast(R.string.key_copied, Toast.LENGTH_SHORT);
					return true;
				} else {
					toast.showToast(R.string.key_copy_failed, Toast.LENGTH_SHORT);
					return false;
				}
			});

			findPreference("preference_account_logout").setOnPreferenceClickListener(
					preference -> {
						// TODO: Display a confirmation dialog
						getActivity().getSharedPreferences("login_data", MODE_PRIVATE)
								.edit().clear().apply();
						ListManager lm = new ListManager(getActivity());
						lm.delete("userData", false);
						lm.invalidateCaches();
						restartApplication(getActivity());
						return true;
					});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}

			return super.onOptionsItemSelected(item);
		}
	}

	public static class TimetableFragment extends PreferenceFragment {
		public TimetableFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_timetable);

			String[] preferencesNeedingRefresh = new String[]{
					"preference_timetable_item_padding",
					"preference_timetable_item_corner_radius",
					"preference_timetable_centered_lesson_info",
					"preference_timetable_bold_lesson_name",
					"preference_timetable_lesson_name_font_size",
					"preference_timetable_lesson_info_font_size"
			};

			for (String prefKey : preferencesNeedingRefresh)
				findPreference(prefKey).setOnPreferenceChangeListener(
						(preference, newValue) -> {
							restartOnExit(getActivity());
							return true;
						});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}

			return super.onOptionsItemSelected(item);
		}
	}

	public static class AboutFragment extends PreferenceFragment {
		private int clicks;

		public AboutFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.prefs_about);
			findPreference("preference_info_contact_email").setOnPreferenceClickListener(
					preference -> {
						SharedPreferences prefs = getActivity()
								.getSharedPreferences("login_data", MODE_PRIVATE);
						Intent i3 = new Intent(Intent.ACTION_SEND);
						i3.setType("plain/text");
						i3.putExtra(Intent.EXTRA_EMAIL,
								new String[]{getString(R.string.contact_email)});
						i3.putExtra(Intent.EXTRA_SUBJECT, "BetterUntis Feedback from user "
								+ prefs.getString("user", "UNKNOWN"));
						startActivity(Intent.createChooser(i3, getString(R.string.give_feedback)));
						return true;
					});

			final Preference prefVersion = findPreference("preference_info_app_version");
			prefVersion.setSummary(getString(R.string.app_version_full,
					BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
			prefVersion.setOnPreferenceClickListener(preference -> {
				clicks++;
				if (clicks > BuildConfig.VERSION_CODE)
					prefVersion.setSummary(getString(R.string.app_version_full, BuildConfig.VERSION_NAME, clicks));
				return true;
			});

			findPreference("preference_info_changelog").setOnPreferenceClickListener(
					preference -> {
						new DisplayChangelog(getActivity())
								.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
						return true;
					});

			findPreference("preference_send_stats").setOnPreferenceClickListener(
					preference -> {
						// TODO: Disable the Preference and send a request to the server with the current settings and the message to log out.
						// After that re-enable the preference and show a toast like "You opted out".
						return false;
					});

			findPreference("preference_info_stats").setOnPreferenceClickListener(
					preference -> {
						// TODO: Show a dialog with this information
						return false;
					});
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				startActivity(new Intent(getActivity(), ActivityPreferences.class));
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}
}