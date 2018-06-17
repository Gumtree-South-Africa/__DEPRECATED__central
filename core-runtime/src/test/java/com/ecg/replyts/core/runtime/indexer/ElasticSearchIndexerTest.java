package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
import com.ecg.replyts.core.runtime.workers.InstrumentedExecutorService;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.joda.time.DateTimeFieldType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.*;

@Import({ElasticSearchIndexer.class})
@RunWith(MockitoJUnitRunner.class)
public class ElasticSearchIndexerTest {

    private ExecutorService executorService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private Conversation2Kafka conversation2Kafka;

    @InjectMocks
    private ElasticSearchIndexer elasticSearchIndexer;

    private final int MAX_AGE_DAYS = 180;
    private final DateTime NOW = DateTime.now();

    @Before
    public void setup() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", ElasticSearchIndexer.class.getSimpleName());
        executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);
        CompletionService completionService = new ExecutorCompletionService(executorService);

        ReflectionTestUtils.setField(elasticSearchIndexer, "executor", executorService);
        ReflectionTestUtils.setField(elasticSearchIndexer, "executorService", executorService);
        ReflectionTestUtils.setField(elasticSearchIndexer, "completionService", completionService);
        ReflectionTestUtils.setField(elasticSearchIndexer, "taskCompletionTimeoutSec", 2);
        ReflectionTestUtils.setField(elasticSearchIndexer, "maxAgeDays", MAX_AGE_DAYS);
        ReflectionTestUtils.setField(elasticSearchIndexer, "convIdDedupBufferSize", 100);
        ReflectionTestUtils.setField(elasticSearchIndexer, "clock", new CurrentClock());

        ReflectionTestUtils.setField(conversation2Kafka, "fetchedConvCounter", new AtomicLong(0));

        List<Conversation> conversations = Lists.newArrayList();
        final List<String> CONV_IDS = Lists.newArrayList("foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7", "foo8", "foo9");
        for (String conversationId : CONV_IDS) {
            MutableConversation mutableConversation = mock(MutableConversation.class);
            when(mutableConversation.getId()).thenReturn(conversationId);
            conversations.add(mutableConversation);

            when(conversationRepository.getById(conversationId)).thenReturn(mutableConversation);
        }
    }

    @After
    public void shutdown() {
        executorService.shutdownNow();
    }

    @Test
    public void indexSince() {
        final List<String> CONV_IDS = Lists.newArrayList("foo1", "foo2", "foo3");
        elasticSearchIndexer.indexConversations(CONV_IDS.stream());
        verify(conversation2Kafka, times(CONV_IDS.size())).updateElasticSearch(anyString());
    }

    @Test
    public void duplicatedIdsAreFilteredOut() {
        final List<String> CONV_IDS = Lists.newArrayList("foo1", "foo2", "foo3", "foo1", "foo2");
        elasticSearchIndexer.indexConversations(CONV_IDS.stream());
        verify(conversation2Kafka, times(3)).updateElasticSearch(anyString());
    }


    @Test
    public void indexBetween() {
        DateTime FROM = DateTime.now().minusDays(1);
        final List<String> CONV_IDS = Lists.newArrayList("foo4", "foo5", "foo6");
        when(conversationRepository.streamConversationsModifiedBetween(FROM, NOW)).thenReturn(CONV_IDS.stream());
        elasticSearchIndexer.doIndexBetween(FROM, NOW);

        verify(conversation2Kafka, times(CONV_IDS.size())).updateElasticSearch(anyString());
    }

    @Test
    public void fullReindex() {
        final List<String> CONV_IDS = Lists.newArrayList("foo7", "foo8", "foo9");
        when(conversationRepository.streamConversationsModifiedBetween(any(DateTime.class), any(DateTime.class))).thenReturn(CONV_IDS.stream());
        elasticSearchIndexer.fullIndex();

        verify(conversation2Kafka, times(CONV_IDS.size())).updateElasticSearch(anyString());
    }

    @Test
    public void startTimeForFullReindex() {
        DateTimeComparator comparator = DateTimeComparator.getInstance(DateTimeFieldType.minuteOfHour());
        DateTime startTimeForFullIndex = elasticSearchIndexer.startTimeForFullIndex();
        Assert.assertEquals(0, comparator.compare(startTimeForFullIndex, NOW.minusDays(MAX_AGE_DAYS)));
    }

}