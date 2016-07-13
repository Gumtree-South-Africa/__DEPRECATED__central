package com.ecg.messagecenter.persistence;

import com.ecg.messagecenter.util.MessageCenterConstants;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

public class PostBox {

    private final String email;
    private final List<ConversationThread> conversationThreads;
    private final Counter newRepliesCounter;

    private static final Logger LOG = LoggerFactory.getLogger(PostBox.class);

    public PostBox(String email, Optional<Long> newRepliesCounter, List<ConversationThread> conversationThreads) {
        this(email, newRepliesCounter.isPresent() ? new Counter(newRepliesCounter.get()) : new Counter(), conversationThreads);
    }

    public PostBox(String email, Counter newRepliesCounter, List<ConversationThread> conversationThreads) {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversationThreads);

        this.conversationThreads = sortDescByReceivedDate(conversationThreads);
        this.email = email.toLowerCase();
        this.newRepliesCounter = newRepliesCounter;
    }

    public Counter getNewRepliesCounter() {
        return newRepliesCounter;
    }

    public List<ConversationThread> getConversationThreads() {
        return FluentIterable.from(conversationThreads)
                .limit(500) // we cap to 500 to not kill riak for very large objects
                .toList();
    }

    public Optional<ConversationThread> removeConversation(String conversationId) {
        int indexToRemove = -1;
        for (int i = 0; i < conversationThreads.size(); i++) {
            if (conversationThreads.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
            }
        }
        if (indexToRemove != -1) {
            return Optional.of(conversationThreads.remove(indexToRemove));
        }
        return Optional.empty();
    }

    public void markConversationUnread(String conversationId, String message) {
        newRepliesCounter.inc();
        Optional<ConversationThread> oldConversation = removeConversation(conversationId);
        if (oldConversation.isPresent()) {
            conversationThreads.add(oldConversation.get().sameButUnread(message));
        } else {
            LOG.error("trying to mark conversation as unread but the conversation id is not in the postbox.");
        }
    }

    public Map<String, ConversationThread> getUnreadConversations() {
        return getUnreadConversationsInternal(Optional.empty());
    }

    public Map<String, ConversationThread> getUnreadConversationsCapped() {
        return getUnreadConversationsInternal(Optional.of(MessageCenterConstants.MAX_DISPLAY_CONVERSATIONS_TO_USER));
    }

    public Map<String, ConversationThread> getUnreadConversationsInternal(Optional<Integer> maxSize) {
        Map<String, ConversationThread> unreadConversations = new LinkedHashMap<>();

        List<ConversationThread> threads;
        if (maxSize.isPresent()) {
            threads = getConversationThreadsCapTo(0, maxSize.get());
        } else {
            threads = conversationThreads;
        }

        for (ConversationThread conversation : threads) {
            if (conversation.isContainsUnreadMessages()) {
                unreadConversations.put(conversation.getConversationId(), conversation);
            }
        }
        return unreadConversations;
    }

    public List<ConversationThread> getConversationThreadsCapTo(int page, int maxSize) {
        return FluentIterable.from(conversationThreads)
                .skip(page * maxSize)
                .limit(maxSize)
                .toList();
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBox postBox = (PostBox) o;

        return Pairwise.pairsAreEqual(email, postBox.email, newRepliesCounter, postBox.newRepliesCounter, conversationThreads, postBox.conversationThreads);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email, newRepliesCounter, conversationThreads);
    }

    public DateTime getLastModification() {
        if (conversationThreads.isEmpty()) {
            return now(UTC);
        }
        DateTime last = now(UTC).minusYears(1000);
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getModifiedAt().isAfter(last)) {
                last = conversationThread.getModifiedAt();
            }
        }

        return last;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("email", email)
                .add("newRepliesCounter", newRepliesCounter)
                .add("conversationThreads", conversationThreads)
                .toString();
    }

    public Optional<ConversationThread> lookupConversation(String conversationId) {
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getConversationId().equals(conversationId)) {
                return Optional.of(conversationThread);
            }
        }
        return Optional.empty();
    }

    public void incNewReplies() {
        this.newRepliesCounter.inc();
    }

    public void decrementNewReplies() {
        this.newRepliesCounter.dec();
    }

    public void resetReplies() {
        this.newRepliesCounter.reset();
    }

    public PostBox withoutThreadsCreatedBefore(DateTime cutoffDate) {
        return new PostBox(
                email,
                newRepliesCounter,
                conversationThreads.stream()
                        .filter(thread -> thread.getCreatedAt().isAfter(cutoffDate))
                        .collect(Collectors.toList()));
    }

    private List<ConversationThread> sortDescByReceivedDate(List<ConversationThread> conversationThreads) {
        return conversationThreads
                .stream()
                .sorted((o1, o2) -> o2.getReceivedAt().compareTo(o1.getReceivedAt()))
                .collect(Collectors.toList());
    }
}
