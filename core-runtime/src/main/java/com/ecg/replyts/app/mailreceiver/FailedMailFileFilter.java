package com.ecg.replyts.app.mailreceiver;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class FailedMailFileFilter implements FileFilter {
    private Pattern failureFileNamePattern;
    private int minimumAge;

    FailedMailFileFilter(Pattern failurePattern, int minimumAge) {
        this.failureFileNamePattern = failurePattern;
        this.minimumAge = minimumAge;
    }

    @Override
    public boolean accept(File file) {
        Matcher matcher = failureFileNamePattern.matcher(file.getName());
        long interval = System.currentTimeMillis() - (60L * 1000 * minimumAge);
        return matcher.matches() && file.lastModified() < interval;
    }

}
