package com.ecg.messagecenter.persistence;

import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

public class PostBox {

    private final String userId;
    private final List<ConversationThread> conversationThreads;

    public PostBox(String userId, List<ConversationThread> conversationThreads) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(conversationThreads);

        this.conversationThreads = new ArrayList<>(conversationThreads);
        Collections.sort(this.conversationThreads, (o1, o2) ->
                DateTimeComparator.getInstance().compare(
                        o2.getLastMessageCreatedAt().orElse(o2.getReceivedAt()),
                        o1.getLastMessageCreatedAt().orElse(o1.getReceivedAt())
                ));
        this.userId = userId.toLowerCase();
    }

    public String getUserId() {
        return userId;
    }

    public List<ConversationThread> getConversationThreads() {
        return conversationThreads;
    }

    public int getNumUnreadMessages() {
        return this.conversationThreads.stream().mapToInt(ConversationThread::getNumUnreadMessages).sum();
    }

    public int getNumUnreadConversations() {
        return (int) this.conversationThreads.stream().filter(ConversationThread::isContainsUnreadMessages).count();
    }

    public void removeConversations(List<String> conversationIds) {
        conversationIds.forEach(id -> conversationThreads.removeIf(ct -> ct.getConversationId().equals(id)));
    }

    public List<ConversationThread> getConversationThreadsCapTo(int page, int maxSize) {
        return conversationThreads.stream().skip(page * maxSize).limit(maxSize).collect(Collectors.toList());
    }

    public DateTime getLastModification() {
        if (conversationThreads.isEmpty()) {
            return now();
        }
        DateTime last = now().minusYears(1000);
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getModifiedAt().isAfter(last)) {
                last = conversationThread.getModifiedAt();
            }
        }
        return last;
    }

    /**
     * @return The new post box if it was changed or the original one if there were no unread messages
     */
    public PostBox markAllAsRead() {
        AtomicBoolean changed = new AtomicBoolean(false);
        List<ConversationThread> newList = this.conversationThreads.stream().map(ct -> {
            if (ct.isContainsUnreadMessages()) {
                changed.set(true);
                return ct.sameButRead();
            } else {
                return ct;
            }
        }).collect(Collectors.toList());
        return changed.get() ? new PostBox(this.userId, newList) : this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBox postBox = (PostBox) o;

        return Pairwise.pairsAreEqual(userId, postBox.userId, conversationThreads, postBox.conversationThreads);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, conversationThreads);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("userId", userId)
                .add("conversationThreads", conversationThreads)
                .toString();
    }
}