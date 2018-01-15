package com.sapuseven.untis.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.sapuseven.untis.utils.ListManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static com.sapuseven.untis.utils.Authentication.getAuthElement;
import static com.sapuseven.untis.utils.Constants.API.DEFAULT_PROTOCOL;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_INVALID_CLIENT_TIME;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_INVALID_CREDENTIALS;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_INVALID_SCHOOLNAME;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_NO_SERVER_FOUND;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_UNKNOWN;
import static com.sapuseven.untis.utils.Constants.API.ERROR_CODE_WEBUNTIS_NOT_INSTALLED;
import static com.sapuseven.untis.utils.Constants.API.PATH;
import static com.sapuseven.untis.utils.Constants.LoginDataInput.REQUEST_ID_CONNECT;
import static com.sapuseven.untis.utils.Constants.LoginDataInput.REQUEST_ID_LOAD;

public class ActivityLoginDataInput extends AppCompatActivity {
	private RelativeLayout mRlConnectionStatus;
	private ProgressBar mPbConnectionStatus;
	private ImageView mIvConnectionStatusSuccess;
	private ImageView mIvConnectionStatusFailed;
	private TextView mTvConnectionStatus;

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

	private LoginRequest mRequest;

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

		mRlConnectionStatus = findViewById(R.id.rlConnectionStatus);
		mPbConnectionStatus = findViewById(R.id.pbConnectionStatus);
		mIvConnectionStatusSuccess = findViewById(R.id.ivConnectionStatusSuccess);
		mIvConnectionStatusFailed = findViewById(R.id.ivConnectionStatusFailed);
		mTvConnectionStatus = findViewById(R.id.tvConnectionStatus);

		mRlLoadingStatus = findViewById(R.id.rlLoadingStatus);
		mPbLoadingStatus = findViewById(R.id.pbLoadingStatus);
		mIvLoadingStatusSuccess = findViewById(R.id.ivLoadingStatusSuccess);
		mIvLoadingStatusFailed = findViewById(R.id.ivLoadingStatusFailed);
		mTvLoadingStatus = findViewById(R.id.tvLoadingStatus);

