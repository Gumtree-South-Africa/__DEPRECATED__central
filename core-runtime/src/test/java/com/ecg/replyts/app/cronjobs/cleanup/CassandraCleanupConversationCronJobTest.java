package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
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
        "cronjob.cleanup.conversation.readFromNewIndexTable = false",
        "replyts.cleanup.conversation.schedule.expression = 0 0 0 * * ? *"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CassandraCleanupConversationCronJobTest {

    private static final byte[] EMPTY = new byte[]{};
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final String CONVERSATION_ID1 = "conversationId1";
    private static final String FILENAME = "foo.bar";
    private static final UUID EVENT_ID1 = UUID.randomUUID();
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

    @Autowired
    @Qualifier("attachmentSink")
    private KafkaSinkService attachmentSink;

    @Test
    public void shouldDeleteConversationWhenLastModifiedDateIsBeforeCleanup() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

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
        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

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
        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

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
        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

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

        verify(conversationRepository, never()).streamConversationEventIdxsByHour(any(DateTime.class));
        verify(conversationRepository, never()).getById(CONVERSATION_ID1);
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository, never()).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }

    @Test
    public void shouldReadFromTheOldIndexTable() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME)).thenReturn(dateToBeProcessed);

        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();

        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

        cronJob.execute();

        verify(conversationRepository, times(1)).streamConversationEventIdxsByHour(dateToBeProcessed);
        verify(conversationRepository, times(0)).streamConversationEventIndexesByHour(dateToBeProcessed);
    }

    @Test
    public void shouldManageInvalidConversations() throws Exception {
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).hourOfDay().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME)).thenReturn(dateToBeProcessed);

        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, EVENT_ID1);
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();

        MutableConversation conversation = mock(MutableConversation.class);

        List<String> filenames = Arrays.asList(FILENAME);
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(CONVERSATION_ID1);
        when(message.getAttachmentFilenames()).thenReturn(filenames);
        ArrayList<Message> messages = new ArrayList<>();
        messages.add(message);
        when(conversation.getMessages()).thenReturn(messages);
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(conversation);
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

        cronJob.execute();

        verify(messageidSink, times(1)).store(null, EMPTY);
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

        @Bean(name="messageidSink")
        public KafkaSinkService msgidKafkaSinkService() {
            return mock(KafkaSinkService.class);
        }

        @Bean(name="attachmentSink")
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

        @Bean
        public PropertySourcesPlaceholderConfigurer configurer() {
            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

            configurer.setNullValue("null");

            return configurer;
        }
    }
}