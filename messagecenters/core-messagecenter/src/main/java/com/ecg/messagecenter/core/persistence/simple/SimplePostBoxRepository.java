package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import org.joda.time.DateTime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface SimplePostBoxRepository {

    PostBox byId(PostBoxId id);

    PostBox byIdWithoutConversationThreads(PostBoxId id);

    void write(PostBox postBox);

    void deleteConversations(PostBox postBox, List<String> deletedIds);

    default void markConversationAsRead(PostBox postBox, AbstractConversationThread conversation) {
        markConversationsAsRead(postBox, Collections.singletonList(conversation));
    }

    void markConversationsAsRead(PostBox postBox, List<AbstractConversationThread> conversations);

    /**
     * Returns <code>true</code> if the cleanup for this time is finished.
     *
     * @param time The rounded modification date of the postbox modification index by date.
     * @return Returns <code>true</code> if the cleanup for this time is finished; returns <code>false</code> when
     * the cleanup for this time is not finished;
     */
    boolean cleanup(DateTime time);

    Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId);

    Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean markAsUnread);

    int unreadCountInConversation(PostBoxId id, String conversationId);

    int unreadCountInConversations(PostBoxId id, List<AbstractConversationThread> conversations);
}
