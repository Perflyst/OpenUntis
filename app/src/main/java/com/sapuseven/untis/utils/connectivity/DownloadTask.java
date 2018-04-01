package com.sapuseven.untis.utils.connectivity;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadTask extends AsyncTask<String, Integer, String> {
	private ResponseHandler handler;

	public DownloadTask() {

	}

	@Override
	protected String doInBackground(String... parameters) {
		InputStream input = null;
		OutputStream output = null;
		HttpURLConnection connection = null;
		try {
			URL tmpUrl = new URL(parameters[0]);
			HttpURLConnection urlConnection = (HttpURLConnection) tmpUrl.openConnection();
			urlConnection.setInstanceFollowRedirects(false);
			String s = urlConnection.getHeaderField("Location");
			URL url = new URL(s);

			connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
				return "Server returned HTTP " + connection.getResponseCode()
						+ " " + connection.getResponseMessage();

			int fileLength = connection.getContentLength();

			File outputFile = new File(parameters[1]);
			input = connection.getInputStream();
			output = new FileOutputStream(outputFile);

			byte data[] = new byte[4096];
			long total = 0;
			int count;
			while ((count = input.read(data)) != -1) {
				if (isCancelled()) {
					input.close();
					return null;
				}
				total += count;
				if (fileLength > 0)
					publishProgress((int) (total * 100 / fileLength), (int) total, fileLength);
				output.write(data, 0, count);
			}
		} catch (Exception e) {
			return e.toString();
		} finally {
			try {
				if (output != null)
					output.close();
				if (input != null)
					input.close();
			} catch (IOException ignored) {
			}

			if (connection != null)
				connection.disconnect();
		}
		return "OK";
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		handler.onProgressUpdate(progress);
	}

	@Override
	protected void onPostExecute(String response) {
		handler.onFileReceived(response);
	}

	public void submit(String downloadUrl, String downloadPath) {
		this.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, downloadUrl, downloadPath);
	}

	public DownloadTask setResponseHandler(DownloadTask.ResponseHandler handler) {
		this.handler = handler;
		return this;
	}

	public interface ResponseHandler {
		void onProgressUpdate(Integer... progress);

		void onFileReceived(String response);
	}
}
