package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.indexer.IndexerStatus;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.ecg.replyts.core.runtime.indexer.IndexingMode.DELTA;
import static com.google.common.collect.Maps.newHashMap;
import static org.joda.time.DateTime.parse;
import static org.junit.Assert.*;

public class IndexingJournalTest {

    private IndexingJournal journal;

    private Map<IndexingMode, IndexingJournal.DataPayload> distrubutedMap = newHashMap();
    @Before
    public void setUp() {
        journal = new IndexingJournal(distrubutedMap, parse("2000-01-01"), parse("2010-01-01"), DELTA);
    }

    @Test
    public void reportsIsRunningForNewJob() {
        journal.startRunning(1000);

        IndexerStatus indexerStatus = getStatus();
        assertTrue(indexerStatus.isRunning());
        assertNotNull(indexerStatus.getStartDate());
        assertNull(indexerStatus.getEndDate());
        assertEquals(0, indexerStatus.getCompletedChunks());
        assertEquals(1000, indexerStatus.getTotalChunks());
    }

    private IndexerStatus getStatus() {
        return distrubutedMap.get(DELTA).toIndexerStatus();
    }

    @Test
    public void updatesExistingJobInfo() {

        IndexingJournal.DataPayload value = new IndexingJournal.DataPayload(parse("2010-01-01"), parse("2011-01-01"), DELTA);
        value.setChunkCount(999);
        distrubutedMap.put(DELTA, value);


        journal.completedChunk();


        IndexerStatus status = getStatus();
        assertEquals(999, status.getTotalChunks());
        assertEquals(1, status.getCompletedChunks());

    }

    @Test
    public void completesRunning() {
        journal.startRunning(1);
        journal.completedChunk();
        journal.completeRunning();

        IndexerStatus status = getStatus();

        assertNotNull(status.getEndDate());
        assertNotNull(status.getStartDate());

    }
}
