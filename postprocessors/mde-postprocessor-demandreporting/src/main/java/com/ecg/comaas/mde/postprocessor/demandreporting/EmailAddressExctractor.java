package com.ecg.comaas.mde.postprocessor.demandreporting;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.common.Strings;

public class EmailAddressExctractor {

    private static Pattern smtpAddressHeaderPattern = Pattern.compile("[^<]*<([^>]+)>");

    public static String extractFromSmptHeader(String header) {
        if (!Strings.hasText(header)) return "";

        Matcher m = smtpAddressHeaderPattern.matcher(header);
        if (m.matches()) {
            return m.group(1);
        }
        return "";
    }
}
