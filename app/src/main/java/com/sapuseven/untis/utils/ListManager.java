package com.sapuseven.untis.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ListManager {
	private final Context context;

	private static JSONObject userData;

	public ListManager(Context context) {
		this.context = context;
	}

	public void saveList(String name, String content, boolean isCacheData) {
		Log.d("ListManager", "Writing list " + name + (isCacheData ? " (using cache)" : ""));
		try {
			FileOutputStream outputStream;
			if (isCacheData)
				outputStream = new FileOutputStream(new File(getCacheDir(), name + ".json"));
			else
				outputStream = new FileOutputStream(new File(context.getFilesDir(), name + ".json"));
			outputStream.write(content.getBytes());
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static JSONObject getUserData(ListManager listManager) {
		if (userData == null) {
			try {
				userData = new JSONObject(listManager.readList("userData", false));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			Log.d("ListManager", "Returning userData from cache");
		}

		return userData;
	}

	public boolean exists(String name, @SuppressWarnings("SameParameterValue") boolean useCaching) {
		File file;
		if (useCaching)
			file = new File(getCacheDir(), name + ".json");
		else
			file = new File(context.getFilesDir(), name + ".json");
		return file.exists();
	}

	public void delete(String name, boolean isCacheData) {
		Log.d("ListManager", "Deleting list " + name + (isCacheData ? " (using cache)" : ""));
		if (isCacheData) {
			if (!new File(getCacheDir(), name + ".json").delete()) {
				Log.e(this.getClass().getSimpleName(), "Failed to delete " + name);
			}
		} else {
			if (!new File(context.getFilesDir(), name + ".json").delete()) {
				Log.e(this.getClass().getSimpleName(), "Failed to delete " + name);
			}
		}
	}

	private File getCacheDir() {
		return context.getCacheDir();
	}

	public static JSONObject getUserData(Context context) {
		return getUserData(new ListManager(context));
	}

	public String readList(String name, boolean isCacheData) {
		Log.d("ListManager", "Reading list " + name + (isCacheData ? " (using cache)" : "") + ", origin: " + new Exception().getStackTrace()[1].getClassName());
		long timer = System.nanoTime();
		StringBuilder content = new StringBuilder();
		try {
			FileInputStream inputStream;
			if (isCacheData)
				inputStream = new FileInputStream(new File(getCacheDir(), name + ".json"));
			else
				inputStream = new FileInputStream(new File(context.getFilesDir(), name + ".json"));
			byte[] input = new byte[inputStream.available()];
			//noinspection StatementWithEmptyBody
			while (inputStream.read(input) != -1) {
			}
			content.append(new String(input));
		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.d("ListManager", "Took " + (System.nanoTime() - timer) / 1000000.0 + "ms");
		return content.toString();
	}
}