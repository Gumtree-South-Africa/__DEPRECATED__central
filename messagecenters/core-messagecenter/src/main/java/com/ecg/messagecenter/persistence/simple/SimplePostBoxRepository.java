package com.ecg.messagecenter.persistence.simple;

import com.ecg.messagecenter.persistence.AbstractConversationThread;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Optional;

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
    PostBox byId(String email);

    void write(PostBox postBox);

    void write(PostBox postBox, List<String> deletedIds);

    void cleanup(DateTime time);

    Optional<AbstractConversationThread> threadById(String email, String conversationId);

    Long upsertThread(String email, AbstractConversationThread conversationThread, boolean markAsUnread);
}
