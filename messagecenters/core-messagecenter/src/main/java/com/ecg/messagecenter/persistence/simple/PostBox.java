package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.Counter;
import com.ecg.messagecenter.util.MessageCenterConstants;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.FluentIterable;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.joda.time.DateTime.now;

public class PostBox<T extends AbstractConversationThread> {
    private static final Logger LOG = LoggerFactory.getLogger(PostBox.class);

    private final String email;
    private final List<T> conversationThreads;
    private final Counter newRepliesCounter;
    private final int maxAgeDays;

    public PostBox(String email, Optional<Long> newRepliesCounter, List<T> conversationThreads, int maxAgeDays) {
        this(email, newRepliesCounter.isPresent() ? new Counter(newRepliesCounter.get()) : new Counter(), conversationThreads, maxAgeDays);
    }

    public PostBox(String email, Counter newRepliesCounter, List<T> conversationThreads, int maxAgeDays) {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversationThreads);
        this.maxAgeDays = maxAgeDays;
        this.conversationThreads = cleanupAndSortByModificationCreationDate(conversationThreads);
        this.email = email.toLowerCase();
        this.newRepliesCounter = newRepliesCounter;
    }

    public Counter getNewRepliesCounter() {
        return newRepliesCounter;
    }

    public List<T> getConversationThreads() {
        return FluentIterable.from(conversationThreads)
                .limit(500) // we cap to 500 to not kill the persistence store for very large objects
                .toList();
    }


    public static final Comparator<AbstractConversationThread> RECEIVED_MODIFIED_CREATION_DATE = Comparator.comparing(
            (AbstractConversationThread c) -> c.getReceivedAt().getMillis()).reversed()
            .thenComparing(c -> c.getModifiedAt().getMillis()).reversed()
            .thenComparing(c -> c.getCreatedAt().getMillis()).reversed()
            // betting on the fact that there is at max two messages with the same modif, creation and preview
            .thenComparing(c -> c.getPreviewLastMessage().orElse("0"));

    public Optional<T> removeConversation(String conversationId) {
        int indexToRemove = -1;
        for (int i = 0; i < conversationThreads.size(); i++) {
            if (conversationThreads.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove != -1) {
            return Optional.of(conversationThreads.remove(indexToRemove));
        }
        return Optional.empty();
    }

    public void markConversationUnread(String conversationId, String message) {
        newRepliesCounter.inc();
        Optional<T> oldConversation = removeConversation(conversationId);
        if (oldConversation.isPresent()) {
            conversationThreads.add((T) oldConversation.get().sameButUnread(message));
        } else {
            LOG.error("trying to mark conversation as unread but the conversation id '{}' is not in the postbox.", conversationId);
        }
    }

    public Map<String, T> getUnreadConversations() {
        return getUnreadConversationsInternal(Optional.empty());
    }

    public Map<String, T> getUnreadConversationsCapped() {
        return getUnreadConversationsInternal(Optional.of(MessageCenterConstants.MAX_DISPLAY_CONVERSATIONS_TO_USER));
    }

    public Map<String, T> getUnreadConversationsInternal(Optional<Integer> maxSize) {
        Map<String, T> unreadConversations = new LinkedHashMap<>();

        List<T> threads;
        if (maxSize.isPresent()) {
            threads = getConversationThreadsCapTo(0, maxSize.get());
        } else {
            threads = conversationThreads;
        }

        for (T conversation : threads) {
            if (conversation.isContainsUnreadMessages()) {
                unreadConversations.put(conversation.getConversationId(), conversation);
            }
        }
        return unreadConversations;
    }

    public List<T> getConversationThreadsCapTo(int page, int maxSize) {
        return FluentIterable.from(conversationThreads)
                .skip(page * maxSize)
                .limit(maxSize)
                .toList();
    }

    public List<T> getFilteredConversationThreads(Predicate<T> filter, int page, int maxSize) {
        return FluentIterable.from(conversationThreads)
                .filter(filter)
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
        return Pairwise.pairsAreEqual(email, postBox.email, conversationThreads, postBox.conversationThreads);
    }

    public DateTime getLastModification() {
        if (conversationThreads.isEmpty()) {
            return now();
        }
        DateTime last = now().minusYears(1000);
        for (T conversationThread : conversationThreads) {
            if (conversationThread.getModifiedAt().isAfter(last)) {
                last = conversationThread.getModifiedAt();
            }
        }

        return last;
    }

    public Optional<T> lookupConversation(String conversationId) {
        for (T conversationThread : conversationThreads) {
            if (conversationThread.getConversationId().equals(conversationId)) {
                return Optional.of(conversationThread);
            }
        }
        return Optional.empty();
    }

    public void incNewReplies() {
        this.newRepliesCounter.inc();
    }

    public void decrementNewReplies(long numOfReplies) {
        this.newRepliesCounter.dec(numOfReplies);
    }

    public void resetReplies() {
        this.newRepliesCounter.reset();
    }

    private List<T> cleanupExpiredConversations(List<T> conversationThreads) {
        final DateTime conversationRetentionTime = DateTime.now().minusDays(maxAgeDays);

        return conversationThreads
                .stream()
                .filter(input -> input == null || input.getCreatedAt().isAfter(conversationRetentionTime))
                .collect(Collectors.toList());
    }

    private List<T> cleanupAndSortByModificationCreationDate(List<T> conversationThreads) {
        List<T> sorted = cleanupExpiredConversations(conversationThreads);
        Collections.sort(sorted, RECEIVED_MODIFIED_CREATION_DATE);
        return sorted;
    }

    @Override
    public String toString() {
        return toStringHelper(AbstractConversationThread::toString);
    }

    private String toStringHelper(Function<AbstractConversationThread, String> stringer) {
        StringBuilder objstr = new StringBuilder(MoreObjects.toStringHelper(this)
                .add("email", email)
                .add("newRepliesCounter", newRepliesCounter.getValue()).toString());

        objstr.append("\nNumber of conversations " + conversationThreads.size() + "\n");
        List<AbstractConversationThread> cThreads = new ArrayList<>(conversationThreads);
        Collections.sort(conversationThreads, RECEIVED_MODIFIED_CREATION_DATE);

        for(AbstractConversationThread ct: cThreads) {
            objstr.append(stringer.apply(ct));
            objstr.append("\n");
        }
        return objstr.toString();
    }

    public String fullToString() {
        return toStringHelper(AbstractConversationThread::fullToString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(email, conversationThreads);
    }
}
