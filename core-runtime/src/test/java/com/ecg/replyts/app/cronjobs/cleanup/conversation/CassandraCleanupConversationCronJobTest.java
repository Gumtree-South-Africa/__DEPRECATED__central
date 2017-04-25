package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
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
@ContextConfiguration(classes = CassandraCleanupConversationCronJobTest.TestContext.class)
@TestPropertySource(properties = {
  "replyts.maxConversationAgeDays = 15",
  "replyts.cleanup.conversation.streaming.queue.size = 1",
  "replyts.cleanup.conversation.streaming.threadcount = 1",
  "replyts.cleanup.conversation.streaming.batch.size = 1",
  "replyts.cleanup.conversation.schedule.expression = 0 0 0 * * ? *"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CassandraCleanupConversationCronJobTest {
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final String CONVERSATION_ID1 = "conversationId1";
    private static final String JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    private CassandraCleanupConversationCronJob cronJob;

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsBeforeCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed, dateToBeProcessed.getMillis());
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // lastModifiedDate is before the date to be processed, so the conversation can be removed
        DateTime lastModifiedDate = now().minusDays(MAX_CONVERSATION_AGE_DAYS + 1);
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteModificationIdxWhenLastModifiedDateIsAfterCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed, dateToBeProcessed.getMillis());
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // last modified date is after the date to be processed, so the conversation cannot be removed
        DateTime lastModifiedDate = now();
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        cronJob.execute();

        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsEqualCleanup() throws Exception {
        // last processed date exists and before the date to be processed
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed, dateToBeProcessed.getMillis());
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // last modified date is the same as the date to be processed, so conversation should be removed
        DateTime lastModifiedDate = dateToBeProcessed;
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldReturnWhenNoCleanupDate() throws Exception {
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(null);

        cronJob.execute();

        verify(conversationRepository, never()).streamConversationModificationsByDay(anyInt(), anyInt(), anyInt());
        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(any(ConversationModificationDate.class));
        verify(cronJobClockRepository, never()).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
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