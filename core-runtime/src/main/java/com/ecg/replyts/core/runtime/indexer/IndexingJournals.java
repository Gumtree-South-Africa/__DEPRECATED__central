package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.indexer.IndexerStatus;
import com.ecg.replyts.core.runtime.indexer.IndexingJournal.DataPayload;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IndexingJournals {
    @Autowired
    private HazelcastInstance hazelcastInstance;

    public IndexingJournal createJournalFor(DateTime start, DateTime end, IndexingMode mode) {
        IMap<IndexingMode, DataPayload> map = hazelcastInstance.getMap(getClass().getName());

        return new IndexingJournal(map, start, end, mode);
    }

    public IndexerStatus getLastRunStatisticsFor(IndexingMode mode) {
        DataPayload payload = hazelcastInstance.<IndexingMode, DataPayload>getMap(getClass().getName()).get(mode);

        if (payload == null) {
            return null;
        }

        return payload.toIndexerStatus();
    }
}
