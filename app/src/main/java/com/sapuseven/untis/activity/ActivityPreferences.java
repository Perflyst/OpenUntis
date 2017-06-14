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
import com.sapuseven.untis.BuildConfig;
import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.BetterToast;
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
				|| InfosFragment.class.getName().equals(fragmentName);
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
		static final int BACKGROUND_REGULAR_ID = 0;
		static final int BACKGROUND_REGULAR_PAST_ID = 1;
		static final int BACKGROUND_IRREGULAR_ID = 2;
		static final int BACKGROUND_IRREGULAR_PAST_ID = 3;
		static final int BACKGROUND_EXAM_ID = 4;
		static final int BACKGROUND_EXAM_PAST_ID = 5;
		static final int BACKGROUND_FREE_ID = 6;
		static final int MARKER_ID = 7;
		BetterToast toast;
		private Preference.OnPreferenceClickListener resetColorsListener;

		public StylingFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			toast = new BetterToast(this.getActivity());
			addPreferencesFromResource(R.xml.prefs_styling);
			sStylingFragment = this;
			resetColorsListener = new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences.Editor editor = PreferenceManager
							.getDefaultSharedPreferences(StylingFragment.this.getActivity()).edit();
					editor.remove("preference_background_regular").apply();
					editor.remove("preference_background_regular_past").apply();
					editor.remove("preference_background_exam").apply();
					editor.remove("preference_background_exam_past").apply();
					editor.remove("preference_background_irregular").apply();
					editor.remove("preference_background_irregular_past").apply();
					editor.remove("preference_background_free").apply();
					editor.remove("preference_marker").apply();
					toast.showToast(R.string.toast_colors_reset, Toast.LENGTH_LONG);
					setPreferenceScreen(null);
					addPreferencesFromResource(R.xml.prefs_styling);
					setupColorPickers();
					restartOnExit(getActivity());
					return true;
				}
			};
			addChangeListeners();
		}

		private void addChangeListeners() {
			setupColorPickers();

			findPreference("preference_use_default_background").setOnPreferenceChangeListener(
					new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});

			findPreference("preference_timetable_colors_reset").setOnPreferenceChangeListener(
					new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});

			findPreference("preference_theme").setOnPreferenceChangeListener(
					new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});

			findPreference("preference_dark_theme").setOnPreferenceChangeListener(
					new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});

			findPreference("preference_dark_theme_amoled").setOnPreferenceChangeListener(
					new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});
		}

		private void setupColorPickers() {
			ColorPreference preferenceBackgroundRegular =
					(ColorPreference) findPreference("preference_background_regular");
			preferenceBackgroundRegular.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_REGULAR_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(), "preference_background_regular_dialog");
				}
			});

			ColorPreference preferenceBackgroundRegularPast =
					(ColorPreference) findPreference("preference_background_regular_past");
			preferenceBackgroundRegularPast.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_REGULAR_PAST_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(),
									"preference_background_regular_past_dialog");
				}
			});

			ColorPreference preferenceBackgroundExam =
					(ColorPreference) findPreference("preference_background_exam");
			preferenceBackgroundExam.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_EXAM_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(), "preference_background_exam_dialog");
				}
			});

			ColorPreference preferenceBackgroundExamPast =
					(ColorPreference) findPreference("preference_background_exam_past");
			preferenceBackgroundExamPast.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_EXAM_PAST_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(), "preference_background_exam_past_dialog");
				}
			});

			ColorPreference preferenceBackgroundIrregular =
					(ColorPreference) findPreference("preference_background_irregular");
			preferenceBackgroundIrregular.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_IRREGULAR_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(), "preference_background_irregular_dialog");
				}
			});

			ColorPreference preferenceBackgroundIrregularPast =
					(ColorPreference) findPreference("preference_background_irregular_past");
			preferenceBackgroundIrregularPast.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment
							.Builder(BACKGROUND_IRREGULAR_PAST_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(),
									"preference_background_irregular_past_dialog");
				}
			});

			ColorPreference preferenceBackgroundFree =
					(ColorPreference) findPreference("preference_background_free");
			preferenceBackgroundFree.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(BACKGROUND_FREE_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build()
							.show(getFragmentManager(), "preference_background_free_dialog");
				}
			});

			ColorPreference preferenceBackgroundMarker =
					(ColorPreference) findPreference("preference_marker");
			preferenceBackgroundMarker.setOnShowDialogListener(
					new ColorPreference.OnShowDialogListener() {
				@Override
				public void onShowColorPickerDialog(String title, int currentColor) {
					new ColorPickerDialogFragment.Builder(MARKER_ID, currentColor)
							.title(title)
							.showHexadecimalInput()
							.build().show(getFragmentManager(), "preference_marker_dialog");
				}
			});
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
			ColorPreference pref = null;
			switch (dialogId) {
				case BACKGROUND_REGULAR_ID:
					pref = (ColorPreference) findPreference("preference_background_regular");
					break;
				case BACKGROUND_REGULAR_PAST_ID:
					pref = (ColorPreference) findPreference("preference_background_regular_past");
					break;
				case BACKGROUND_EXAM_ID:
					pref = (ColorPreference) findPreference("preference_background_exam");
					break;
				case BACKGROUND_EXAM_PAST_ID:
					pref = (ColorPreference) findPreference("preference_background_exam_past");
					break;
				case BACKGROUND_IRREGULAR_ID:
					pref = (ColorPreference) findPreference("preference_background_irregular");
					break;
				case BACKGROUND_IRREGULAR_PAST_ID:
					pref = (ColorPreference) findPreference("preference_background_irregular_past");
					break;
				case BACKGROUND_FREE_ID:
					pref = (ColorPreference) findPreference("preference_background_free");
					break;
				case MARKER_ID:
					pref = (ColorPreference) findPreference("preference_marker");
					break;
			}
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
					new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					if (!preference.isEnabled())
						((NotificationManager) getActivity()
								.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
					return true;
				}
			});

			findPreference("preference_notifications_clear").setOnPreferenceClickListener(
					new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					((NotificationManager) getActivity()
							.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
					return true;
				}
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
					.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
			});

			findPreference("preference_room_to_display_in_free_lessons_trim")
					.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					restartOnExit(getActivity());
					return true;
				}
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

	public static class InfosFragment extends PreferenceFragment {
		private int clicks;
		private BetterToast toast;

		public InfosFragment() {
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			toast = new BetterToast(this.getActivity());
			addPreferencesFromResource(R.xml.prefs_infos);
			findPreference("preference_info_contact_email").setOnPreferenceClickListener(
					new Preference.OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
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
				}
			});

			final Preference prefVersion = findPreference("preference_info_app_version");
			prefVersion.setSummary(getString(R.string.app_version_full,
					BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
			prefVersion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					clicks++;
					if (clicks > BuildConfig.VERSION_CODE)
						prefVersion.setSummary(getString(R.string.app_version_full, BuildConfig.VERSION_NAME, clicks));
					return true;
				}
			});


			findPreference("preference_info_changelog").setOnPreferenceClickListener(
					new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new DisplayChangelog(getActivity())
							.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
					return true;
				}
			});

			final SharedPreferences prefs = getActivity()
					.getSharedPreferences("login_data", MODE_PRIVATE);
			Preference prefKey = findPreference("preference_info_access_key");
			prefKey.setSummary(prefs.getString("key", "UNKNOWN"));
			prefKey.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					ClipboardManager clipboard = (ClipboardManager) getActivity()
							.getSystemService(Context.CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText(getString(R.string.access_key),
							prefs.getString("key", "UNKNOWN"));
					clipboard.setPrimaryClip(clip);
					toast.showToast(R.string.key_copied, Toast.LENGTH_SHORT);
					return true;
				}
			});

			findPreference("preference_info_logout").setOnPreferenceClickListener(
					new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getActivity().getSharedPreferences("login_data", MODE_PRIVATE)
							.edit().clear().apply();
					ListManager lm = new ListManager(getActivity());
					lm.delete("userData", false);
					restartApplication(getActivity());
					return true;
				}
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