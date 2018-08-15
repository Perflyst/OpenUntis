package com.perflyst.untis.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;

import com.perflyst.untis.R;
import com.perflyst.untis.activity.ActivityMain;

public class ThemeUtils {
// --Commented out by Inspection START (24-Mar-18 9:16):
//	@SuppressWarnings("SameParameterValue")
//	public static void tintDrawable(Context context, Drawable drawable, @AttrRes int attr) {
//		int[] attrs = new int[]{attr};
//		TypedArray ta = context.obtainStyledAttributes(attrs);
//		final int color = ta.getColor(0, 0);
//		ta.recycle();
//		DrawableCompat.setTint(drawable, color);
//	}
// --Commented out by Inspection STOP (24-Mar-18 9:16)

	public static void setupTheme(Context context, boolean actionBar) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (actionBar)
			switch (prefs.getString("preference_theme", "default")) {
				case "untis":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeUntis_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ThemeUntis_ActionBar);
					break;
				case "blue":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeBlue_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ThemeBlue_ActionBar);
					break;
				case "green":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeGreen_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ThemeGreen_ActionBar);
					break;
				case "pink":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemePink_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ThemePink_ActionBar);
					break;
				case "cyan":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeCyan_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ThemeCyan_ActionBar);
					break;
				default:
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ActionBar);
					else
						context.setTheme(R.style.AppTheme_ActionBar);
			}
		else
			switch (prefs.getString("preference_theme", "default")) {
				case "untis":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeUntis);
					else
						context.setTheme(R.style.AppTheme_ThemeUntis);
					break;
				case "blue":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeBlue);
					else
						context.setTheme(R.style.AppTheme_ThemeBlue);
					break;
				case "green":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeGreen);
					else
						context.setTheme(R.style.AppTheme_ThemeGreen);
					break;
				case "pink":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemePink);
					else
						context.setTheme(R.style.AppTheme_ThemePink);
					break;
				case "cyan":
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark_ThemeCyan);
					else
						context.setTheme(R.style.AppTheme_ThemeCyan);
					break;
				default:
					if (prefs.getBoolean("preference_dark_theme", false))
						context.setTheme(R.style.AppThemeDark);
					else
						context.setTheme(R.style.AppTheme);
			}
	}

	public static void setupBackground(Activity context) {
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

		if (sharedPrefs.getBoolean("preference_dark_theme_amoled", false) && sharedPrefs.getBoolean("preference_dark_theme", false))
			context.getWindow().getDecorView().setBackgroundColor(Color.BLACK);
	}

	public static void restartApplication(Context context) {
		Intent i = new Intent(context, ActivityMain.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(i);
	}
}