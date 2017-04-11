package com.ecg.messagebox.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MessageBodyExtractor {

    // reference for on printable chars https://www.juniper.net/documentation/en_US/idp5.1/topics/reference/general/intrusion-detection-prevention-custom-attack-object-extended-ascii.html
    static String START_OF_TEXT = "\u0002";
    static String END_OF_TEXT = "\u0003";
    private static Pattern pattern = Pattern.compile(START_OF_TEXT + "(.*)" + END_OF_TEXT, Pattern.DOTALL);


    static String extractBodyMarkedByNonPrintableChars(String message) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return message;
        }
    }
}
