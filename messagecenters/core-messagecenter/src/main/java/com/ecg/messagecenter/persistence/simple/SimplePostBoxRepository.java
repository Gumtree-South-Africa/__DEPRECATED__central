package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository for the 'simple' variety of the PostBox store. The 'simple' variety is a hybrid of the 1st generation
 * repository already used by KJCA, GTAU, and eBayK, as well as the 2nd generation Marktplaats repository with several
 * performance improvements. Marktplaats has since created a 3rd generation repository in the form of their 'messagebox'
 * implementation; the eventual goal is to move all tenants over to this repository/implementationm. This is the
 * intermediate step to getting there and requires no REST/datamodel/migration while still being able to run on more
 * than just Riak and consolidate several shared messagecenter pieces into core-messagecenter.
 *
 * It currently has a Riak, Cassandra, and a Hybrid (migration) implementation. There is also
 * a Riak 'read-only' implementation which is used to talk to tenants' production repositories to test migration with.
 */
public interface SimplePostBoxRepository {

    PostBox byId(PostBoxId id);

    void write(PostBox postBox);

    void deleteConversations(PostBox postBox, List<String> deletedIds);

    default void markConversationAsRead(PostBox postBox, AbstractConversationThread conversation) {
        markConversationsAsRead(postBox, Collections.singletonList(conversation));
    }

    void markConversationsAsRead(PostBox postBox, List<AbstractConversationThread> conversations);

    void cleanup(DateTime time);

    Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId);

    Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean markAsUnread);

    int unreadCountInConversation(PostBoxId id, String conversationId);

    int unreadCountInConversations(PostBoxId id, List<AbstractConversationThread> conversations);
}