		mRlConnectionStatus.setVisibility(View.GONE);
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
			mEtUrl.setText(prefs.getString("etUrl", ""));
			mEtSchool.setText(prefs.getString("etSchool", ""));
			mEtUser.setText(prefs.getString("etUser", ""));
			mEtKey.setText(prefs.getString("etKey", ""));
		}
		setElementsEnabled(true);
	}

	@Override
	public void onPause() {
		if (mRequest != null && mRequest.getStatus().equals(AsyncTask.Status.RUNNING)) {
			mRequest.cancel(true);
		}

		SharedPreferences prefs = this.getSharedPreferences("loginDataInputBackup", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("etUrl", mEtUrl.getText().toString());
		editor.putString("etSchool", mEtSchool.getText().toString());
		editor.putString("etUser", mEtUser.getText().toString());
		editor.putString("etKey", mEtKey.getText().toString());
		editor.apply();

		super.onPause();
	}

	private void loadData() {
		mRlLoadingStatus.setVisibility(View.GONE);
		mRlConnectionStatus.setVisibility(View.VISIBLE);
		mTvConnectionStatus.setText(getString(R.string.connecting));
		mIvConnectionStatusFailed.setVisibility(View.GONE);
		mIvConnectionStatusSuccess.setVisibility(View.GONE);
		mPbConnectionStatus.setVisibility(View.VISIBLE);
		mRequest = new LoginRequest(mEtUrl.getText().toString() + "/WebUntis/jsonrpc_intern.do" +
				"?school=" + mEtSchool.getText().toString());
		mRequest.setId(REQUEST_ID_CONNECT);
		mRequest.execute();
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

	private class LoginRequest extends AsyncTask<Void, Void, String> {
		private static final String jsonrpc = "2.0";
		private final String url;
		private final String method = "getUserData2017";
		private String id = "-1";
		private String params = "[]";
		private String json;

		LoginRequest(String url) {
			this.url = DEFAULT_PROTOCOL + url;
		}

		@Override
		protected void onPreExecute() {
			setElementsEnabled(false);
		}

		@SuppressWarnings("deprecation")
		@Override
		protected String doInBackground(Void... p1) {
			InputStream inputStream;
			String result;
			try {
				HttpClient httpclient = new DefaultHttpClient();

				HttpPost httpPost = new HttpPost(url);

				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", id);
				jsonObject.put("method", method);
				jsonObject.put("params", new JSONArray(params));
				jsonObject.put("jsonrpc", jsonrpc);

				json = jsonObject.toString();

				StringEntity se = new StringEntity(json);

				httpPost.setEntity(se);

				httpPost.setHeader("Accept", "application/json");
				httpPost.setHeader("Content-type", "application/json");

				HttpResponse httpResponse;
				try {
					httpResponse = httpclient.execute(httpPost);
				} catch (IOException e) {
					e.printStackTrace();
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":"
							+ ERROR_CODE_NO_SERVER_FOUND + "," +
							"\"message\":" + "\"" + e.getMessage().replace("\"", "\\\"") + "\"}}";
				}

				if (httpResponse.getStatusLine().getStatusCode() != 200) {
					Log.e("BetterUntis", "Server responded with code "
							+ httpResponse.getStatusLine().getStatusCode());
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":"
							+ ERROR_CODE_WEBUNTIS_NOT_INSTALLED + ",\"message\":" +
							"\"WebUntis is not installed on the specified server!\"}}";
				}

				try {
					inputStream = httpResponse.getEntity().getContent();

					result = inputStreamToString(inputStream);
				} catch (IOException e) {
					e.printStackTrace();
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":"
							+ ERROR_CODE_WEBUNTIS_NOT_INSTALLED + ",\"message\":" +
							"\"WebUntis is not installed on the specified server!\"}}";
				}
			} catch (JSONException | UnsupportedEncodingException e) {
				e.printStackTrace();
				result = "{\"id\":\"" + id + "\",\"error\":{\"code\":"
						+ ERROR_CODE_UNKNOWN + ",\"message\":" +
						"\"An unknown error occurred.\"}}";
			}

			return result;
		}

		@Override
		protected void onPostExecute(String result) {
			try {
				JSONObject data = new JSONObject(result);

				if (id.equals(REQUEST_ID_CONNECT) &&
						data.optJSONObject("error").optInt("code") != ERROR_CODE_NO_SERVER_FOUND &&
						data.optJSONObject("error").optInt("code")
								!= ERROR_CODE_INVALID_SCHOOLNAME) {
					mPbConnectionStatus.setVisibility(View.GONE);
					mIvConnectionStatusSuccess.setVisibility(View.VISIBLE);
					mTvConnectionStatus.setText(getString(R.string.connected));
					mRlLoadingStatus.setVisibility(View.VISIBLE);
					mTvLoadingStatus.setText(getString(R.string.loading_data));
					mIvLoadingStatusFailed.setVisibility(View.GONE);
					mIvLoadingStatusSuccess.setVisibility(View.GONE);
					mPbLoadingStatus.setVisibility(View.VISIBLE);
					mRequest = new LoginRequest(mEtUrl.getText().toString() + PATH
							+ "?school=" + mEtSchool.getText().toString());
					mRequest.setId(REQUEST_ID_LOAD);
					mRequest.setParams("[{" + getAuthElement(mEtUser.getText().toString(),
							mEtKey.getText().toString()) + "}]");
					mRequest.execute();
				}
				if (!data.has("error")) {
					mPbLoadingStatus.setVisibility(View.GONE);
					mIvLoadingStatusSuccess.setVisibility(View.VISIBLE);
					mTvLoadingStatus.setText(getString(R.string.data_loaded));
					saveCredentials(mEtUrl.getText().toString(), mEtSchool.getText().toString(),
							mEtUser.getText().toString(), mEtKey.getText().toString());
					saveData(data.optJSONObject("result"));
					finish();
				} else {
					if (id.equals(REQUEST_ID_CONNECT) &&
							(data.optJSONObject("error").optInt("code")
									== ERROR_CODE_NO_SERVER_FOUND ||
									data.optJSONObject("error").optInt("code")
											== ERROR_CODE_INVALID_SCHOOLNAME)) {
						mPbConnectionStatus.setVisibility(View.GONE);
						mIvConnectionStatusFailed.setVisibility(View.VISIBLE);

						switch (data.optJSONObject("error").optInt("code")) {
							case ERROR_CODE_NO_SERVER_FOUND:
								mTvConnectionStatus.setText(R.string.invalid_server_url);
								break;
							case ERROR_CODE_INVALID_SCHOOLNAME:
								mTvConnectionStatus.setText(R.string.invalid_school);
								break;
							default:
								mTvConnectionStatus.setText(R.string.unknown_error);
								break;
						}

						setElementsEnabled(true);
					} else if (id.equals(REQUEST_ID_LOAD)) {
						mPbLoadingStatus.setVisibility(View.GONE);
						mIvLoadingStatusFailed.setVisibility(View.VISIBLE);

						switch (data.optJSONObject("error").optInt("code")) {
							case ERROR_CODE_INVALID_CLIENT_TIME:
								mTvLoadingStatus.setText(R.string.invalid_time_settings);
								break;
							case ERROR_CODE_INVALID_CREDENTIALS:
								mTvLoadingStatus.setText(R.string.invalid_credentials);
								break;
							case ERROR_CODE_WEBUNTIS_NOT_INSTALLED:
								mTvLoadingStatus.setText(R.string.server_webuntis_not_installed);
								break;
							case ERROR_CODE_UNKNOWN:
							default:
								mTvLoadingStatus.setText(R.string.unknown_error);
								break;
						}

						setElementsEnabled(true);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				mPbConnectionStatus.setVisibility(View.GONE);
				mIvConnectionStatusFailed.setVisibility(View.VISIBLE);
				mTvConnectionStatus.setText(R.string.unknown_error);
				setElementsEnabled(true);
			}
		}

		private String inputStreamToString(InputStream inputStream) throws IOException {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
			String result = bufferedReader.readLine();
			inputStream.close();
			return result;
		}

		public void setId(String id) {
			this.id = id;
		}

		void setParams(String params) {
			this.params = params;
		}
	}
}
