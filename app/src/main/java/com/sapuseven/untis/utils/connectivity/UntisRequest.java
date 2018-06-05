package com.sapuseven.untis.utils.connectivity;

import android.content.Context;
import android.os.AsyncTask;

import com.sapuseven.untis.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

public class UntisRequest extends AsyncTask<UntisRequest.UntisRequestQuery, Void, JSONObject> {
	private WeakReference<Context> context;
	private ResponseHandler handler;
	private CachingMode cachingMode;

	public UntisRequest() {

	}

	public UntisRequest(Context context) {
		this.context = new WeakReference<>(context);
	}

	@Override
	protected JSONObject doInBackground(UntisRequestQuery... query) {
		try {
			URL url = new URL(query[0].getURI().toString());

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();

			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			connection.setDoOutput(true);
			connection.connect();

			OutputStream out = connection.getOutputStream();

			out.write(new JSONObject()
					.put("id", 0)
					.put("method", query[0].getMethod())
					.put("params", query[0].getParams())
					.put("jsonrpc", query[0].getJsonrpc())
					.toString().getBytes());
			out.flush();
			out.close();

			int statusCode = connection.getResponseCode();
			if (statusCode != HttpURLConnection.HTTP_OK) {
				return new JSONObject()
						.put("id", 0)
						.put("error", new JSONObject()
								.put("code", Constants.UntisAPI.ERROR_CODE_UNKNOWN)
								.put("message", "Unexpected status code: " + statusCode)
						);
			}

			InputStream inputStream = connection.getInputStream();

			if (inputStream != null) {
				return new JSONObject(readStream(inputStream))
						.put("timeModified", System.currentTimeMillis());
			} else {
				return new JSONObject();
			}
		} catch (Exception e) {
			try {
				return new JSONObject()
						.put("id", 0)
						.put("error", new JSONObject()
								.put("code", e instanceof UnknownHostException ?
										Constants.UntisAPI.ERROR_CODE_NO_SERVER_FOUND :
										Constants.UntisAPI.ERROR_CODE_UNKNOWN)
								.put("message", e.getMessage())
						);
			} catch (JSONException e1) {
				e1.printStackTrace();
				return null;
			}
		}
	}

	@Override
	protected void onPostExecute(JSONObject response) {
		if (context.get() == null)
			return;

		handler.onResponseReceived(response);
	}

	public void submit(UntisRequestQuery query) {
		this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
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

	public UntisRequest setResponseHandler(UntisRequest.ResponseHandler handler) {
		this.handler = handler;
		return this;
	}

	public void setCachingMode(CachingMode cachingMode) {
		this.cachingMode = cachingMode;
	}

	public enum CachingMode {
		RETURN_CACHE,
		RETURN_CACHE_LOAD_LIVE,
		RETURN_CACHE_LOAD_LIVE_RETURN_LIVE,
		LOAD_LIVE
	}

	public interface ResponseHandler {
		void onResponseReceived(JSONObject response);
	}

	public static class UntisRequestQuery {
		private String jsonrpc = "2.0";
		private String method = "";
		private String url = "";
		private String school = "";
		private JSONArray params = new JSONArray();

		public String getJsonrpc() {
			return jsonrpc;
		}

		public String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getSchool() {
			return school;
		}

		public void setSchool(String school) {
			this.school = school;
		}

		public JSONArray getParams() {
			return params;
		}

		public void setParams(JSONArray params) {
			this.params = params;
		}

		public URI getURI() throws URISyntaxException {
			return new URI("https", url, "/WebUntis/jsonrpc_intern.do", "school=" + getSchool(), "");
		}
	}
}
