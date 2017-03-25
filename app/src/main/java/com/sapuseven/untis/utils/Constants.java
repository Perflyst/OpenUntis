package com.sapuseven.untis.utils;

/**
 * @author paul
 * @version 1.0
 * @since 2017-02-19
 */

@SuppressWarnings("unused")
public class Constants {
	public class API {
		public static final String DEFAULT_PROTOCOL = "https://";
		public static final String PATH = "/WebUntis/jsonrpc_intern.do";

		public static final int ERROR_CODE_INVALID_SCHOOLNAME = -8500;
		public static final int ERROR_CODE_INVALID_CREDENTIALS = -8504;
		public static final int ERROR_CODE_INVALID_CLIENT_TIME = -8524;
		public static final int ERROR_CODE_NO_SERVER_FOUND = 100;
		public static final int ERROR_CODE_WEBUNTIS_NOT_INSTALLED = 101;

		public static final int ERROR_CODE_UNKNOWN = 0;
	}

	public class LoginDataInput {
		public static final String REQUEST_ID_CONNECT = "1";
		public static final String REQUEST_ID_LOAD = "2";
	}

	public class TimetableItem {
		public static final String CODE_REGULAR = "REGULAR";
		public static final String CODE_CANCELLED = "CANCELLED";
		public static final String CODE_IRREGULAR = "IRREGULAR";
		public static final String CODE_EXAM = "EXAM";
	}
}