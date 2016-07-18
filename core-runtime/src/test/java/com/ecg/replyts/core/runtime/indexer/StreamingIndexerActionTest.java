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
    IndexerChunkHandler indexerChunkHandler = mock(IndexerChunkHandler.class);

    StreamingIndexerAction streamingIndexerAction = new StreamingIndexerAction(conversationRepository, indexerChunkHandler, 8, 1000, 10, 300);

    @Test
    public void shouldIndexWithDateRange() throws Exception {
        DateTime from = new DateTime("2015-06-01");
        DateTime to = new DateTime("2015-07-01");

        when(conversationRepository.streamConversationsModifiedBetween(from, to)).thenReturn(Stream.of("c1", "c2"));

        streamingIndexerAction.doIndexBetween(from, to, null, null);

        // Give scheduler some time to submit the task
        Thread.sleep(500);

        verify(indexerChunkHandler).indexChunkAsync(new HashSet(Arrays.asList("c1", "c2")));
    }
}