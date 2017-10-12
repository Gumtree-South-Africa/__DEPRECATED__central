package com.ecg.replyts.app.cronjobs.cleanup;

import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEventIdx;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.clock.CronJobClockRepository;
import com.ecg.replyts.core.runtime.persistence.conversation.CassandraConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaSinkService;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = CassandraCleanupConversationCronJobMsgidOnTest.TestContext.class)
@TestPropertySource(properties = {
        "replyts.maxConversationAgeDays = 15",
        "replyts.cleanup.conversation.streaming.queue.size = 1",
        "replyts.cleanup.conversation.streaming.threadcount = 1",
        "replyts.cleanup.conversation.streaming.batch.size = 1",
        "replyts.cleanup.conversation.schedule.expression = 0 0 0 * * ? *"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CassandraCleanupConversationCronJobMsgidOnTest {
    private static final int MAX_CONVERSATION_AGE_DAYS = 15;
    private static final String CONVERSATION_ID1 = "conversationId1";
    private static final String JOB_NAME = "cleanupConversationJob";
    private static final String FILENAME = "att1 sdf sdfl;  sdf: & %$ /\\ ! ±§§ + \t\n ";

    @Autowired
    private CassandraConversationRepository conversationRepository;

    @Autowired
    private CronJobClockRepository cronJobClockRepository;

    @Autowired
    private CleanupDateCalculator cleanupDateCalculator;

    @Autowired
    @Qualifier("messageidSink")
    private KafkaSinkService msgidKafkaSinkService;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private CassandraCleanupConversationCronJob cronJob;

    @Test
    public void whenDeleteConversationAlsoPersistMgsIds() throws Exception {
        // last processed date exists and before the date to be processed
        DateTime dateToBeProcessed = now().minusDays(MAX_CONVERSATION_AGE_DAYS).dayOfMonth().roundFloorCopy().toDateTime();
        when(cleanupDateCalculator.getCleanupDate(MAX_CONVERSATION_AGE_DAYS, JOB_NAME))
                .thenReturn(dateToBeProcessed);
        ConversationEventIdx conversationEventIdx = new ConversationEventIdx(dateToBeProcessed, CONVERSATION_ID1, UUID.randomUUID());
        List<ConversationEventIdx> conversationEventIdxList = Arrays.asList(conversationEventIdx);
        Stream<ConversationEventIdx> conversationEventIdxStream = conversationEventIdxList.stream();
        when(conversationRepository.streamConversationEventIdxsByHour(dateToBeProcessed)).thenReturn(conversationEventIdxStream);

        // last modified date is the same as the date to be processed, so conversation should be removed
        DateTime lastModifiedDate = dateToBeProcessed;
        when(conversationRepository.getLastModifiedDate(CONVERSATION_ID1)).thenReturn(lastModifiedDate.getMillis());

        MutableConversation conversation = buildConversation(); //mock(MutableConversation.class);
        when(conversationRepository.getById(CONVERSATION_ID1)).thenReturn(conversation);

        cronJob.execute();

        verify(conversationRepository).getById(CONVERSATION_ID1);
        verify(conversation).getMessages();
        verify(attachmentRepository).getCompositeKey("msg1", "att1");
        verify(attachmentRepository).getCompositeKey("msg1", "att2");
        verify(attachmentRepository).getCompositeKey("msg2", FILENAME);
        verify(attachmentRepository, never()).getCompositeKey("msg3", "");
        verify(msgidKafkaSinkService, times(3)).store(anyString(), any());
        verify(conversationRepository, never()).deleteConversationModificationIdxs(CONVERSATION_ID1);
        verify(cronJobClockRepository).set(eq(JOB_NAME), any(DateTime.class), any(DateTime.class));
    }


    public MutableConversation buildConversation() {
        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn(CONVERSATION_ID1);

        MutableConversation mutableConversation = Mockito.mock(MutableConversation.class);
        when(mutableConversation.getImmutableConversation()).thenReturn(conversation);

        List<Message> msgs = mockMessage();
        when(mutableConversation.getMessages()).thenReturn(msgs);
        return mutableConversation;
    }

    public static List<Message> mockMessage() {
        Message message1 = Mockito.mock(Message.class);
        Message message2 = Mockito.mock(Message.class);
        Message message3 = Mockito.mock(Message.class);
        when(message1.getId()).thenReturn("msg1");
        when(message1.getAttachmentFilenames()).thenReturn(Lists.newArrayList("att1", "att2"));
        when(message2.getId()).thenReturn("msg2");
        when(message2.getAttachmentFilenames()).thenReturn(Lists.newArrayList(FILENAME));
        when(message3.getId()).thenReturn("msg3");
        when(message3.getAttachmentFilenames()).thenReturn(Lists.newArrayList());

        return Lists.newArrayList(message1, message2, message3);
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
        public SwiftAttachmentRepository swiftAttachmentRepository() {
            return mock(SwiftAttachmentRepository.class);
        }

        @Bean(name="messageidSink")
        public KafkaSinkService msgidKafkaSinkService() {
            return mock(KafkaSinkService.class);
        }


        @Bean(name="attachmentSink")
        public KafkaSinkService attachmentKafkaSinkService() {
            return mock(KafkaSinkService.class);
        }

        @Bean
        public AttachmentRepository attachmentRepository() {
            return mock(AttachmentRepository.class);
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