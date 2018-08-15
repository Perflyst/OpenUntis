package com.perflyst.untis.utils;

import android.content.Context;

public class Conversions {
	private static float scale = -1;

	public Conversions() {
		throw new RuntimeException("Instantiation not allowed");
	}

	public static void setScale(Context context) {
		scale = context.getResources().getDisplayMetrics().density;
	}

	public static int dp2px(int dp) {
		if (scale < 0)
			throw new IllegalStateException("You have to call setScale first!");
		return (int) (dp * scale + 0.5f);
	}
}
