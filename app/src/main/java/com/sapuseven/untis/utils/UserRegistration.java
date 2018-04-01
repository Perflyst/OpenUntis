package com.sapuseven.untis.utils;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;

import com.sapuseven.untis.R;
import com.sapuseven.untis.activity.ActivityMain;
import com.sapuseven.untis.utils.connectivity.ApiRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

// This class is to verify donators
public class UserRegistration {
	private ActivityMain main;

	public void submit(ActivityMain activityMain) {
		this.main = activityMain;

		SharedPreferences prefs = activityMain.getLoginData();
		String user = prefs.getString("user", "UNKNOWN");
		String school = prefs.getString("school", "UNKNOWN");

		String id = Settings.Secure.getString(activityMain.getContentResolver(), Settings.Secure.ANDROID_ID);

		ApiRequest api = new ApiRequest(main);

		Map<String, String> params = new HashMap<>();
		params.put("method", "registerUser");
		params.put("name", user);
		params.put("school", school);
		params.put("id", id);

		ApiRequest.ResponseHandler handler = response -> {
			if (response != null) {
				try {
					final JSONObject result = new JSONObject(response);
					if (result.has("result") && result.getJSONObject("result").has("message")) {
						AlertDialog.Builder builder = new AlertDialog.Builder(main)
								.setTitle(result.getJSONObject("result").optString("title", main.getString(R.string.alert)))
								.setMessage(result.getJSONObject("result").getString("message"))
								.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
									try {
										if (result.getJSONObject("result").optBoolean("exit"))
											main.finish();
										else
											dialogInterface.dismiss();
									} catch (JSONException e) {
										dialogInterface.dismiss();
										e.printStackTrace();
									}
								})
								.setCancelable(false);
						AlertDialog dialog = builder.create();
						dialog.show();

						if (result.getJSONObject("result").optInt("delay") > 0) {
							final Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
							button.setEnabled(false);

							new Handler().postDelayed(() -> button.setEnabled(true), result.getJSONObject("result").optInt("delay"));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		};

		api.setResponseHandler(handler).submit(params);
	}
}
