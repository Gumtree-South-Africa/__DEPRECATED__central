package com.ecg.replyts.core.api.indexer;

import java.util.Collection;

/**
 * Implementation can be informed about conversations that appear to be outdated in the search index and might need
 * reindexing. Reindexing is done asynchronousely.
 */
public interface OutdatedEntityReporter {
    /**
     * Marks a list of conversation as outdated. the conversations will be reloaded from database and indexed again.
     */
    void reportOutdated(Collection<String> conversationIds);
}
