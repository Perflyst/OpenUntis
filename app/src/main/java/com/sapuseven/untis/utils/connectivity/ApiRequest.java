package com.sapuseven.untis.utils.connectivity;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class ApiRequest extends AsyncTask<Map<String, String>, Void, String> {
	private final WeakReference<Context> context;
	private ResponseHandler handler;

	public ApiRequest(Context context) {
		this.context = new WeakReference<>(context);
	}

	@SafeVarargs
	@Override
	protected final String doInBackground(Map<String, String>... params) {
		if (context.get() == null)
			return null;
		HttpURLConnection urlConnection = null;
		try {
			Uri.Builder builder = new Uri.Builder()
					.scheme("https")
					.authority("data.sapuseven.com")
					.path("BetterUntis/api.php");

			for (Map.Entry<String, String> param : params[0].entrySet())
				builder.appendQueryParameter(param.getKey(), param.getValue());

			Uri url = builder.build();
			urlConnection = (HttpURLConnection) new URL(url.toString()).openConnection();
			BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
			return readStream(in);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}

	@Override
	protected void onPostExecute(String response) {
		if (context.get() == null)
			return;

		handler.onResponseReceived(response);
	}

	public void submit(Map<String, String> params) {
		this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
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

	public ApiRequest setResponseHandler(ApiRequest.ResponseHandler handler) {
		this.handler = handler;
		return this;
	}

	public interface ResponseHandler {
		void onResponseReceived(String response);
	}
}
