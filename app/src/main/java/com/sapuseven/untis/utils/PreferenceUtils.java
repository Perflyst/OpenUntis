package com.sapuseven.untis.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class PreferenceUtils {
	public static int getPrefInt(Context context, SharedPreferences prefs, String key, boolean convertString) {
		Resources res = context.getResources();
		if (convertString)
			return Integer.parseInt(prefs.getString(key, String.valueOf(res.getInteger(res.getIdentifier(key + "_default", "integer", context.getPackageName())))));
		else
			return prefs.getInt(key, res.getInteger(res.getIdentifier(key + "_default", "integer", context.getPackageName())));
	}

	public static int getPrefInt(Context context, SharedPreferences prefs, String key) {
		return getPrefInt(context, prefs, key, false);
	}

	public static boolean getPrefBool(Context context, SharedPreferences prefs, String key) {
		Resources res = context.getResources();
		return prefs.getBoolean(key, res.getBoolean(res.getIdentifier(key + "_default", "bool", context.getPackageName())));
	}
}
