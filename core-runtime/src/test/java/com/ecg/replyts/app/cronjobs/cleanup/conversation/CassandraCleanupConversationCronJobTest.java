package com.ecg.replyts.app.cronjobs.cleanup.conversation;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.model.conversation.ConversationModificationDate;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraCleanupConversationCronJobTest {

    private static final int WORK_QUEUE_SIZE = 1;
    private static final int THREAD_COUNT = 1;
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final int BATCH_SIZE = 1;
    private static final String CRON_JOB_EXPRESSION = "test";
    private static final String CONVERSATION_ID1 = "conversationId1";

    @Mock
    private CassandraConversationRepository conversationRepository;
    @Mock
    private CronJobClockRepository cronJobClockRepository;
    @Mock
    private ConversationEventListeners conversationEventListeners;
    @Mock
    private CleanupDateCalculator cleanupDateCalculator;

    @InjectMocks
    private CassandraCleanupConversationCronJob testObject = new CassandraCleanupConversationCronJob(WORK_QUEUE_SIZE, THREAD_COUNT,
            MAX_CONVERSATION_AGE_DAYS, BATCH_SIZE, CRON_JOB_EXPRESSION) {

        @Override
        protected CleanupDateCalculator createCleanupDateCalculator() {
            return cleanupDateCalculator;
        }
    };

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsBeforeCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS);
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed);
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // lastModifiedDate is before the date to be processed, so the conversation can be removed
        DateTime lastModifiedDate = now().minusDays(MAX_CONVERSATION_AGE_DAYS + 1);
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        testObject.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteModificationIdxWhenLastModifiedDateIsAfterCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS);
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed);
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // last modified date is after the date to be processed, so the conversation cannot be removed
        DateTime lastModifiedDate = now();
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        testObject.execute();

        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsEqualCleanup() throws Exception {
        // last processed date exists and before the date to be processed
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS);
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationModificationDate modificationDate = new ConversationModificationDate(CONVERSATION_ID1, dateToBeProcessed);
        List<ConversationModificationDate> modificationDates = Arrays.asList(modificationDate);
        Stream<ConversationModificationDate> modificationDatesStream = modificationDates.stream();
        when(conversationRepository.streamConversationModificationsByDay(dateToBeProcessed.getYear(),
                dateToBeProcessed.getMonthOfYear(), dateToBeProcessed.getDayOfMonth())).thenReturn(modificationDatesStream);

        // last modified date is the same as the date to be processed, so conversation should be removed
        DateTime lastModifiedDate = dateToBeProcessed;
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate);

        testObject.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(modificationDate);
        verify(cronJobClockRepository).set(eq(CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldReturnWhenNoCleanupDate() throws Exception {
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME))
                .thenReturn(null);

        testObject.execute();

        verify(conversationRepository, never()).streamConversationModificationsByDay(anyInt(), anyInt(), anyInt());
        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteOldConversationModificationDate(any(ConversationModificationDate.class));
        verify(cronJobClockRepository, never()).set(eq(CassandraCleanupConversationCronJob.CLEANUP_CONVERSATION_JOB_NAME), any(DateTime.class), any(DateTime.class));
    }
}
