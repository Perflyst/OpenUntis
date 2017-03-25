package com.sapuseven.untis.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class AutoUpdater {
	private String versionURL;
	private HttpURLConnection urlConnection;
	private InputStream in;
	private URL url;
	private PackageInfo pInfo;
	private int appVersion;

	@SuppressWarnings("SameParameterValue")
	public void setVersionURL(String URL) {
		versionURL = URL;
	}

	public void startAutoUpdate(final Context context) {
		new Thread(new Runnable() {
			public void run() {
				// clean up old update files
				File dir = new File(context.getCacheDir() + "/update");
				String[] children = dir.list();
				if (children != null)
					for (String aChildren : children) //noinspection ResultOfMethodCallIgnored
						new File(dir, aChildren).delete();

				try {
					url = new URL(versionURL);
					urlConnection = (HttpURLConnection) url.openConnection();
					in = new BufferedInputStream(urlConnection.getInputStream());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				appVersion = pInfo.versionCode;
				JSONObject newVersion;
				try {
					newVersion = new JSONObject(readStream(in));
				} catch (JSONException e) {
					e.printStackTrace();
					return;
				}
				urlConnection.disconnect();
				if (newVersion.length() > 0 && newVersion.optJSONObject("result").optInt("versionCode") > appVersion)
					onAppVersionOutdated();
			}
		}).start();
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

	public abstract void onAppVersionOutdated();

	public void setPackageInfo(PackageManager pm, String packageName) throws PackageManager.NameNotFoundException {
		pInfo = pm.getPackageInfo(packageName, 0);
	}
}
