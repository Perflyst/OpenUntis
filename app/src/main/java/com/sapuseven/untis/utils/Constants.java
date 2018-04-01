package com.sapuseven.untis.utils;

@SuppressWarnings("unused")
public class Constants {
	public class UntisAPI {
		public static final String DEFAULT_PROTOCOL = "https://";
		public static final String DEFAULT_WEBUNTIS_PATH = "/WebUntis/jsonrpc_intern.do";

		public static final int ERROR_CODE_INVALID_SCHOOLNAME = -8500;
		public static final int ERROR_CODE_INVALID_CREDENTIALS = -8504;
		public static final int ERROR_CODE_INVALID_CLIENT_TIME = -8524;
		public static final int ERROR_CODE_NO_SERVER_FOUND = 100;
		public static final int ERROR_CODE_WEBUNTIS_NOT_INSTALLED = 101;

		public static final int ERROR_CODE_UNKNOWN = 0;

		public static final String METHOD_GET_USER_DATA = "getUserData2017";
		public static final String METHOD_GET_TIMETABLE = "getTimetable2017";
	}

	public class TimetableItem {
		public static final String CODE_REGULAR = "REGULAR";
		public static final String CODE_CANCELLED = "CANCELLED";
		public static final String CODE_IRREGULAR = "IRREGULAR";
		public static final String CODE_EXAM = "EXAM";
	}
}