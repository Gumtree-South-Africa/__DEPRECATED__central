package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.Counter;
import com.ecg.messagecenter.util.MessageCenterConstants;
import com.ecg.replyts.core.api.util.Pairwise;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.joda.time.DateTime.now;

public class PostBox<T extends AbstractConversationThread> {
    private static final Logger LOG = LoggerFactory.getLogger(PostBox.class);
    public static final int MAX_CONVERSATIONS_IN_POSTBOX = 500;

    private final PostBoxId id;
    private final String email;
    private final List<T> conversations;
    private final Counter newRepliesCounter;

    public PostBox(String email, Optional<Long> newRepliesCounter, List<T> conversations) {
        this(email, newRepliesCounter.isPresent() ? new Counter(newRepliesCounter.get()) : new Counter(), conversations);
    }

    public PostBox(String email, Counter newRepliesCounter, List<T> conversations) {
        Preconditions.checkNotNull(email);
        Preconditions.checkNotNull(conversations);
        this.id = PostBoxId.fromEmail(email);
        this.conversations = conversations.stream().sorted(RECEIVED_MODIFIED_CREATION_DATE).collect(toList());
        this.email = email.toLowerCase();
        this.newRepliesCounter = newRepliesCounter;
    }

    public Counter getNewRepliesCounter() {
        return newRepliesCounter;
    }

    public List<T> getConversationThreads() {
        return conversations.stream()
                // we cap to 500 to not kill the persistence store for very large objects
                .limit(MAX_CONVERSATIONS_IN_POSTBOX)
                .collect(Collectors.toList());
    }

    public static final Comparator<AbstractConversationThread> RECEIVED_MODIFIED_CREATION_DATE = Comparator.comparing(
            (AbstractConversationThread c) -> c.getReceivedAt().getMillis()).reversed()
            .thenComparing(c -> c.getModifiedAt().getMillis()).reversed()
            .thenComparing(c -> c.getCreatedAt().getMillis()).reversed()
            // betting on the fact that there is at max two messages with the same modif, creation and preview
            .thenComparing(c -> c.getPreviewLastMessage().orElse("0"));

    public Optional<T> removeConversation(String conversationId) {
        int indexToRemove = -1;
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getConversationId().equals(conversationId)) {
                indexToRemove = i;
                break;
            }
        }
        if (indexToRemove != -1) {
            return Optional.of(conversations.remove(indexToRemove));
        }
        return Optional.empty();
    }

    // This methods returns a copy of the conversation with all the conversation read and with modifiedDate set to now
    public Optional<T> cloneConversationMarkAsRead(String conversationId) {
        Optional<T> oldConversation = removeConversation(conversationId);
        if (oldConversation.isPresent()) {
            T newConversation = (T) oldConversation.get().newReadConversation();
            conversations.add(newConversation);
            return Optional.of(newConversation);
        } else {
            LOG.error("trying to mark conversation as read but the conversation id '{}' is not in the postbox.", conversationId);
        }

        return Optional.empty();
    }

    public void sortConversations() {
        conversations.sort(RECEIVED_MODIFIED_CREATION_DATE);
    }

    public Map<String, T> getUnreadConversations() {
        return getUnreadConversationsInternal(Optional.empty());
    }

    public Map<String, T> getUnreadConversationsCapped() {
        return getUnreadConversationsInternal(Optional.of(MessageCenterConstants.MAX_DISPLAY_CONVERSATIONS_TO_USER));
    }

    private Map<String, T> getUnreadConversationsInternal(Optional<Integer> maxSize) {
        Map<String, T> unreadConversations = new LinkedHashMap<>();

        List<T> threads;
        if (maxSize.isPresent()) {
            threads = getConversationThreadsCapTo(0, maxSize.get());
        } else {
            threads = conversations;
        }

        for (T conversation : threads) {
            if (conversation.isContainsUnreadMessages()) {
                unreadConversations.put(conversation.getConversationId(), conversation);
            }
        }
        return unreadConversations;
    }

    public List<T> getConversationThreadsCapTo(int page, int maxSize) {
        return conversations.stream()
                .skip(page * maxSize)
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    public List<T> getFilteredConversationThreads(Predicate<T> filter, int page, int maxSize) {
        return conversations.stream()
                .filter(filter)
                .skip(page * maxSize)
                .limit(maxSize)
                .collect(Collectors.toList());
    }

    public boolean containsConversation(AbstractConversationThread conversation) {
        return conversations.stream()
                .anyMatch(con -> con.getConversationId().equals(conversation.getConversationId()));
    }

    public String getEmail() {
        return email;
    }

    public PostBoxId getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostBox postBox = (PostBox) o;
        return Pairwise.pairsAreEqual(email, postBox.email, conversations, postBox.conversations);
    }

    public DateTime getLastModification() {
        if (conversations.isEmpty()) {
            return now();
        }
        DateTime last = now().minusYears(1000);
        for (T conversationThread : conversations) {
            if (conversationThread.getModifiedAt().isAfter(last)) {
                last = conversationThread.getModifiedAt();
            }
        }

        return last;
    }

    public Optional<T> lookupConversation(String conversationId) {
        for (T conversationThread : conversations) {
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

    @Override
    public String toString() {
        return toStringHelper(AbstractConversationThread::toString);
    }

    private String toStringHelper(Function<AbstractConversationThread, String> stringer) {
        StringBuilder objstr = new StringBuilder(MoreObjects.toStringHelper(this)
                .add("email", email)
                .add("newRepliesCounter", newRepliesCounter.getValue()).toString());

        objstr.append("\nNumber of conversations " + conversations.size() + "\n");
        List<AbstractConversationThread> cThreads = new ArrayList<>(conversations);
        conversations.sort(RECEIVED_MODIFIED_CREATION_DATE);

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
        return Objects.hashCode(email, conversations);
    }
}
