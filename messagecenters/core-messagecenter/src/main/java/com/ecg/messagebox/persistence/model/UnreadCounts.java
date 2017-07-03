package com.ecg.messagebox.persistence.model;

import com.google.common.base.Objects;

public class UnreadCounts {
    private int userUnreadCounts;
    private int otherParticipantUnreadCount;

    public UnreadCounts(int userUnreadCounts, int otherParticipantUnreadCount) {
        this.userUnreadCounts = userUnreadCounts;
        this.otherParticipantUnreadCount = otherParticipantUnreadCount;
    }

    public int getUserUnreadCounts() {
        return userUnreadCounts;
    }

    public int getOtherParticipantUnreadCount() {
        return otherParticipantUnreadCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnreadCounts that = (UnreadCounts) o;
        return userUnreadCounts == that.userUnreadCounts &&
                otherParticipantUnreadCount == that.otherParticipantUnreadCount;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userUnreadCounts, otherParticipantUnreadCount);
    }
}
