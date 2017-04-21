package com.ecg.de.ebayk.messagecenter.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mdarapour@ebay.com
 */
public class MessageContentHelper {
    static final Pattern XML_PATTERN = Pattern.compile("<(\\S+?)(.*?)>(.*?)</\\1>");
    static final Pattern SENDER_PATTERN =
                    Pattern.compile("(.*)via\\sgumtree$", Pattern.CASE_INSENSITIVE);
    static final String ANONYMOUS = "Anonymous";

    public static boolean isXml(String text) {
        return !Strings.isNullOrEmpty(text) && XML_PATTERN.matcher(text).find();
    }

    public static String senderName(String name) {
        return Optional.fromNullable(name).transform(new Function<String, String>() {

            @Nullable @Override public String apply(@Nullable String input) {
                if (Strings.isNullOrEmpty(input)) {
                    return ANONYMOUS;
                }

                String result = input;
                Matcher matcher = SENDER_PATTERN.matcher(input);
                if (matcher.matches()) {
                    result = matcher.group(1).trim();
                }
                return Strings.isNullOrEmpty(result) ? ANONYMOUS : result;
            }
        }).get();
    }
}
