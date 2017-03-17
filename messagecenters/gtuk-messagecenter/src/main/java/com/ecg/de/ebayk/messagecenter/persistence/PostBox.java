package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.de.ebayk.messagecenter.util.MessageCenterConstants;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.joda.time.DateTime.now;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 15:39
 *
 * @author maldana@ebay.de
 */
public class PostBox {

    public static final int DEFAULT_MAX_CONVERSATION_AGE_DAYS = 30;

    private final String email;
    private final List<ConversationThread> conversationThreads;
    private final Counter newRepliesCounter;
    private int maxConversationAgeDays;


    private PostBox(String email,
                    Counter newRepliesCounter,
                    List<ConversationThread> conversationThreads,
                    Optional<Integer> maxConversationAgeDaysOpt) {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversationThreads);

        this.email = email.toLowerCase();
        this.newRepliesCounter = newRepliesCounter;
        this.maxConversationAgeDays = maxConversationAgeDaysOpt.or(DEFAULT_MAX_CONVERSATION_AGE_DAYS);
        this.conversationThreads = cleanupAndSortByReceivedDate(conversationThreads);
    }

    public Counter getNewRepliesCounter() {
        return newRepliesCounter;
    }

    public List<ConversationThread> getConversationThreads() {
        return FluentIterable.from(conversationThreads)
                .limit(500) // we cap to 500 to not kill riak for very large objects
                .toList();
    }

    public void removeConversation(String conversationId) {
        int indexToRemove = -1;
        for (int i = 0; i < conversationThreads.size(); i++) {
            if (conversationThreads.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
            }
        }
        if (indexToRemove != -1) {
            conversationThreads.remove(indexToRemove);
        }
    }

    public Map<String, ConversationThread> getUnreadConversations() {
        return getUnreadConversationsInternal(Optional.<Integer>absent());
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PostBox postBox = (PostBox) o;

        return Pairwise.pairsAreEqual(
                email, postBox.email,
                newRepliesCounter, postBox.newRepliesCounter,
                conversationThreads, postBox.conversationThreads);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email, newRepliesCounter, conversationThreads);
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
        return Optional.absent();
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

    private List<ConversationThread> cleanupOldConversations(List<ConversationThread> conversationThreads) {
        final DateTime conversationRetentionTime = DateTime.now().minusDays(getMaxConversationAgeDays());
        return Lists.newArrayList(Collections2.filter(conversationThreads,
                input -> input == null || input.getCreatedAt().isAfter(conversationRetentionTime)));
    }

    private List<ConversationThread> cleanupAndSortByReceivedDate(List<ConversationThread> conversationThreads) {
        List<ConversationThread> tmp = cleanupOldConversations(conversationThreads);
        Collections.sort(tmp, (o1, o2) ->
                DateTimeComparator.getInstance().compare(o2.getReceivedAt(), o1.getReceivedAt()));
        return tmp;
    }

    private int getMaxConversationAgeDays() {
        return maxConversationAgeDays > 0 ? maxConversationAgeDays : DEFAULT_MAX_CONVERSATION_AGE_DAYS;
    }

    public static class PostBoxBuilder {
        private String email;
        private List<ConversationThread> conversationThreads;
        private Counter newRepliesCounter = new Counter(0L);
        private Integer maxConversationAgeDays;

        public PostBoxBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public PostBoxBuilder withNewRepliesCounter(Long newRepliesCounter) {
            this.newRepliesCounter = new Counter(newRepliesCounter);
            return this;
        }

        public PostBoxBuilder withConversationThreads(List<ConversationThread> conversationThreads) {
            this.conversationThreads = conversationThreads;
            return this;
        }

        public PostBoxBuilder withMaxConversationAgeDays(int maxConversationAgeDays) {
            this.maxConversationAgeDays = maxConversationAgeDays;
            return this;
        }

        public PostBox build() {
            return new PostBox(email, newRepliesCounter, conversationThreads, Optional.fromNullable(maxConversationAgeDays));
        }
    }
}
