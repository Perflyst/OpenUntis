package com.sapuseven.untis.utils.connectivity;

import android.content.Context;
import android.os.AsyncTask;

import com.sapuseven.untis.utils.Constants;
import com.sapuseven.untis.utils.ListManager;
import com.sapuseven.untis.utils.SessionInfo;
import com.sapuseven.untis.utils.timetable.TimegridUnitManager;

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

import static com.sapuseven.untis.utils.DateOperations.addDaysToInt;
import static com.sapuseven.untis.utils.connectivity.UntisRequest.CachingMode.LOAD_LIVE;
import static com.sapuseven.untis.utils.connectivity.UntisRequest.CachingMode.LOAD_LIVE_FALLBACK_CACHE;
import static com.sapuseven.untis.utils.connectivity.UntisRequest.CachingMode.RETURN_CACHE;
import static com.sapuseven.untis.utils.connectivity.UntisRequest.CachingMode.RETURN_CACHE_LOAD_LIVE;

public class UntisRequest extends AsyncTask<UntisRequest.UntisRequestQuery, JSONObject, JSONObject> {
	private final WeakReference<Context> context;
	private ResponseHandler handler;
	private CachingMode cachingMode = LOAD_LIVE;
	private int startDateFromWeek;
	private JSONObject cacheFallback;
	private SessionInfo sessionInfo;

	public UntisRequest(Context context) {
		this.context = new WeakReference<>(context);
	}

	public UntisRequest(Context context, SessionInfo sessionInfo) {
		this.context = new WeakReference<>(context);
		this.sessionInfo = sessionInfo;
	}

	public UntisRequest(Context context, SessionInfo sessionInfo, int startDateFromWeek) {
		this.context = new WeakReference<>(context);
		this.startDateFromWeek = startDateFromWeek;
		this.sessionInfo = sessionInfo;
	}

	@Override
	protected JSONObject doInBackground(UntisRequestQuery... query) {
		boolean cacheExists = false;

		if (cachingMode != LOAD_LIVE) {
			ListManager listManager = new ListManager(context.get());

			JSONArray days = null;
			try {
				days = new JSONObject(listManager
						.readList("userData", false))
						.getJSONObject("masterData")
						.getJSONObject("timeGrid")
						.getJSONArray("days");
			} catch (JSONException e) {
				e.printStackTrace();
			}

			TimegridUnitManager unitManager = new TimegridUnitManager(days);

			String fileName = sessionInfo.getElemType() + "-"
					+ sessionInfo.getElemId() + "-"
					+ startDateFromWeek + "-"
					+ addDaysToInt(startDateFromWeek, unitManager.getNumberOfDays() - 1);

			cacheExists = listManager.exists(fileName, true);

			if (cacheExists)
				try {
					switch (cachingMode) {
						case RETURN_CACHE:
							return new JSONObject(listManager.readList(fileName, true));
						case LOAD_LIVE_FALLBACK_CACHE:
							cacheFallback = new JSONObject(listManager.readList(fileName, true));
							break;
						default:
							publishProgress(new JSONObject(listManager.readList(fileName, true)));
							break;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					if (cachingMode == RETURN_CACHE)
						return null;
				}
		}

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
				if (cachingMode == LOAD_LIVE)
					return new JSONObject()
							.put("id", 0)
							.put("error", new JSONObject()
									.put("code", Constants.UntisAPI.ERROR_CODE_UNKNOWN)
									.put("message", "Unexpected status code: " + statusCode)
							);
				else if (cachingMode == LOAD_LIVE_FALLBACK_CACHE && cacheFallback != null)
					return cacheFallback;
				return null;
			}

			InputStream inputStream = connection.getInputStream();

			if (inputStream != null) {
				if (cachingMode != RETURN_CACHE_LOAD_LIVE
						|| (cachingMode == RETURN_CACHE_LOAD_LIVE && !cacheExists))
					return new JSONObject(readStream(inputStream))
							.put("timeModified", System.currentTimeMillis());
			} else {
				if (cachingMode == LOAD_LIVE_FALLBACK_CACHE && cacheFallback != null)
					return cacheFallback;
				else
					return null;
			}
		} catch (Exception e) {
			try {
				if (cachingMode == LOAD_LIVE)
					return new JSONObject()
							.put("id", 0)
							.put("error", new JSONObject()
									.put("code", e instanceof UnknownHostException ?
											Constants.UntisAPI.ERROR_CODE_NO_SERVER_FOUND :
											Constants.UntisAPI.ERROR_CODE_UNKNOWN)
									.put("message", e.getMessage())
							);
				else if (cachingMode == LOAD_LIVE_FALLBACK_CACHE && cacheFallback != null)
					return cacheFallback;
				else
					return null;
			} catch (JSONException e1) {
				e1.printStackTrace();
				return null;
			}
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(JSONObject... responses) {
		if (context.get() == null)
			return;

		if (responses.length > 0)
			for (JSONObject response : responses)
				handler.onResponseReceived(response);
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
		LOAD_LIVE,
		LOAD_LIVE_FALLBACK_CACHE
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

		String getJsonrpc() {
			return jsonrpc;
		}

		String getMethod() {
			return method;
		}

		public void setMethod(String method) {
			this.method = method;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		String getSchool() {
			return school;
		}

		public void setSchool(String school) {
			this.school = school;
		}

		JSONArray getParams() {
			return params;
		}

		public void setParams(JSONArray params) {
			this.params = params;
		}

		URI getURI() throws URISyntaxException {
			return new URI("https", url, "/WebUntis/jsonrpc_intern.do", "school=" + getSchool(), "");
		}
	}
}
