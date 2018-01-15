package com.sapuseven.untis.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;

import com.sapuseven.untis.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;
import static com.sapuseven.untis.utils.StreamUtils.readStream;

// This class is to verify donators
class ActivityUserRegistration {
	private ActivityMain main;

	void submit(ActivityMain activityMain) {
		this.main = activityMain;
		new UserRegistrator().executeOnExecutor(THREAD_POOL_EXECUTOR,
				activityMain.getSharedPreferences("login_data", Context.MODE_PRIVATE).getString("user", "UNKNOWN"),
				Settings.Secure.getString(activityMain.getContentResolver(), Settings.Secure.ANDROID_ID));
	}

	private class UserRegistrator extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... strings) {
			HttpsURLConnection urlConnection = null;

			try {
				URL url = new URL("https://data.sapuseven.com/BetterUntis/api.php?method=registerUser&username=" + strings[0] + "&userid=" + strings[1]);
				urlConnection = (HttpsURLConnection) url.openConnection();

				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				return readStream(in);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String s) {
			if (s != null) {
				try {
					final JSONObject result = new JSONObject(s);
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
		}
	}
}
