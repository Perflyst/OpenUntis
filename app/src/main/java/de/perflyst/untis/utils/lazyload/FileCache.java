package de.perflyst.untis.utils.lazyload;

import android.content.Context;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

class FileCache {

	private final File cacheDir;

	FileCache(Context context) {
		cacheDir = new File(context.getCacheDir(), "images");
		cacheDir.mkdirs();
	}

	File getFile(String url) throws UnsupportedEncodingException {
		String filename = URLEncoder.encode(url, "UTF-8");
		return new File(cacheDir, filename);
	}

	public void clear() { // TODO: Implement some setting to clear cached images
		File[] files = cacheDir.listFiles();
		if (files == null)
			return;
		for (File f : files)
			f.delete();
	}

}