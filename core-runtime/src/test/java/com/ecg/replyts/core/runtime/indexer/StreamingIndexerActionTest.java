package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StreamingIndexerActionTest {

    ConversationRepository conversationRepository = mock(ConversationRepository.class);
    IndexerBulkHandler indexerBulkHandler = mock(IndexerBulkHandler.class);

    BulkIndexerAction bulkIndexerAction = new BulkIndexerAction(conversationRepository, indexerBulkHandler, 8, 1000, 10);

    @Test
    public void shouldIndexWithDateRange() throws Exception {
        DateTime from = new DateTime("2015-06-01");
        DateTime to = new DateTime("2015-07-01");

        when(conversationRepository.streamConversationsModifiedBetween(from, to)).thenReturn(Stream.of("c1", "c2"));

        bulkIndexerAction.doIndexBetween(from, to, null, null);

        // Give scheduler some time to submit the task
        Thread.sleep(500);

        verify(indexerBulkHandler).indexChunk(new HashSet(Arrays.asList("c1", "c2")));
    }
}