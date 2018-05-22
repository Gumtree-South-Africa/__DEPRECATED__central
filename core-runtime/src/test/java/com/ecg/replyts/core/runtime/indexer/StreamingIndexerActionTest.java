package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
  "replyts.indexer.streaming.threadcount = 8",
  "replyts.indexer.streaming.queue.size = 1000",
  "replyts.indexer.streaming.conversationid.batch.size = 10",
  "replyts.indexer.streaming.timeout.sec = 20"
})
public class StreamingIndexerActionTest {
    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private IndexerBulkHandler indexerBulkHandler;

    @Autowired
    private BulkIndexerAction bulkIndexerAction;

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

    @Configuration
    @Import(BulkIndexerAction.class)
    static class Context {
        @MockBean
        private ConversationRepository conversationRepository;

        @MockBean
        private IndexerBulkHandler indexerBulkHandler;
    }
}