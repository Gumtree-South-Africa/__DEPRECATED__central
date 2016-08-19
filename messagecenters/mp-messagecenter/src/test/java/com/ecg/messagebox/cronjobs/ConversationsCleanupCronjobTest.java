package com.ecg.messagebox.cronjobs;

import com.ecg.messagebox.model.ConversationModification;
import com.ecg.messagebox.persistence.CassandraPostBoxRepository;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

public class ConversationsCleanupCronjobTest {

    private static final int WORK_QUEUE_SIZE = 1;
    private static final int THREAD_COUNT = 1;
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final int BATCH_SIZE = 1;
    private static final String CONVERSATIONS_CLEANUP_CRONJOB_NAME = "conversations_cleanup_cronjob";
    private static final String CRONJOB_EXPRESSION = "0 0 0 * * ? *";
    private DateTime deleteEverythingBefore;
    private DateTime dateToProcess;
    private DateTime lastProcessedDate;

    private CassandraPostBoxRepository repository = mock(CassandraPostBoxRepository.class);

    private ConversationsCleanupCronJob cronJob = new ConversationsCleanupCronJob(repository, WORK_QUEUE_SIZE, THREAD_COUNT, MAX_CONVERSATION_AGE_DAYS, BATCH_SIZE, CRONJOB_EXPRESSION);

    private ConversationModification conv1 = new ConversationModification("u1", "c1", null, timeBased(), new DateTime());
    private ConversationModification conv2 = new ConversationModification("u2", "c2", null, timeBased(), new DateTime());
    private ConversationModification conv3 = new ConversationModification("u3", "c3", null, timeBased(), new DateTime());

    @Before
    public void setDates(){
        DateTimeUtils.setCurrentMillisFixed(now().getMillis());
        deleteEverythingBefore = now().minusDays(MAX_CONVERSATION_AGE_DAYS);
        dateToProcess = deleteEverythingBefore.minusHours(2);
        lastProcessedDate = dateToProcess.minusHours(1);
    }

    @Test
    public void shouldDeleteConversationsWhenLastModifiedBeforeCleanupDate() throws Exception{

        when(repository.getCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1, conv2));
        when(repository.getConversationModificationsByHour(dateToProcess.plusHours(1))).thenReturn(Stream.empty());
        when(repository.getLastConversationModification("u1", "c1")).thenReturn(new ConversationModification("u1", "c1", "a1", timeBased(), deleteEverythingBefore.minusHours(5)));
        when(repository.getLastConversationModification("u2", "c2")).thenReturn(new ConversationModification("u2", "c2", "a2", timeBased(), deleteEverythingBefore.minusHours(5)));

        cronJob.execute();

        verify(repository).deleteConversation(conv1.getUserId(), "a1", conv1.getConversationId());
        verify(repository).deleteConversation(conv2.getUserId(), "a2", conv2.getConversationId());
        verify(repository).setCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess);
        verify(repository).setCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME, dateToProcess.plusHours(1));
    }

    @Test
    public void shouldOnlyDeleteModificationIndexWhenConversationWasLastModifiedAfterCleanupDate() throws Exception{
        when(repository.getCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1));
        when(repository.getConversationModificationsByHour(dateToProcess.plusHours(1))).thenReturn(Stream.of(conv2, conv3));
        when(repository.getLastConversationModification("u1", "c1")).thenReturn(new ConversationModification("u1", "c1", "a1", timeBased(), deleteEverythingBefore.minusHours(5)));
        when(repository.getLastConversationModification("u2", "c2")).thenReturn(new ConversationModification("u2", "c2", "a2", timeBased(), deleteEverythingBefore.plusHours(5)));
        when(repository.getLastConversationModification("u3", "c3")).thenReturn(new ConversationModification("u3", "c3", "a3", timeBased(), deleteEverythingBefore.minusHours(5)));

        cronJob.execute();

        verify(repository).deleteConversation(conv1.getUserId(), "a1", conv1.getConversationId());
        verify(repository).deleteModificationIndexByDate(conv2.getModifiedAt(), conv2.getMessageId(), conv2.getUserId(), conv2.getConversationId());
        verify(repository).deleteConversation(conv3.getUserId(), "a3", conv3.getConversationId());
    }

    @Test
    public void shouldNotUpdateProcessedDateWhenFailure() throws Exception {
        when(repository.getCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME)).thenReturn(lastProcessedDate);
        when(repository.getConversationModificationsByHour(dateToProcess)).thenReturn(Stream.of(conv1));
        when(repository.getLastConversationModification("u1", "c1")).thenThrow(new RuntimeException("TEST"));

        cronJob.execute();

        verify(repository).getCronjobLastProcessedDate(CONVERSATIONS_CLEANUP_CRONJOB_NAME);
        verify(repository).getConversationModificationsByHour(dateToProcess);
        verify(repository).getLastConversationModification("u1", "c1");
        verifyNoMoreInteractions(repository);
    }

}
