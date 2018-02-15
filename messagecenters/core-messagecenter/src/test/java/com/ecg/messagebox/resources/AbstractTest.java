package com.ecg.messagebox.resources;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.messagebox.model.*;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.service.ResponseDataService;
import org.joda.time.DateTime;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

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

    public static class Configuration {

        @Bean
        PostBoxService postBoxService() {
            return mock(PostBoxService.class);
        }

        @Bean
        ResponseDataService responseDataService() {
            return mock(ResponseDataService.class);
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
        return conversation;
    }
}
