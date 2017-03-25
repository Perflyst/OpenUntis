package com.sapuseven.untis.utils;

import android.content.Context;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class BetterToast {
	private final Context context;
	private Toast toast;

	public BetterToast(Context context) {
		this.context = context;
	}

	private void showToast(String text, int duration) {
		try {
			this.toast.getView().isShown();
			this.toast.setText(text);
		} catch (Exception e) {
			this.toast = Toast.makeText(this.context, text, duration);
		}
		this.toast.show();
	}

	public void showToast(@StringRes int resId, int duration) {
		showToast(context.getString(resId), duration);
	}
}