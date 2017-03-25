package com.sapuseven.untis.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class ListManager {
	private final Context context;

	public ListManager(Context context) {
		this.context = context;
	}

	public void saveList(String name, String content, boolean useCache) {
		try {
			FileOutputStream outputStream;
			if (useCache)
				outputStream = new FileOutputStream(new File(context.getCacheDir(), name + ".json"));
			else
				outputStream = new FileOutputStream(new File(context.getFilesDir(), name + ".json"));
			outputStream.write(content.getBytes());
			outputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String readList(String name, boolean useCache) {
		StringBuilder content = new StringBuilder();
		try {
			FileInputStream inputStream;
			if (useCache)
				inputStream = new FileInputStream(new File(context.getCacheDir(), name + ".json"));
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

	@SuppressWarnings("SameParameterValue")
	public boolean exists(String name, boolean useCache) {
		File file;
		if (useCache)
			file = new File(context.getCacheDir(), name + ".json");
		else
			file = new File(context.getFilesDir(), name + ".json");
		return file.exists();
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public void delete(String name, boolean useCache) {
		if (useCache)
			new File(context.getCacheDir(), name + ".json").delete();
		else
			new File(context.getFilesDir(), name + ".json").delete();
	}
}