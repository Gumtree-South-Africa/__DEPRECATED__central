package com.ecg.messagebox.resources;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.ConversationMetadata;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.MessageNotification;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.app.preprocessorchain.preprocessors.UniqueConversationSecret;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ComponentScan("com.ecg.messagebox.resources")
@TestPropertySource(properties = "spring.cloud.consul.enabled = false")
@ContextConfiguration(classes = AbstractTest.Configuration.class)
public abstract class AbstractTest {

    static final String USER_ID = "USER_ID";
    static final String USER_BUYER_ID = "USER_BUYER_ID";
    static final String USER_SELLER_ID = "USER_SELLER_ID";
    static final String CONVERSATION_ID = "CONVERSATION_ID";
    static final String MESSAGE_TEXT = "MESSAGE TEXT";
    static final String AD_ID = "AD_ID";
    static final String CUSTOM_DATA = "CUSTOM_DATA";

    @Autowired
    protected PostBoxService postBoxService;

    @Autowired
    protected ResponseDataRepository responseDataRepository;

    @Before
    public void setUp() {
        Mockito.reset(postBoxService, responseDataRepository);
    }

    public static class Configuration {

        @Bean
        PostBoxService postBoxService() {
            return mock(PostBoxService.class);
        }

        @Bean
        ResponseDataRepository responseDataService() {
            return mock(ResponseDataRepository.class);
        }

        @Bean
        ProcessingContextFactory processingContextFactory() {
            return mock(ProcessingContextFactory.class);
        }

        @Bean
        AnonymizedMailConverter anonymizedMailConverter() {
            return mock(AnonymizedMailConverter.class);
        }

        @Bean
        MessageProcessingCoordinator messageProcessingCoordinator() {
            return mock(MessageProcessingCoordinator.class);
        }

        @Bean
        MutableConversationRepository mutableConversationRepository() {
            return mock(MutableConversationRepository.class);
        }

        @Bean
        UserIdentifierService userIdentifierService() {
            return mock(UserIdentifierService.class);
        }

        @Bean
        UniqueConversationSecret uniqueConversationSecret() {
            return mock(UniqueConversationSecret.class);
        }
    }

    static ConversationThread conversationThread() {
        ConversationMetadata metadata = new ConversationMetadata(DateTime.now(), "SUBJECT", "TITLE", "IMAGE");
        Message message = new Message(UUIDs.timeBased(), MESSAGE_TEXT, USER_BUYER_ID, MessageType.CHAT);

        Participant buyer = new Participant(USER_BUYER_ID, "BUYER_NAME", "BUYER_EMAIL", ParticipantRole.BUYER);
        Participant seller = new Participant(USER_SELLER_ID, "SELLER_NAME", "SELLER_EMAIL", ParticipantRole.SELLER);

        ConversationThread conversation = new ConversationThread(CONVERSATION_ID, AD_ID, USER_BUYER_ID, Visibility.ACTIVE, MessageNotification.RECEIVE, Arrays.asList(buyer, seller), message, metadata);
        conversation.addNumUnreadMessages(USER_BUYER_ID, 0);
        conversation.addNumUnreadMessages(USER_SELLER_ID, 0);
        conversation.addMessages(Collections.singletonList(message));
        return conversation;
    }
}
