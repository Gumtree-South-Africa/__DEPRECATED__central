package com.ecg.replyts.core.runtime.persistence.kafka;

import com.ecg.replyts.app.ContentOverridingPostProcessorService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MessageTransport;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageEventPublisherTest {

    private static final String AD_ID = "AD_ID";
    private static final String CONVERSATION_ID = "CONVERSATION_ID";
    public static final String MESSAGE_ID = "MESSAGE_ID";
    public static final String MESSAGE_CONTENT = "MESSAGE_CONTENT";

    @Mock
    private Conversation conversation;

    @Mock
    private Message message;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private BlockUserRepository blockUserRepository;

    @Mock
    private ConversationEventService conversationEventService;

    @Mock
    private UserIdentifierService userIdentifierService;

    @Mock
    private ContentOverridingPostProcessorService contentOverridingPostProcessorService;

    @Mock
    private AnonymizedMailConverter anonymizedMailConverter;

    private String shortTenant = "TENANT";

    private MessageEventPublisher publisher;

    @Before
    public void setUp() {
        publisher = new MessageEventPublisher(blockUserRepository, conversationEventService, userIdentifierService,
                contentOverridingPostProcessorService, anonymizedMailConverter, shortTenant);
    }

    @Test
    public void successfulSendWithConversationCreateEvent() throws InterruptedException {
        Map<String, String> customValues = new HashMap<>();
        DateTime createdAt = DateTime.now();
        DateTime receivedAt = DateTime.now();

        when(context.getTransport()).thenReturn(MessageTransport.CHAT);
        when(context.getOriginTenant()).thenReturn(shortTenant);
        when(conversation.getCustomValues()).thenReturn(customValues);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));
        when(conversation.getAdId()).thenReturn(AD_ID);
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(conversation.getCreatedAt()).thenReturn(createdAt);
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getReceivedAt()).thenReturn(receivedAt);
        when(contentOverridingPostProcessorService.getCleanedMessage(conversation, message)).thenReturn(MESSAGE_CONTENT);
        when(userIdentifierService.getBuyerUserId(customValues)).thenReturn(Optional.of("BUYER_ID"));
        when(userIdentifierService.getSellerUserId(customValues)).thenReturn(Optional.of("SELLER_ID"));

        publisher.publish(context, conversation, message);

        verify(conversationEventService).sendConversationCreatedEvent(eq(shortTenant), eq(AD_ID),
                eq(CONVERSATION_ID), eq(customValues), any(), eq(createdAt));

        verify(conversationEventService).sendMessageAddedEvent(eq(shortTenant), eq(CONVERSATION_ID), eq("SELLER_ID"), eq(MESSAGE_ID), eq(MESSAGE_CONTENT),
                eq(customValues), eq(MessageTransport.CHAT), eq(shortTenant), eq(receivedAt), eq(Arrays.asList("SELLER_ID", "BUYER_ID")));
    }

    @Test
    public void successfulSendToExistingConversation() throws InterruptedException {
        Map<String, String> customValues = new HashMap<>();
        DateTime createdAt = DateTime.now();
        DateTime receivedAt = DateTime.now();

        when(context.getTransport()).thenReturn(MessageTransport.CHAT);
        when(context.getOriginTenant()).thenReturn(shortTenant);
        when(conversation.getCustomValues()).thenReturn(customValues);
        when(conversation.getMessages()).thenReturn(Arrays.asList(message, message));
        when(conversation.getAdId()).thenReturn(AD_ID);
        when(conversation.getId()).thenReturn(CONVERSATION_ID);
        when(conversation.getCreatedAt()).thenReturn(createdAt);
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getId()).thenReturn(MESSAGE_ID);
        when(message.getReceivedAt()).thenReturn(receivedAt);
        when(contentOverridingPostProcessorService.getCleanedMessage(conversation, message)).thenReturn(MESSAGE_CONTENT);
        when(userIdentifierService.getBuyerUserId(customValues)).thenReturn(Optional.of("BUYER_ID"));
        when(userIdentifierService.getSellerUserId(customValues)).thenReturn(Optional.of("SELLER_ID"));

        publisher.publish(context, conversation, message);

        verify(conversationEventService).sendMessageAddedEvent(eq(shortTenant), eq(CONVERSATION_ID), eq("SELLER_ID"), eq(MESSAGE_ID), eq(MESSAGE_CONTENT),
                eq(customValues), eq(MessageTransport.CHAT), eq(shortTenant), eq(receivedAt), eq(Arrays.asList("SELLER_ID", "BUYER_ID")));

        verifyNoMoreInteractions(conversationEventService);
    }
}
