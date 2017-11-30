package com.sapuseven.untis.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ListManager {
	private final Context context;

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

	public String readList(String name, boolean isCacheData) {
		Log.d("ListManager", "Reading list " + name + (isCacheData ? " (using cache)" : ""));
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
		return content.toString();
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
}