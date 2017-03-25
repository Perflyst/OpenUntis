package com.sapuseven.untis.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
	private RelativeLayout rlConnectionStatus;
	private ProgressBar pbConnectionStatus;
	private ImageView ivConnectionStatusSuccess;
	private ImageView ivConnectionStatusFailed;
	private TextView tvConnectionStatus;

	private RelativeLayout rlLoadingStatus;
	private ProgressBar pbLoadingStatus;
	private ImageView ivLoadingStatusSuccess;
	private ImageView ivLoadingStatusFailed;
	private TextView tvLoadingStatus;

	private AutoCompleteTextView etUrl;
	private EditText etSchool;
	private EditText etUser;
	private EditText etKey;
	private Button btnLogin;

	private makeRequest request;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_data_input);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		btnLogin = (Button) findViewById(R.id.btnLogin);
		etUrl = (AutoCompleteTextView) findViewById(R.id.etUrl);
		etSchool = (EditText) findViewById(R.id.etSchool);
		etUser = (EditText) findViewById(R.id.etUser);
		etKey = (EditText) findViewById(R.id.etKey);

		btnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean error = false;
				if (etUrl.getText().length() == 0) {
					etUrl.setError(getString(R.string.error_field_empty));
					error = true;
				}
				if (etSchool.getText().length() == 0) {
					etSchool.setError(getString(R.string.error_field_empty));
					error = true;
				}
				if (etUser.getText().length() == 0) {
					etUser.setError(getString(R.string.error_field_empty));
					error = true;
				}
				if (!error) {
					loadData();
				}
			}
		});

		rlConnectionStatus = (RelativeLayout) findViewById(R.id.rlConnectionStatus);
		pbConnectionStatus = (ProgressBar) findViewById(R.id.pbConnectionStatus);
		ivConnectionStatusSuccess = (ImageView) findViewById(R.id.ivConnectionStatusSuccess);
		ivConnectionStatusFailed = (ImageView) findViewById(R.id.ivConnectionStatusFailed);
		tvConnectionStatus = (TextView) findViewById(R.id.tvConnectionStatus);

		rlLoadingStatus = (RelativeLayout) findViewById(R.id.rlLoadingStatus);
		pbLoadingStatus = (ProgressBar) findViewById(R.id.pbLoadingStatus);
		ivLoadingStatusSuccess = (ImageView) findViewById(R.id.ivLoadingStatusSuccess);
		ivLoadingStatusFailed = (ImageView) findViewById(R.id.ivLoadingStatusFailed);
		tvLoadingStatus = (TextView) findViewById(R.id.tvLoadingStatus);

		rlConnectionStatus.setVisibility(View.GONE);
		rlLoadingStatus.setVisibility(View.GONE);

		String[] servers = getResources().getStringArray(R.array.webuntis_servers);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, servers);
		etUrl.setAdapter(adapter);

		Uri appLinkData = getIntent().getData();

		if (appLinkData != null) {
			if (appLinkData.getQueryParameter("url") != null)
				etUrl.setText(appLinkData.getQueryParameter("url"));
			if (appLinkData.getQueryParameter("school") != null)
				etSchool.setText(appLinkData.getQueryParameter("school"));
			if (appLinkData.getQueryParameter("user") != null)
				etUser.setText(appLinkData.getQueryParameter("user"));
			if (appLinkData.getQueryParameter("key") != null)
				etKey.setText(appLinkData.getQueryParameter("key"));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences prefs = this.getSharedPreferences("loginDataInputBackup", MODE_PRIVATE);
		Uri uri = getIntent().getData();
		if (uri != null) {
			etUrl.setText(uri.getQueryParameter("url"));
			etSchool.setText(uri.getQueryParameter("school"));
			etUser.setText(uri.getQueryParameter("user"));
			etKey.setText(uri.getQueryParameter("key"));
		} else if (prefs != null) {
			etUrl.setText(prefs.getString("etUrl", ""));
			etSchool.setText(prefs.getString("etSchool", ""));
			etUser.setText(prefs.getString("etUser", ""));
			etKey.setText(prefs.getString("etKey", ""));
		}
		setElementsEnabled(true);
	}

	@Override
	public void onPause() {
		if (request != null && request.getStatus().equals(AsyncTask.Status.RUNNING)) {
			request.cancel(true);
		}

		SharedPreferences prefs = this.getSharedPreferences("loginDataInputBackup", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("etUrl", etUrl.getText().toString());
		editor.putString("etSchool", etSchool.getText().toString());
		editor.putString("etUser", etUser.getText().toString());
		editor.putString("etKey", etKey.getText().toString());
		editor.apply();

		super.onPause();
	}

	private void loadData() {
		rlLoadingStatus.setVisibility(View.GONE);
		rlConnectionStatus.setVisibility(View.VISIBLE);
		tvConnectionStatus.setText(getString(R.string.connecting));
		ivConnectionStatusFailed.setVisibility(View.GONE);
		ivConnectionStatusSuccess.setVisibility(View.GONE);
		pbConnectionStatus.setVisibility(View.VISIBLE);
		request = new makeRequest(etUrl.getText().toString() + "/WebUntis/jsonrpc_intern.do?school=" + etSchool.getText().toString());
		request.setId(REQUEST_ID_CONNECT);
		request.execute();
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
		btnLogin.setEnabled(false);
		super.onBackPressed();
	}

	private void saveData(JSONObject data) {
		ListManager listManager = new ListManager(this);
		listManager.saveList("userData", data.toString(), false);
	}

	private void setElementsEnabled(boolean enabled) {
		etUrl.setEnabled(enabled);
		etSchool.setEnabled(enabled);
		etUser.setEnabled(enabled);
		etKey.setEnabled(enabled);
		btnLogin.setEnabled(enabled);
	}

	private class makeRequest extends AsyncTask<Void, Void, String> {
		private static final String jsonrpc = "2.0";
		private final String url;
		private final String method = "getUserData2017";
		private String id = "-1";
		private String params = "[]";
		private String json;

		makeRequest(String url) {
			if (url.contains("://"))
				this.url = url;
			else
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
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":" + ERROR_CODE_NO_SERVER_FOUND + "," +
							"\"message\":" + "\"" + e.getMessage().replace("\"", "\\\"") + "\"}}";
				}

				if (httpResponse.getStatusLine().getStatusCode() != 200) {
					Log.e("BetterUntis", "Server responded with code " + httpResponse.getStatusLine().getStatusCode());
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":" + ERROR_CODE_WEBUNTIS_NOT_INSTALLED + ",\"message\":" +
							"\"WebUntis is not installed on the specified server!\"}}";
				}

				try {
					inputStream = httpResponse.getEntity().getContent();

					result = inputStreamToString(inputStream);
				} catch (IOException e) {
					e.printStackTrace();
					return "{\"id\":\"" + id + "\",\"error\":{\"code\":" + ERROR_CODE_WEBUNTIS_NOT_INSTALLED + ",\"message\":" +
							"\"WebUntis is not installed on the specified server!\"}}";
				}
			} catch (JSONException | UnsupportedEncodingException e) {
				e.printStackTrace();
				result = "{\"id\":\"" + id + "\",\"error\":{\"code\":" + ERROR_CODE_UNKNOWN + ",\"message\":" +
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
						data.optJSONObject("error").optInt("code") != ERROR_CODE_INVALID_SCHOOLNAME) {
					pbConnectionStatus.setVisibility(View.GONE);
					ivConnectionStatusSuccess.setVisibility(View.VISIBLE);
					tvConnectionStatus.setText(getString(R.string.connected));
					rlLoadingStatus.setVisibility(View.VISIBLE);
					tvLoadingStatus.setText(getString(R.string.loading_data));
					ivLoadingStatusFailed.setVisibility(View.GONE);
					ivLoadingStatusSuccess.setVisibility(View.GONE);
					pbLoadingStatus.setVisibility(View.VISIBLE);
					request = new makeRequest(etUrl.getText().toString() + PATH + "?school=" + etSchool.getText().toString());
					request.setId(REQUEST_ID_LOAD);
					request.setParams("[{" + getAuthElement(etUser.getText().toString(), etKey.getText().toString()) + "}]");
					request.execute();
				}
				if (!data.has("error")) {
					pbLoadingStatus.setVisibility(View.GONE);
					ivLoadingStatusSuccess.setVisibility(View.VISIBLE);
					tvLoadingStatus.setText(getString(R.string.data_loaded));
					saveCredentials(etUrl.getText().toString(), etSchool.getText().toString(), etUser.getText().toString(), etKey.getText().toString());
					saveData(data.optJSONObject("result"));
					finish();
				} else {
					if (id.equals(REQUEST_ID_CONNECT) &&
							(data.optJSONObject("error").optInt("code") == ERROR_CODE_NO_SERVER_FOUND ||
									data.optJSONObject("error").optInt("code") == ERROR_CODE_INVALID_SCHOOLNAME)) {
						pbConnectionStatus.setVisibility(View.GONE);
						ivConnectionStatusFailed.setVisibility(View.VISIBLE);

						switch (data.optJSONObject("error").optInt("code")) {
							case ERROR_CODE_NO_SERVER_FOUND:
								tvConnectionStatus.setText(R.string.invalid_server_url);
								break;
							case ERROR_CODE_INVALID_SCHOOLNAME:
								tvConnectionStatus.setText(R.string.invalid_school);
								break;
							default:
								tvConnectionStatus.setText(R.string.unknown_error);
								break;
						}

						setElementsEnabled(true);
					} else if (id.equals(REQUEST_ID_LOAD)) {
						pbLoadingStatus.setVisibility(View.GONE);
						ivLoadingStatusFailed.setVisibility(View.VISIBLE);

						switch (data.optJSONObject("error").optInt("code")) {
							case ERROR_CODE_INVALID_CLIENT_TIME:
								tvLoadingStatus.setText(R.string.invalid_time_settings);
								break;
							case ERROR_CODE_INVALID_CREDENTIALS:
								tvLoadingStatus.setText(R.string.invalid_credentials);
								break;
							case ERROR_CODE_WEBUNTIS_NOT_INSTALLED:
								tvLoadingStatus.setText(R.string.server_webuntis_not_installed);
								break;
							case ERROR_CODE_UNKNOWN:
							default:
								tvLoadingStatus.setText(R.string.unknown_error);
								break;
						}

						setElementsEnabled(true);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				pbConnectionStatus.setVisibility(View.GONE);
				ivConnectionStatusFailed.setVisibility(View.VISIBLE);
				tvConnectionStatus.setText(R.string.unknown_error);
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
