package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIndex;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("UnnecessaryLocalVariable")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CassandraCleanupConversationCronJobTest.TestContext.class)
@TestPropertySource(properties = {
        "replyts.maxConversationAgeDays = 15",
        "replyts.cleanup.conversation.streaming.queue.size = 1",
        "replyts.cleanup.conversation.streaming.threadcount = 1",
        "replyts.cleanup.conversation.streaming.batch.size = 1",
        "replyts.cleanup.conversation.schedule.expression = 0 0/30 * * * ? *"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CassandraCleanupConversationCronJobTest {

    private static final byte[] EMPTY = new byte[]{};
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final String CONVERSATION_ID1 = "conversationId1";
    private static final String FILENAME = "foo.bar";
    private static final String JOB_NAME = "cleanupConversationJob";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    private CassandraCleanupConversationCronJob cronJob;

    @Autowired
    private KafkaSinkService messageidSink;

    private static Stream<ConversationEventIndex> streamOfConversations(DateTime dateToBeProcessed) {
        ConversationEventIndex conversationEventIdx = new ConversationEventIndex(dateToBeProcessed, CONVERSATION_ID1);
        List<ConversationEventIndex> conversationEventIdxList = Collections.singletonList(conversationEventIdx);
        return conversationEventIdxList.stream();
    }

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsBeforeCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);

        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(streamOfConversations(dateToBeProcessed));

        // lastModifiedDate is before the date to be processed, so the conversation can be removed
        DateTime lastModifiedDate = now().minusDays(MAX_CONVERSATION_AGE_DAYS + 1);
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate.getMillis());

        MutableConversation conversation = mock(MutableConversation.class);
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(conversation);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteConversationEventIdxWhenLastModifiedDateIsAfterCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);

        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(streamOfConversations(dateToBeProcessed));

        // last modified date is after the date to be processed, so the conversation cannot be removed
        DateTime lastModifiedDate = now();
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate.getMillis());

        cronJob.execute();

        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsEqualCleanup() throws Exception {
        // last processed date exists and before the date to be processed
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(streamOfConversations(dateToBeProcessed));

        // last modified date is the same as the date to be processed, so conversation should be removed
        DateTime lastModifiedDate = dateToBeProcessed;
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate.getMillis());

        MutableConversation conversation = mock(MutableConversation.class);
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(conversation);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldDeleteIndexWhenLastModifiedDateIsEqualCleanupButConversationIsDeleted() throws Exception {
        // last processed date exists and before the date to be processed
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);

        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(streamOfConversations(dateToBeProcessed));

        // last modified date is the same as the date to be processed, so conversation should be removed
        DateTime lastModifiedDate = dateToBeProcessed;
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate.getMillis());
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(null);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversationRepository).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldReturnWhenNoCleanupDate() throws Exception {
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(null);

        cronJob.execute();

        verify(conversationRepository, never()).streamConversationEventIndexesByHour(any(DateTime.class));
        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository, never()).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldManageInvalidConversations() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME)).thenReturn(dateToBeProcessed);

        MutableConversation conversation = mock(MutableConversation.class);

        List<String> filenames = Collections.singletonList(FILENAME);
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(CONVERSATION_ID1);
        when(message.getAttachmentFilenames()).thenReturn(filenames);
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        when(conversation.getMessages()).thenReturn(messages);
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(conversation);
        when(conversationRepository.streamConversationEventIndexesByHour(dateToBeProcessed)).thenReturn(streamOfConversations(dateToBeProcessed));

        cronJob.execute();

        verify(messageidSink, times(1)).store(null, EMPTY);
    }

    @Configuration
    static class TestContext extends CassandreConversationCleanupTestContext {

        @Bean(name = "messageidSink")
        public KafkaSinkService msgidKafkaSinkService() {
            return mock(KafkaSinkService.class);
        }

        @Bean(name = "attachmentSink")
        public KafkaSinkService attachmentSink() {
            return mock(KafkaSinkService.class);
        }

        @Bean
        public SwiftAttachmentRepository swiftAttachmentRepository() {
            return mock(SwiftAttachmentRepository.class);
        }

        @Bean
        public AttachmentRepository attachmentRepository() {
            return mock(AttachmentRepository.class);
        }
    }
}