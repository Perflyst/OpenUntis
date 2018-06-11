package com.sapuseven.untis.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sapuseven.untis.R;
import com.sapuseven.untis.utils.Constants;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.connectivity.UntisRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.sapuseven.untis.utils.connectivity.UntisAuthentication.getAuthObject;

public class ActivityLoginDataInput extends AppCompatActivity {
	private RelativeLayout mRlLoadingStatus;
	private ProgressBar mPbLoadingStatus;
	private ImageView mIvLoadingStatusSuccess;
	private ImageView mIvLoadingStatusFailed;
	private TextView mTvLoadingStatus;

	private AutoCompleteTextView mEtUrl;
	private EditText mEtSchool;
	private EditText mEtUser;
	private EditText mEtKey;
	private Button mBtnLogin;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_data_input);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mBtnLogin = findViewById(R.id.btnLogin);
		mEtUrl = findViewById(R.id.etUrl);
		mEtSchool = findViewById(R.id.etSchool);
		mEtUser = findViewById(R.id.etUser);
		mEtKey = findViewById(R.id.etKey);

		mBtnLogin.setOnClickListener(v -> {
			EditText error = null;
			if (mEtUser.getText().length() == 0) {
				mEtUser.setError(getString(R.string.error_field_empty));
				error = mEtUser;
			}
			if (mEtSchool.getText().length() == 0) {
				mEtSchool.setError(getString(R.string.error_field_empty));
				error = mEtSchool;
			}
			if (mEtUrl.getText().length() == 0) {
				mEtUrl.setError(getString(R.string.error_field_empty));
				error = mEtUrl;
			} else if (!Patterns.DOMAIN_NAME.matcher(mEtUrl.getText()).matches()) {
				mEtUrl.setError(getString(R.string.error_invalid_url));
				error = mEtUrl;
			}

			if (error == null)
				loadData();
			else
				error.requestFocus();
		});

		mRlLoadingStatus = findViewById(R.id.rlLoadingStatus);
		mPbLoadingStatus = findViewById(R.id.pbLoadingStatus);
		mIvLoadingStatusSuccess = findViewById(R.id.ivLoadingStatusSuccess);
		mIvLoadingStatusFailed = findViewById(R.id.ivLoadingStatusFailed);
		mTvLoadingStatus = findViewById(R.id.tvLoadingStatus);

		mRlLoadingStatus.setVisibility(View.GONE);

		String[] servers = getResources().getStringArray(R.array.webuntis_servers);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
				servers);
		mEtUrl.setAdapter(adapter);

		Uri appLinkData = getIntent().getData();

		if (appLinkData != null && appLinkData.isHierarchical()) {
			if (appLinkData.getQueryParameter("url") != null)
				mEtUrl.setText(appLinkData.getQueryParameter("url"));
			if (appLinkData.getQueryParameter("school") != null)
				mEtSchool.setText(appLinkData.getQueryParameter("school"));
			if (appLinkData.getQueryParameter("user") != null)
				mEtUser.setText(appLinkData.getQueryParameter("user"));
			if (appLinkData.getQueryParameter("key") != null)
				mEtKey.setText(appLinkData.getQueryParameter("key"));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences prefs = this.getSharedPreferences("loginDataInputBackup", MODE_PRIVATE);
		Uri uri = getIntent().getData();
		if (uri != null) {
			mEtUrl.setText(uri.getQueryParameter("url"));
			mEtSchool.setText(uri.getQueryParameter("school"));
			mEtUser.setText(uri.getQueryParameter("user"));
			mEtKey.setText(uri.getQueryParameter("key"));
		} else if (prefs != null) {
			restoreInput(prefs);
		}
		setElementsEnabled(true);
	}

	@Override
	public void onPause() {
		backupInput(this.getSharedPreferences("loginDataInputBackup", MODE_PRIVATE));

		super.onPause();
	}

	private void backupInput(SharedPreferences prefs) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("etUrl", mEtUrl.getText().toString());
		editor.putString("etSchool", mEtSchool.getText().toString());
		editor.putString("etUser", mEtUser.getText().toString());
		editor.putString("etKey", mEtKey.getText().toString());
		editor.apply();
	}

	private void restoreInput(SharedPreferences prefs) {
		mEtUrl.setText(prefs.getString("etUrl", ""));
		mEtSchool.setText(prefs.getString("etSchool", ""));
		mEtUser.setText(prefs.getString("etUser", ""));
		mEtKey.setText(prefs.getString("etKey", ""));
	}

	private void loadData() {
		mRlLoadingStatus.setVisibility(View.VISIBLE);
		mTvLoadingStatus.setText(getString(R.string.loading_data));
		mIvLoadingStatusFailed.setVisibility(View.GONE);
		mIvLoadingStatusSuccess.setVisibility(View.GONE);
		mPbLoadingStatus.setVisibility(View.VISIBLE);

		sendRequest();
	}

	private void sendRequest() {
		UntisRequest api = new UntisRequest(this);

		UntisRequest.ResponseHandler handler = response -> {
			if (response == null) {
				Log.w("ActivityLoginDataInput", "response is null");
				// TODO: Stop loading and show "unknown error: null";
				return;
			}
			try {
				if (response.has("result")) {
					mPbLoadingStatus.setVisibility(View.GONE);
					mIvLoadingStatusSuccess.setVisibility(View.VISIBLE);
					mTvLoadingStatus.setText(getString(R.string.data_loaded));
					saveCredentials(mEtUrl.getText().toString(), mEtSchool.getText().toString(),
							mEtUser.getText().toString(), mEtKey.getText().toString());
					saveData(response.getJSONObject("result"));
					finish();
				} else {
					mPbLoadingStatus.setVisibility(View.GONE);
					mIvLoadingStatusFailed.setVisibility(View.VISIBLE);

					int errorCode = Constants.UntisAPI.ERROR_CODE_UNKNOWN;
					if (response != null)
						errorCode = response.getJSONObject("error").getInt("code");

					switch (errorCode) {
						case Constants.UntisAPI.ERROR_CODE_NO_SERVER_FOUND:
							mTvLoadingStatus.setText(R.string.invalid_server_url);
							break;
						case Constants.UntisAPI.ERROR_CODE_INVALID_SCHOOLNAME:
							mTvLoadingStatus.setText(R.string.invalid_school);
							break;
						case Constants.UntisAPI.ERROR_CODE_INVALID_CLIENT_TIME:
							mTvLoadingStatus.setText(R.string.invalid_time_settings);
							break;
						case Constants.UntisAPI.ERROR_CODE_INVALID_CREDENTIALS:
							mTvLoadingStatus.setText(R.string.invalid_credentials);
							break;
						case Constants.UntisAPI.ERROR_CODE_WEBUNTIS_NOT_INSTALLED:
							mTvLoadingStatus.setText(R.string.server_webuntis_not_installed);
							break;
						case Constants.UntisAPI.ERROR_CODE_UNKNOWN:
						default:
							mTvLoadingStatus.setText(R.string.unknown_error);
							break;
					}

					setElementsEnabled(true);
				}
			} catch (JSONException e) {
				e.printStackTrace();
				mPbLoadingStatus.setVisibility(View.GONE);
				mIvLoadingStatusFailed.setVisibility(View.VISIBLE);
				mTvLoadingStatus.setText(R.string.unknown_error);
				setElementsEnabled(true);
			}
		};

		UntisRequest.UntisRequestQuery query = new UntisRequest.UntisRequestQuery();
		query.setMethod(Constants.UntisAPI.METHOD_GET_USER_DATA);
		query.setUrl(mEtUrl.getText().toString());
		query.setSchool(mEtSchool.getText().toString());

		JSONArray params = new JSONArray();
		JSONObject auth = new JSONObject();
		try {
			auth.put("auth", getAuthObject(mEtUser.getText().toString(),
					mEtKey.getText().toString()));
		} catch (JSONException e) {
			e.printStackTrace(); // TODO: Implment proper error handling (search for possible cases first)
		}
		params.put(auth);
		query.setParams(params);

		setElementsEnabled(false);
		api.setCachingMode(UntisRequest.CachingMode.LOAD_LIVE);
		api.setResponseHandler(handler).submit(query);
	}

	private void saveCredentials(String url, String school, String user, String key) {
		SharedPreferences prefs1 = getSharedPreferences("login_data", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor1 = prefs1.edit();
		editor1.putString("url", url);
		editor1.putString("school", school);
		editor1.putString("user", user);
		editor1.putString("key", key);
		editor1.apply();
	}

	@Override
	public void onBackPressed() {
		mBtnLogin.setEnabled(false);
		super.onBackPressed();
	}

	private void saveData(JSONObject data) {
		ListManager listManager = new ListManager(this);
		listManager.saveList("userData", data.toString(), false);
	}

	private void setElementsEnabled(boolean enabled) {
		mEtUrl.setEnabled(enabled);
		mEtSchool.setEnabled(enabled);
		mEtUser.setEnabled(enabled);
		mEtKey.setEnabled(enabled);
		mBtnLogin.setEnabled(enabled);
	}
}
