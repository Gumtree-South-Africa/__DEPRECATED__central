package com.ecg.de.ebayk.messagecenter.webapi.requests;

/**
 * Created by jaludden on 05/02/16.
 */
public class RequestUtil {

    public static String cleanText(String message) {
        if (message.trim().startsWith(">")) {
            message = message.trim().substring(1);
        }
        return message.replaceAll("\r?\n(\\s)*>", "\n");
    }

}
