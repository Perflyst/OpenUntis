package com.sapuseven.untis.activity;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.sapuseven.untis.R;
import com.sapuseven.untis.adapter.AdapterFeatures;
import com.sapuseven.untis.utils.FeatureInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.sapuseven.untis.activity.ActivityMain.setupTheme;

/**
 * @author paul
 * @version 1.0
 * @since 2016-12-01
 */

public class ActivityFeatures extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_features);

		new loadList().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

		Button btnSuggestNew = (Button) findViewById(R.id.btnSuggestNew);
		btnSuggestNew.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final Dialog dialog = new Dialog(ActivityFeatures.this);
				dialog.setContentView(R.layout.layout_suggest_new_feature);

				final EditText etTitle = (EditText) dialog.findViewById(R.id.etTitle);
				final EditText etDesc = (EditText) dialog.findViewById(R.id.etDesc);
				Button btnSend = (Button) dialog.findViewById(R.id.btnSend);
				btnSend.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
						new suggestFeature().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, etTitle.getText().toString(), etDesc.getText().toString());
					}
				});
				dialog.show();
				Window window = dialog.getWindow();
				assert window != null;
				window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			}
		});
	}

	private String readStream(InputStream is) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			int i = is.read();
			while (i != -1) {
				bo.write(i);
				i = is.read();
			}
			return bo.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	public class loadList extends AsyncTask<Void, Void, List<FeatureInfo>> {
		ListView lvFeatures;
		ProgressBar pbLoading;

		@Override
		protected void onPreExecute() {
			lvFeatures = (ListView) findViewById(R.id.lvFeatures);
			pbLoading = (ProgressBar) findViewById(R.id.pbLoading);
			pbLoading.setVisibility(View.VISIBLE);
		}

		@Override
		protected List<FeatureInfo> doInBackground(Void... voids) {
			final List<FeatureInfo> items = new ArrayList<>();
			try {
				SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
				String user = prefs.getString("user", "");
				URL url = new URL("https://data.sapuseven.com/BetterUntis/api.php?method=getSuggestedFeatures");
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
				BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
				JSONObject list = new JSONObject(readStream(in));
				urlConnection.disconnect();
				for (int i = 0; i < list.optJSONObject("result").optJSONArray("suggestedFeatures").length(); i++) {
					final FeatureInfo featureInfo = new FeatureInfo();
					featureInfo.setTitle(list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optString("title"));
					featureInfo.setDesc(list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optString("desc"));
					int likes = 0;
					for (int j = 0; j < list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optJSONArray("votes").length(); j++) {
						likes += list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optJSONArray("votes").optJSONObject(j).optInt("vote");
						if (list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optJSONArray("votes").optJSONObject(j).optString("name").equals(user))
							featureInfo.setHasVoted(list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optJSONArray("votes").optJSONObject(j).optInt("vote"));
					}
					featureInfo.setLikes(likes);
					featureInfo.setId(list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optInt("id"));
					if (!list.optJSONObject("result").optJSONArray("suggestedFeatures").optJSONObject(i).optBoolean("disabled", false))
						items.add(featureInfo);
				}
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			}
			return items;
		}

		@Override
		protected void onPostExecute(List<FeatureInfo> featureInfos) {
			pbLoading.setVisibility(View.GONE);
			lvFeatures.setAdapter(new AdapterFeatures(ActivityFeatures.this, featureInfos));
		}
	}

	private class suggestFeature extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... values) {
			HttpURLConnection urlConnection = null;
			JSONObject list = null;
			try {
				SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
				String user = prefs.getString("user", "");
				Uri url = new Uri.Builder()
						.scheme("https")
						.authority("data.sapuseven.com")
						.path("BetterUntis/api.php")
						.appendQueryParameter("method", "addSuggestedFeature")
						.appendQueryParameter("name", user)
						.appendQueryParameter("title", values[0])
						.appendQueryParameter("desc", values[1])
						.build();
				urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
				BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
				list = new JSONObject(readStream(in));
			} catch (JSONException | IOException e) {
				e.printStackTrace();
				return false;
			} finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
			return list.optString("result").equals("OK");
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Toast.makeText(ActivityFeatures.this, R.string.toast_suggestion_submitted, Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(ActivityFeatures.this, R.string.toast_error_occurred, Toast.LENGTH_SHORT).show();
			}
		}
	}
}