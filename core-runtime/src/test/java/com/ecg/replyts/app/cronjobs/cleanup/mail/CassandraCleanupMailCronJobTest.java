package com.ecg.replyts.app.cronjobs.cleanup.mail;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.cronjobs.cleanup.CleanupDateCalculator;
import com.ecg.replyts.core.api.model.mail.MailCreationDate;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.mail.CassandraMailRepository;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraCleanupMailCronJobTest {

    private static final int WORK_QUEUE_SIZE = 1;
    private static final int THREAD_COUNT = 1;
    private static final int MAX_AGE_DAYS = 15;
    private static final int BATCH_SIZE = 1;
    private static final String CRON_JOB_EXPRESSION = "test";
    private static final String MAIL_ID1 = "conversationId1";

    @Mock
    private CassandraMailRepository mailRepository;
    @Mock
    private CronJobClockRepository cronJobClockRepository;
    @Mock
    private ConversationEventListeners conversationEventListeners;
    @Mock
    private CleanupDateCalculator cleanupDateCalculator;

    @InjectMocks
    private CassandraCleanupMailCronJob testObject = new CassandraCleanupMailCronJob(WORK_QUEUE_SIZE, THREAD_COUNT,
            MAX_AGE_DAYS, BATCH_SIZE, CRON_JOB_EXPRESSION) {

        @Override
        protected CleanupDateCalculator createCleanupDateCalculator() {
            return cleanupDateCalculator;
        }
    };

    @Test
    public void shouldDeleteMailForGivenCleanupDate() throws Exception {
        DateTime cleanupDate = now().minusDays(MAX_AGE_DAYS);
        when(cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME))
                .thenReturn(cleanupDate);
        MailCreationDate creationDate = new MailCreationDate(MAIL_ID1, cleanupDate);
        List<MailCreationDate> creationDates = Arrays.asList(creationDate);
        Stream<MailCreationDate> creationDatesStream = creationDates.stream();
        when(mailRepository.streamMailCreationDatesByDay(cleanupDate.getYear(),
                cleanupDate.getMonthOfYear(), cleanupDate.getDayOfMonth())).thenReturn(creationDatesStream);

        testObject.execute();

        verify(mailRepository).deleteMail(MAIL_ID1, cleanupDate);
        verify(cronJobClockRepository).set(eq(CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME), any(DateTime.class), eq(cleanupDate));
    }

    @Test
    public void shouldNotDeleteMailForNoCleanupDate() throws Exception {
        when(cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME))
                .thenReturn(null);

        testObject.execute();

        verify(mailRepository, never()).deleteMail(eq(MAIL_ID1), any(DateTime.class));
        verify(cronJobClockRepository, never()).set(eq(CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldCatchRuntimeExceptionWhileDeletingMail() throws Exception {
        DateTime cleanupDate = now().minusDays(MAX_AGE_DAYS);
        when(cleanupDateCalculator.getCleanupDate(MAX_AGE_DAYS, CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME))
                .thenReturn(cleanupDate);
        MailCreationDate creationDate = new MailCreationDate(MAIL_ID1, cleanupDate);
        List<MailCreationDate> creationDates = Arrays.asList(creationDate);
        Stream<MailCreationDate> creationDatesStream = creationDates.stream();
        when(mailRepository.streamMailCreationDatesByDay(cleanupDate.getYear(),
                cleanupDate.getMonthOfYear(), cleanupDate.getDayOfMonth())).thenReturn(creationDatesStream);

        doThrow(new RuntimeException("Expected exception")).when(mailRepository).deleteMail(MAIL_ID1);

        testObject.execute();

        verify(mailRepository).deleteMail(MAIL_ID1, cleanupDate);
        verify(cronJobClockRepository).set(eq(CassandraCleanupMailCronJob.CLEANUP_MAIL_JOB_NAME), any(DateTime.class), eq(cleanupDate));
    }
}
