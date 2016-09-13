package com.ecg.messagebox.cronjobs;

import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Stream;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static com.ecg.messagebox.cronjobs.ConversationsCleanupCronJob.CONVERSATIONS_CLEANUP_CRONJOB_NAME;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

public class ConversationsCleanupCronjobTest {

    private static final int WORK_QUEUE_SIZE = 1;
    private static final int THREAD_COUNT = 1;
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final int BATCH_SIZE = 1;
    private static final String CRONJOB_EXPRESSION = "0 0 0 * * ? *";
    private DateTime deleteEverythingBefore;
    private DateTime dateToProcess;
    private DateTime lastProcessedDate;

    private CassandraPostBoxRepository repository = mock(CassandraPostBoxRepository.class);
    private CronJobClockRepository cronJobClockRepository = mock(CronJobClockRepository.class);

    private ConversationsCleanupCronJob cronJob = new ConversationsCleanupCronJob(repository, cronJobClockRepository, true, WORK_QUEUE_SIZE, THREAD_COUNT, MAX_CONVERSATION_AGE_DAYS, BATCH_SIZE, CRONJOB_EXPRESSION);

    private ConversationModification conv1 = new ConversationModification("u1", "c1", null, timeBased(), new DateTime());
    private ConversationModification conv2 = new ConversationModification("u2", "c2", null, timeBased(), new DateTime());
    private ConversationModification conv3 = new ConversationModification("u3", "c3", null, timeBased(), new DateTime());

    @Before
    public void setup(){
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        deleteEverythingBefore = now().minusDays(MAX_CONVERSATION_AGE_DAYS);
        dateToProcess = deleteEverythingBefore.minusHours(2);
        lastProcessedDate = dateToProcess.minusHours(1);
    }

    @After
    public void tearDown() throws Exception {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void shouldDeleteConversationsWhenLastModifiedBeforeCleanupDate() throws Exception{
        when(cronJobClockRepository.getLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1, conv2));
        when(repository.getConversationModificationsByHour(dateToProcess.plusHours(1))).thenReturn(Stream.empty());
        when(repository.getLastConversationModification("u1", "c1")).thenReturn(new ConversationModification("u1", "c1", "a1", timeBased(), deleteEverythingBefore.minusHours(5)));
        when(repository.getLastConversationModification("u2", "c2")).thenReturn(new ConversationModification("u2", "c2", "a2", timeBased(), deleteEverythingBefore.minusHours(5)));

        cronJob.execute();

        verify(repository).deleteConversations(conv1.getUserId(), Collections.singletonMap("a1", conv1.getConversationId()));
        verify(repository).deleteConversations(conv2.getUserId(), Collections.singletonMap("a2", conv2.getConversationId()));
        verify(cronJobClockRepository).set(CONVERSATIONS_CLEANUP_CRONJOB_NAME, now(), dateToProcess);
        verify(cronJobClockRepository).set(CONVERSATIONS_CLEANUP_CRONJOB_NAME, now(), dateToProcess.plusHours(1));
    }

    @Test
    public void shouldOnlyDeleteModificationIndexWhenConversationWasLastModifiedAfterCleanupDate() throws Exception{
        when(cronJobClockRepository.getLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1));
        when(repository.getConversationModificationsByHour(dateToProcess.plusHours(1))).thenReturn(Stream.of(conv2, conv3));
        when(repository.getLastConversationModification("u1", "c1")).thenReturn(new ConversationModification("u1", "c1", "a1", timeBased(), deleteEverythingBefore.minusHours(5)));
        when(repository.getLastConversationModification("u2", "c2")).thenReturn(new ConversationModification("u2", "c2", "a2", timeBased(), deleteEverythingBefore.plusHours(5)));
        when(repository.getLastConversationModification("u3", "c3")).thenReturn(new ConversationModification("u3", "c3", "a3", timeBased(), deleteEverythingBefore.minusHours(5)));

        cronJob.execute();

        verify(repository).deleteConversations(conv1.getUserId(), Collections.singletonMap("a1", conv1.getConversationId()));
        verify(repository).deleteModificationIndexByDate(conv2.getModifiedAt(), conv2.getMessageId(), conv2.getUserId(), conv2.getConversationId());
        verify(repository).deleteConversations(conv3.getUserId(), Collections.singletonMap("a3", conv3.getConversationId()));
    }

    @Test
    public void shouldNotUpdateProcessedDateWhenFailure() throws Exception {
        when(cronJobClockRepository.getLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1));
        when(repository.getLastConversationModification("u1", "c1")).thenThrow(new RuntimeException("EXPECTED EXCEPTION"));

        cronJob.execute();

        verify(cronJobClockRepository).getLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME);
        verify(repository).getConversationModificationsByHour(dateToProcess);
        verify(repository).getLastConversationModification("u1", "c1");
        verifyNoMoreInteractions(repository);
    }

}
