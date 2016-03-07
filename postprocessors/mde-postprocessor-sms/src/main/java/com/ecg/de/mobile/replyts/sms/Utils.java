package com.ecg.de.mobile.replyts.sms;

import static org.springframework.util.StringUtils.hasText;

public final class Utils {
	private static final String AD_ID_PREFIX = "COMA";
	private Utils(){}
	
	public static boolean isValidAdId(Long adId) {
		return null != adId && adId > 0 ? true : false;
	}

	public static Long adIdStringToAdId(String adIdString) {
		if (hasText(adIdString)) {
			return toLong(stripPrefix(adIdString));
		}
		return -1L;
	}

	private static String stripPrefix(String adIdString) {
		if (adIdString.startsWith(AD_ID_PREFIX)) {
			return adIdString.replace(AD_ID_PREFIX, "");
		}
		return adIdString;
	}

	public static Long toLong(String string) {
		if (hasText(string)) {
			return Long.valueOf(string);
		}
		return -1L;
	}
}