package de.perflyst.untis.utils.connectivity;

import org.apache.commons.codec.binary.Base32;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class UntisAuthentication {
	private static long createTimeBasedCode(long timestamp, String secret) {
		GeneralSecurityException e;
		if (secret == null || secret.isEmpty()) {
			return 0;
		}
		try {
			return (long) verify_code(new Base32().decode(secret.toUpperCase().getBytes()), timestamp / 30000);
		} catch (InvalidKeyException | NoSuchAlgorithmException e2) {
			e = e2;
			throw new RuntimeException(e);
		}
	}

	private static int verify_code(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
		if (key == null || key.length == 0) {
			return 0;
		}
		byte[] data = new byte[8];
		long value = t;
		int i = 8;
		while (true) {
			int i2 = i - 1;
			if (i <= 0)
				break;
			data[i2] = (byte) ((int) value);
			value >>>= 8;
			i = i2;
		}
		//noinspection SpellCheckingInspection
		SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
		//noinspection SpellCheckingInspection
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signKey);
		byte[] hash = mac.doFinal(data);
		int offset = hash[19] & 15;
		long truncatedHash = 0;
		for (int i2 = 0; i2 < 4; i2++)
			truncatedHash = (truncatedHash << 8) | ((long) (hash[offset + i2] & 255));
		return (int) ((truncatedHash & 2147483647L) % 1000000);
	}

	public static JSONObject getAuthObject(String user, String key) throws JSONException {
		return new JSONObject()
				.put("user", user)
				.put("otp", createTimeBasedCode(System.currentTimeMillis(), key))
				.put("clientTime", System.currentTimeMillis());
	}
}
