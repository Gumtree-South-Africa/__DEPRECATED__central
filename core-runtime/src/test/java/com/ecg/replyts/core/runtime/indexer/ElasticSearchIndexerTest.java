package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.util.CurrentClock;
import com.ecg.replyts.core.runtime.workers.InstrumentedCallerRunsPolicy;
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

    private final int MAX_AGE_DAYS = 1;
    private final DateTime NOW = DateTime.now();
    private ExecutorService executorService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private Conversation2Kafka conversation2Kafka;
    @InjectMocks
    private ElasticSearchIndexer elasticSearchIndexer;

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
        ReflectionTestUtils.setField(elasticSearchIndexer, "maxRetriesOnFailure", 1);

        ReflectionTestUtils.setField(elasticSearchIndexer, "maxAgeDays", MAX_AGE_DAYS);
        ReflectionTestUtils.setField(elasticSearchIndexer, "convIdDedupBufferSize", 100);
        ReflectionTestUtils.setField(elasticSearchIndexer, "clock", new CurrentClock());

        ReflectionTestUtils.setField(conversation2Kafka, "submittedConvCounter", new AtomicLong(0));

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
        List<ElasticSearchIndexer.TimeIntervalPair> intervals = elasticSearchIndexer.getTimeIntervals(FROM, NOW);
        for (ElasticSearchIndexer.TimeIntervalPair interval : intervals) {
            when(conversationRepository.streamConversationsModifiedBetween(interval.startInterval, interval.endInterval)).thenReturn(CONV_IDS.stream());
        }

        elasticSearchIndexer.doIndexBetween(FROM, NOW);
        verify(conversation2Kafka, times(CONV_IDS.size() * intervals.size())).updateElasticSearch(anyString());
    }

    @Test
    public void testGetTimeIntervalPairs() {
        DateTime NOW = DateTime.now();
        DateTime dateTo = NOW;
        DateTime dateFrom = NOW.minusDays(5);

        List<ElasticSearchIndexer.TimeIntervalPair> timeIntervalPairs = elasticSearchIndexer.getTimeIntervals(dateFrom, dateTo);
        ElasticSearchIndexer.TimeIntervalPair firstPair = timeIntervalPairs.get(0);
        ElasticSearchIndexer.TimeIntervalPair lastPair = timeIntervalPairs.get(timeIntervalPairs.size() - 1);

        Assert.assertEquals(dateFrom, firstPair.startInterval);
        Assert.assertEquals(dateTo, lastPair.endInterval);

    }

    @Test
    public void startTimeForFullReindex() {
        DateTimeComparator comparator = DateTimeComparator.getInstance(DateTimeFieldType.minuteOfHour());
        DateTime startTimeForFullIndex = elasticSearchIndexer.startTimeForFullIndex();
        Assert.assertEquals(0, comparator.compare(startTimeForFullIndex, NOW.minusDays(MAX_AGE_DAYS)));
    }

}