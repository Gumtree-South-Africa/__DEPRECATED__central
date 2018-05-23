package com.ecg.comaas.mde.postprocessor.demandreporting;


import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailAddressExctractor {

    private static Pattern smtpAddressHeaderPattern = Pattern.compile("[^<]*<([^>]+)>");

    public static String extractFromSmptHeader(String header) {
        if (StringUtils.isBlank(header)) return "";

        Matcher m = smtpAddressHeaderPattern.matcher(header);
        if (m.matches()) {
            return m.group(1);
        }
        return "";
    }
}
