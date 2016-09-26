package com.ecg.messagebox.diff;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.runtime.TimingReports.newCounter;

@Component
public class UnreadCountsDiff {

    private final Counter diffCounter = newCounter("diff.postBoxUnreadCountsDiff.counter");

    private final DiffReporter reporter;
    private final boolean checkUnreadCounts;

    @Autowired
    public UnreadCountsDiff(DiffReporter reporter,
                            @Value("${messagebox.diff.checkUnreadCounts:true}") boolean checkUnreadCounts) {
        this.reporter = reporter;
        this.checkUnreadCounts = checkUnreadCounts;
    }

    public void diff(String userId, PostBoxUnreadCounts newValue, PostBoxUnreadCounts oldValue) {
        if (checkUnreadCounts) {
            if (newValue.getNumUnreadConversations() != oldValue.getNumUnreadConversations()) {
                logDiffForUnreadCounts(userId, "numUnreadConversations",
                        Integer.toString(newValue.getNumUnreadConversations()),
                        Integer.toString(oldValue.getNumUnreadConversations()),
                        false);
            }
            if (newValue.getNumUnreadMessages() != oldValue.getNumUnreadMessages()) {
                logDiffForUnreadCounts(userId, "numUnreadMessages",
                        Integer.toString(newValue.getNumUnreadMessages()),
                        Integer.toString(oldValue.getNumUnreadMessages()),
                        false);
            }
        }
    }

    private void logDiffForUnreadCounts(String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        diffCounter.inc();
        logDiff("unreadCountsDiff", params, fieldName, newValue, oldValue, useNewLogger);
    }

    private void logDiff(String methodName, String params, String fieldName, String newValue, String oldValue, boolean useNewLogger) {
        reporter.report(
                String.format("%s(%s) - %s - new: '%s' vs old: '%s'", methodName, params, fieldName, newValue, oldValue),
                useNewLogger
        );
    }
}
