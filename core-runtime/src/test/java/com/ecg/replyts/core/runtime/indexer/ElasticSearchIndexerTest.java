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
import java.util.stream.Collectors;

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

    private final List<String> CONV_IDS = Lists.newArrayList("foo1", "foo2", "foo3");
    private final int MAX_AGE_DAYS = 180;
    private final DateTime NOW = DateTime.now();

    @Before
    public void setup() {
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(10);
        RejectedExecutionHandler rejectionHandler = new InstrumentedCallerRunsPolicy("indexer", ElasticSearchIndexer.class.getSimpleName());
        executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workQueue, rejectionHandler);

        ReflectionTestUtils.setField(elasticSearchIndexer, "conversationIdBatchSize", 3);
        ReflectionTestUtils.setField(elasticSearchIndexer, "threadPoolExecutor", executorService);
        ReflectionTestUtils.setField(elasticSearchIndexer, "taskCompletionTimeoutSec", 10);
        ReflectionTestUtils.setField(elasticSearchIndexer, "maxAgeDays", MAX_AGE_DAYS);
        ReflectionTestUtils.setField(elasticSearchIndexer, "conversationIdBatchSize", 10);
        ReflectionTestUtils.setField(elasticSearchIndexer, "clock", new CurrentClock());

        ReflectionTestUtils.setField(conversation2Kafka, "fetchedConvCounter", new AtomicLong(0));

        List<Conversation> conversations = Lists.newArrayList();
        for (String conversationId : CONV_IDS) {
            MutableConversation mutableConversation = mock(MutableConversation.class);

            conversations.add(mutableConversation);

            when(conversationRepository.getById(conversationId)).thenReturn(mutableConversation);
        }
    }

    @Test
    public void indexSince() {
        elasticSearchIndexer.indexConversations(CONV_IDS.stream());
        verify(conversation2Kafka).indexChunk(CONV_IDS.stream().collect(Collectors.toSet()));
    }

    @Test
    public void indexBetween() {
        DateTime FROM = DateTime.now().minusDays(1);

        when(conversationRepository.streamConversationsModifiedBetween(FROM, NOW)).thenReturn(CONV_IDS.stream());
        elasticSearchIndexer.doIndexBetween(FROM, NOW);

        verify(conversation2Kafka).indexChunk(CONV_IDS.stream().collect(Collectors.toSet()));
    }

    @Test
    public void fullReindex() {
        when(conversationRepository.streamConversationsModifiedBetween(any(DateTime.class), any(DateTime.class))).thenReturn(CONV_IDS.stream());
        elasticSearchIndexer.fullIndex();

        verify(conversation2Kafka).indexChunk(CONV_IDS.stream().collect(Collectors.toSet()));
    }

    @Test
    public void startTimeForFullReindex() {
        DateTimeComparator comparator = DateTimeComparator.getInstance(DateTimeFieldType.minuteOfHour());
        DateTime startTimeForFullIndex = elasticSearchIndexer.startTimeForFullIndex();
        Assert.assertEquals(0, comparator.compare(startTimeForFullIndex, NOW.minusDays(MAX_AGE_DAYS)));
    }

}