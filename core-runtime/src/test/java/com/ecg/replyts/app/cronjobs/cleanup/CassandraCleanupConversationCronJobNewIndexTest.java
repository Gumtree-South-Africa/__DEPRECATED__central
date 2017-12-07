package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIndex;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CassandraCleanupConversationCronJobNewIndexTest.TestContext.class)
@TestPropertySource(properties = {
  "replyts.maxConversationAgeDays = 15",
  "replyts.cleanup.conversation.streaming.queue.size = 1",
  "replyts.cleanup.conversation.streaming.threadcount = 1",
  "replyts.cleanup.conversation.streaming.batch.size = 1",
  "replyts.cleanup.conversation.readFromNewIndexTable = true",
  "replyts.cleanup.conversation.schedule.expression = 0 0 0 * * ? *"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CassandraCleanupConversationCronJobNewIndexTest {
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final String CONVERSATION_ID1 = "conversationId1";
    private static final String JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    private CassandraCleanupConversationCronJob cronJob;

    @Test
    public void shouldReadFromTheCorrectIndexTable() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME)).thenReturn(dateToBeProcessed);

        ConversationEventIndex conversationEventIndex = new ConversationEventIndex(dateToBeProcessed, CONVERSATION_ID1);
        List<ConversationEventIndex> conversationEventIndexList = Arrays.asList(conversationEventIndex);
        Stream<ConversationEventIndex> conversationEventIndexesStream = conversationEventIndexList.stream();

        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(conversationEventIndexesStream);

        cronJob.execute();

        verify(conversationRepository, times(0)).streamConversationEventIdxsByHour(dateToBeProcessed);
        verify(conversationRepository, times(1)).streamConversationEventIndexesByHour(dateToBeProcessed);
    }


    @Configuration
    static class TestContext {
        @Bean
        public CassandraConversationRepository conversationRepository() {
            return mock(CassandraConversationRepository.class);
        }

        @Bean
        public CronJobClockRepository cronJobClockRepository() {
            return mock(CronJobClockRepository.class);
        }

        @Bean
        public ConversationEventListeners conversationEventListeners() {
            return mock(ConversationEventListeners.class);
        }

        @Bean
        public CleanupDateCalculator cleanupDateCalculator() {
            return mock(CleanupDateCalculator.class);
        }

        @Bean
        public CassandraCleanupConversationCronJob cleanupCronJob() {
            return new CassandraCleanupConversationCronJob();
        }

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}