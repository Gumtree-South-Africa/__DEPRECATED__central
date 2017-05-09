package com.ecg.replyts.core.webapi.control;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

public class Util {

    public static final String DEFAULT_NO_EXECUTION_MESSAGE = "No action was triggered as other process is already executing migration";
    public static final Splitter CSV_SPLITTER = Splitter.on(CharMatcher.WHITESPACE.or(CharMatcher.is(',')).or(CharMatcher.BREAKING_WHITESPACE)).trimResults().omitEmptyStrings();

}
