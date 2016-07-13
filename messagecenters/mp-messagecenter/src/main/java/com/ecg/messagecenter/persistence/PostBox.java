package com.ecg.messagecenter.persistence;

import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

public class PostBox {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBox.class);

    private final String userId;
    private final List<ConversationThread> conversationThreads;

    @JsonIgnore
    private List<String> removedThreads = new ArrayList<>();

    public PostBox(String userId, List<ConversationThread> conversationThreads) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(conversationThreads);

        this.conversationThreads = cleanupAndSort(conversationThreads);
        this.userId = userId.toLowerCase();
    }

    public int getNewRepliesCounter() {
        return this.conversationThreads.stream().mapToInt(ConversationThread::getNumUnreadMessages).sum();
    }

    public int getNumUnreadConversations() {
        return (int) this.conversationThreads.stream().filter(ConversationThread::isContainsUnreadMessages).count();
    }

    public int getNumUnreadMessagesForConversation(String conversationId) {
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getConversationId().equals(conversationId)) {
                return conversationThread.getNumUnreadMessages();
            }
        }
        return 0;
    }

    public List<ConversationThread> getConversationThreads() {
        // we cap to 500 to not kill riak for very large objects
        return conversationThreads.stream().limit(500).collect(Collectors.toList());
    }

    public void removeConversations(List<String> conversationIds) {
        conversationIds.forEach(this::removeConversation);
    }

    public Optional<ConversationThread> removeConversation(String conversationId) {
        int indexToRemove = -1;
        for (int i = 0; i < conversationThreads.size(); i++) {
            if (conversationThreads.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
            }
        }
        if (indexToRemove != -1) {
            ConversationThread removed = conversationThreads.remove(indexToRemove);
            this.removedThreads.add(removed.getConversationId());
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    public void markConversationUnread(String conversationId, String message) {
        Optional<ConversationThread> oldConversation = removeConversation(conversationId);
        if (oldConversation.isPresent()) {
            conversationThreads.add(oldConversation.get().sameButUnread(message));
        } else {
            LOGGER.error("trying to mark conversation as unread but the conversation id is not in the postbox.");
        }
    }

    public Map<String, ConversationThread> getUnreadConversations() {
        return getUnreadConversationsInternal(Optional.<Integer>empty());
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
        return conversationThreads.stream().skip(page * maxSize).limit(maxSize).collect(Collectors.toList());
    }

    public String getUserId() {
        return userId;
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

    public Optional<ConversationThread> lookupConversation(String conversationId) {
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getConversationId().equals(conversationId)) {
                return Optional.of(conversationThread);
            }
        }
        return Optional.empty();
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

    private List<ConversationThread> cleanupOldConversations(List<ConversationThread> conversationThreads) {
        // ugly hardcoded 180 days (configured itself via replyts.maxConversationAgeDays but too much refactoring to tunnel param through)
        final DateTime conversationRetentionTime = DateTime.now().minusDays(180);
        return Lists.newArrayList(Collections2.filter(conversationThreads, new Predicate<ConversationThread>() {
            @Override
            public boolean apply(@Nullable ConversationThread input) {
                return input == null || input.getCreatedAt().isAfter(conversationRetentionTime);
            }
        }));
    }

    private List<ConversationThread> cleanupAndSort(List<ConversationThread> conversationThreads) {
        List<ConversationThread> tmp = cleanupOldConversations(conversationThreads);

        Collections.sort(tmp, (o1, o2) ->
                DateTimeComparator.getInstance().compare(
                        o2.getLastMessageCreatedAt().orElse(o2.getReceivedAt()),
                        o1.getLastMessageCreatedAt().orElse(o1.getReceivedAt())
                ));
        return tmp;
    }

    public List<String> flushRemovedThreads() {
        List<String> temp = removedThreads;
        removedThreads = new ArrayList<>();
        return temp;
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
