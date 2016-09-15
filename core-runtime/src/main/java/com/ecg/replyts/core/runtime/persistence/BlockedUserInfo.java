package com.ecg.replyts.core.runtime.persistence;

import org.joda.time.DateTime;

import java.util.Objects;

/**
 * Contains information about blocked users:
 * the id of the user who did the blocking
 * the id of the user who was blocked.
 * the date when the block happened
 */
public class BlockedUserInfo {

    private String reporterUserId;
    private String blockedUserId;
    private DateTime blockedDate;

    public BlockedUserInfo(String reporterUserId, String blockedUserId, DateTime blockedDate) {
        this.reporterUserId = reporterUserId;
        this.blockedUserId = blockedUserId;
        this.blockedDate = blockedDate;
    }

    public String getReporterUserId() {
        return reporterUserId;
    }

    public String getBlockedUserId() {
        return blockedUserId;
    }

    public DateTime getBlockedDate() {
        return blockedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockedUserInfo that = (BlockedUserInfo) o;
        return Objects.equals(reporterUserId, that.reporterUserId)
                && Objects.equals(blockedUserId, that.blockedUserId)
                && Objects.equals(blockedDate, that.blockedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reporterUserId, blockedUserId, blockedDate);
    }
}