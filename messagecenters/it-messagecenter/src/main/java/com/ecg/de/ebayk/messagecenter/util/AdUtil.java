package com.ecg.de.ebayk.messagecenter.util;

/**
 * Created by jaludden on 30/03/16.
 */
public class AdUtil {
    public static long getAdFromMail(String adId, String adIdPrefix) {
        if (adId.startsWith(adIdPrefix)) {
            adId = adId.substring(adIdPrefix.length());
        }
        return Long.parseLong(adId);

    }
}