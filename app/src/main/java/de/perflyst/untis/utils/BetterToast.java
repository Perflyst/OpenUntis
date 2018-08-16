package de.perflyst.untis.utils;

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
			toast.getView().isShown();
			toast.setText(text);
			toast.show();
		} catch (Exception e) {
			toast = Toast.makeText(context, text, duration);
			toast.show();
		}
	}

	public void showToast(@StringRes int resId, int duration) {
		showToast(context.getString(resId), duration);
	}
}