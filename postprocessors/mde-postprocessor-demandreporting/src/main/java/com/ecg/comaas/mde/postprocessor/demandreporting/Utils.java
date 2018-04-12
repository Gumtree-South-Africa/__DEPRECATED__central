package com.ecg.comaas.mde.postprocessor.demandreporting;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Strings.isNullOrEmpty;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;



public final class Utils {
    private static final String AD_ID_PREFIX = "COMA";

    private Utils() {
    }

    public static boolean isValidAdId(Long adId) {
        return adId != null && adId > 0;
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

    public static int intValueOr(@Nullable String s, int fallback) {
        if (! isNullOrEmpty(s)) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                // expected
            }
        }
        return fallback;
    }

    public static Map<String, String> parseAbTestMap(String input) {
        Map<String, String> result = new HashMap<>();
        if (hasText(input)) {
            Scanner scanner = new Scanner(input).useDelimiter(";");
            while (scanner.hasNext()) {
                parseKeyValuePair(scanner.next(), result);
            }
        }
        return result;
    }

    private static boolean hasText(String input) {
        return !(input == null || input.isEmpty());
    }

    private static void parseKeyValuePair(String input, Map<String, String> result) {
        int equalsSignPos = input.indexOf('=');
        if (equalsSignPos >= 0) {
            result.put(input.substring(0, equalsSignPos), input.substring(equalsSignPos + 1));
        } else if (StringUtils.isNotBlank(input)){
            result.put(input, "");
        }
    }
}
