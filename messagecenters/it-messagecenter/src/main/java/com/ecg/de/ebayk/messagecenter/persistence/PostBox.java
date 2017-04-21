package com.ecg.de.ebayk.messagecenter.persistence;

import com.ecg.de.ebayk.messagecenter.util.MessageCenterConstants;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.*;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static org.joda.time.DateTime.now;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 15:39
 *
 * @author maldana@ebay.de
 */
public class PostBox {

    private final String email;
    private final List<ConversationThread> conversationThreads;
    private final Counter newRepliesCounter;

    private static final Logger LOG = LoggerFactory.getLogger(PostBox.class);

    public PostBox(String email, Optional<Long> newRepliesCounter,
                    List<ConversationThread> conversationThreads) {
        this(email, new Counter(newRepliesCounter.or(0L)), conversationThreads);
        /*isPresent() ? new Counter(newRepliesCounter.get()) : new Counter()*/
    }

    public PostBox(String email, Counter newRepliesCounter,
                    List<ConversationThread> conversationThreads) {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversationThreads);

        this.conversationThreads = cleanupAndSortByReceivedDate(conversationThreads);
        this.email = email.toLowerCase();
        this.newRepliesCounter = newRepliesCounter;
    }

    public Counter getNewRepliesCounter() {
        return newRepliesCounter;
    }

    public FluentIterable<ConversationThread> filter(Predicate<? super ConversationThread> p) {
        return FluentIterable.from(conversationThreads).filter(p);
    }

    public List<ConversationThread> getConversationThreads() {
        return FluentIterable.from(conversationThreads).limit(
                        500) // we cap to 500 to not kill riak for very large objects
                        .toList();
    }

    public Optional<ConversationThread> removeConversation(String conversationId) {
        LOG.debug("removeConversation: " + conversationId);
        int indexToRemove = -1;
        for (int i = 0; i < conversationThreads.size(); i++) {
            if (conversationThreads.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
            }
        }
        if (indexToRemove != -1) {
            return of(conversationThreads.remove(indexToRemove));
        }
        return absent();
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
        return getUnreadConversationsInternal(Optional.<Integer>absent());
    }

    public Map<String, ConversationThread> getUnreadConversationsCapped() {
        return getUnreadConversationsInternal(
                        of(MessageCenterConstants.MAX_DISPLAY_CONVERSATIONS_TO_USER));
    }

    public Map<String, ConversationThread> getUnreadConversationsInternal(
                    Optional<Integer> maxSize) {
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
        return FluentIterable.from(conversationThreads).skip(page * maxSize).limit(maxSize)
                        .toList();
    }

    public String getEmail() {
        return email;
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        PostBox postBox = (PostBox) o;

        return Pairwise.pairsAreEqual(email, postBox.email, newRepliesCounter,
                        postBox.newRepliesCounter, conversationThreads,
                        postBox.conversationThreads);
    }

    @Override public int hashCode() {
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

    @Override public String toString() {
        return Objects.toStringHelper(this).add("email", email)
                        .add("newRepliesCounter", newRepliesCounter)
                        .add("conversationThreads", conversationThreads).toString();
    }

    public Optional<ConversationThread> lookupConversation(String conversationId) {
        for (ConversationThread conversationThread : conversationThreads) {
            if (conversationThread.getConversationId().equals(conversationId)) {
                return of(conversationThread);
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

    private List<ConversationThread> cleanupOldConversations(
                    List<ConversationThread> conversationThreads) {
        // ugly hardcoded 180 days (configured itself via replyts.maxConversationAgeDays but too much refactoring to tunnel param through)
        final DateTime conversationRetentionTime = DateTime.now().minusDays(180);
        return Lists.newArrayList(Collections2.filter(conversationThreads,
                        new Predicate<ConversationThread>() {
                            @Override public boolean apply(@Nullable ConversationThread input) {
                                return input == null || input.getCreatedAt()
                                                .isAfter(conversationRetentionTime);
                            }
                        }));
    }

    private List<ConversationThread> cleanupAndSortByReceivedDate(
                    List<ConversationThread> conversationThreads) {
        List<ConversationThread> tmp = cleanupOldConversations(conversationThreads);

        Collections.sort(tmp, new Comparator<ConversationThread>() {
            @Override public int compare(ConversationThread o1, ConversationThread o2) {
                return DateTimeComparator.getInstance()
                                .compare(o2.getReceivedAt(), o1.getReceivedAt());

            }
        });
        return tmp;
    }
}
