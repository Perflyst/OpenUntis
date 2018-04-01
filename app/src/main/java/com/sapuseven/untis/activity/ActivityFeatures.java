package com.sapuseven.untis.activity;

import android.app.Dialog;
import android.content.SharedPreferences;
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
import com.sapuseven.untis.adapter.AdapterItemFeatures;
import com.sapuseven.untis.utils.connectivity.ApiRequest;
import com.sapuseven.untis.view.SortableListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.sapuseven.untis.utils.ThemeUtils.setupTheme;

public class ActivityFeatures extends AppCompatActivity {
	private AdapterFeatures adapter;

	private final SortableListView.DropListener onDrop =
			new SortableListView.DropListener() {
				@Override
				public void drop(int from, int to) {
					showSaveButton(true);

					AdapterItemFeatures item = (AdapterItemFeatures) adapter.getItem(from);

					adapter.remove(from);
					adapter.insert(item, to);
					adapter.notifyDataSetChanged();

					while (((AdapterItemFeatures) adapter.getItem(adapter.getCount() - 1)).getLabel() != null)
						adapter.remove(adapter.getCount() - 1);
				}
			};

	private void showSaveButton(boolean show) {
		findViewById(R.id.btnSuggestNew).setVisibility(show ? View.INVISIBLE : View.VISIBLE);
		findViewById(R.id.btnSave).setVisibility(show ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setupTheme(this, true);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_features);

		SortableListView lv = findViewById(R.id.lvFeatures);
		lv.setDropListener(onDrop);

		loadList();

		findViewById(R.id.btnSuggestNew).setOnClickListener(view -> {
			final Dialog dialog = new Dialog(ActivityFeatures.this);
			dialog.setContentView(R.layout.dialog_suggest_new_feature);

			EditText etTitle = dialog.findViewById(R.id.etTitle);
			EditText etDesc = dialog.findViewById(R.id.etDesc);
			Button btnSend = dialog.findViewById(R.id.btnSend);
			btnSend.setOnClickListener(v -> {
				dialog.dismiss();
				suggestFeature(etTitle.getText().toString(), etDesc.getText().toString());
			});
			dialog.show();
			Window window = dialog.getWindow();
			assert window != null;
			window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT);
		});

		findViewById(R.id.btnSave).setOnClickListener(view -> {
			SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
			String user = prefs.getString("user", "");
			String school = prefs.getString("school", "");

			JSONArray features = new JSONArray();

			for (AdapterItemFeatures feature : adapter.getData()) {
				if (feature.getLabel() != null)
					break;

				features.put(feature.getId());
			}

			ApiRequest api = new ApiRequest(this);

			Map<String, String> params = new HashMap<>();
			params.put("method", "saveRatedFeatures");
			params.put("name", user);
			params.put("school", school);
			params.put("features", features.toString());

			ApiRequest.ResponseHandler handler = response -> {
				try {
					if (new JSONObject(response).optString("result").equals("OK")) {
						Toast.makeText(this, R.string.toast_ratings_saved,
								Toast.LENGTH_SHORT).show();

						showSaveButton(false);
					} else {
						Toast.makeText(this, R.string.toast_error_occurred,
								Toast.LENGTH_SHORT).show();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			};

			api.setResponseHandler(handler).submit(params);
		});
	}

	private void suggestFeature(String title, String desc) {
		SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
		String user = prefs.getString("user", "");

		String lang = Locale.getDefault().getLanguage();

		ApiRequest api = new ApiRequest(this);

		Map<String, String> params = new HashMap<>();
		params.put("method", "addSuggestedFeature");
		params.put("name", user);
		params.put("lang", lang);
		params.put("title", title);
		params.put("desc", desc);

		ApiRequest.ResponseHandler handler = response -> {
			try {
				if (new JSONObject(response).optString("result").equals("OK")) {
					Toast.makeText(this, R.string.toast_suggestion_submitted,
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(this, R.string.toast_error_occurred,
							Toast.LENGTH_SHORT).show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		};

		api.setResponseHandler(handler).submit(params);
	}

	private void loadList() {
		ListView lvFeatures = findViewById(R.id.lvFeatures);
		ProgressBar pbLoading = findViewById(R.id.pbLoading);
		pbLoading.setVisibility(View.VISIBLE);

		SharedPreferences prefs = getSharedPreferences("login_data", MODE_PRIVATE);
		String user = prefs.getString("user", "");
		String school = prefs.getString("school", "");

		String lang = Locale.getDefault().getLanguage();

		ApiRequest api = new ApiRequest(this);

		Map<String, String> params = new HashMap<>();
		params.put("method", "getSuggestedFeatures");
		params.put("name", user);
		params.put("school", school);
		params.put("lang", lang);

		ApiRequest.ResponseHandler handler = response -> {
			if (response == null)
				return; // TODO: Display "Network Error"

			List<AdapterItemFeatures> items = new ArrayList<>();

			try {
				JSONObject list = new JSONObject(response);
				JSONArray ratedFeatures = list.optJSONObject("result").getJSONArray("ratedFeatures");
				JSONArray newFeatures = list.optJSONObject("result").getJSONArray("newFeatures");

				for (int i = 0; i < ratedFeatures.length(); i++) {
					AdapterItemFeatures featureInfo = new AdapterItemFeatures(getApplicationContext());
					JSONObject item = ratedFeatures.getJSONObject(i);

					featureInfo.setTitle(item.getString("title"));
					featureInfo.setDesc(item.getString("desc"));
					featureInfo.setId(item.getInt("id"));
					if (!item.optBoolean("disabled"))
						items.add(featureInfo);
				}

				if (newFeatures.length() > 0) {
					AdapterItemFeatures newFeaturesLabel = new AdapterItemFeatures(getApplicationContext());
					newFeaturesLabel.setLabel("Unrated Features");
					items.add(newFeaturesLabel);

					for (int i = 0; i < newFeatures.length(); i++) {
						AdapterItemFeatures featureInfo = new AdapterItemFeatures(getApplicationContext());
						JSONObject item = newFeatures.getJSONObject(i);

						featureInfo.setTitle(item.getString("title"));
						featureInfo.setDesc(item.getString("desc"));
						featureInfo.setId(item.getInt("id"));
						if (!item.optBoolean("disabled"))
							items.add(featureInfo);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			pbLoading.setVisibility(View.GONE);
			adapter = new AdapterFeatures(ActivityFeatures.this, items);
			lvFeatures.setAdapter(adapter);
		};

		api.setResponseHandler(handler).submit(params);
	}
}