package com.sapuseven.untis.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.sapuseven.untis.R;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.sapuseven.untis.utils.StreamUtils.readStream;

public class DisplayChangelog extends AsyncTask<Integer, Void, String> {
	private final Context context;

	public DisplayChangelog(Context context) {
		this.context = context;
	}

	@Override
	protected String doInBackground(Integer... integers) {
		try {
			URL url = new URL("https://data.sapuseven.com/BetterUntis/changelog.php?format=txt&since=" + integers[0]);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			BufferedInputStream in = new BufferedInputStream(urlConnection.getInputStream());
			String content = readStream(in);
			urlConnection.disconnect();
			return content;
		} catch (IOException e) {
			e.printStackTrace();
			return "Error: " + Log.getStackTraceString(e);
		}
	}

	@Override
	protected void onPostExecute(String content) {
		new AlertDialog.Builder(context)
				.setTitle(R.string.changelog)
				.setMessage(content.replace("<br>", "\n"))
				.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						dialog.dismiss();
					}
				})
				.show();
	}
}
