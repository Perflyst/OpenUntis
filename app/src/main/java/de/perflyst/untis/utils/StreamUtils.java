package de.perflyst.untis.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

class StreamUtils {
	public static String readStream(InputStream is) {
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
}