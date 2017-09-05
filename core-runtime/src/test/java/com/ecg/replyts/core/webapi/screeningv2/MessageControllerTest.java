package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.MessageModeratedCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.ModerateMessagePayload;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MessageControllerTest {
    static final String CONVERSATION_ID = "convId";
    static final String MESSAGE_ID = "msgId";
    static final String AD_ID = "1234";
    static final String BUYER_ID = "buyer";
    static final String SELLER_ID = "seller";
    static final String BUYER_SECRET = "buyerSecret";
    static final String SELLER_SECRET = "sellerSecret";
    static final String EDITOR_NAME = "editor";

    @Mock
    private ModerationService moderationService;
    @Mock
    private DomainObjectConverter domainObjectConverter;
    @Mock
    private SearchService searchService;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private OutdatedEntityMonitor entityMonitor;

    private MessageController messageController;
    private DateTime now;
    private ModerateMessagePayload positiveModeration;
    private MutableConversation conversation;

    @Before
    public void setUp() throws Exception {
        messageController = new MessageController(moderationService, domainObjectConverter, searchService, conversationRepository, entityMonitor);
        now = DateTime.now(DateTimeZone.UTC);
        positiveModeration = new ModerateMessagePayload();
        positiveModeration.setEditor(EDITOR_NAME);
        positiveModeration.setNewMessageState(ModerationResultState.GOOD);

        conversation = DefaultMutableConversation.create(new NewConversationCommand(
                CONVERSATION_ID, AD_ID, BUYER_ID, SELLER_ID, BUYER_SECRET, SELLER_SECRET, now, ConversationState.ACTIVE, new HashMap<>()
        ));
    }

    @Test
    public void noConversation_errorReturned() throws Exception {
        ResponseObject<?> response = messageController.changeMessageState(CONVERSATION_ID, MESSAGE_ID, positiveModeration);
        assertThat(response.getStatus().getState(), equalTo(RequestState.ENTITY_NOT_FOUND));

        verify(conversationRepository).getById(CONVERSATION_ID);
    }

    @Test
    public void noMessage_errorReturned() throws Exception {
        when(conversationRepository.getById(CONVERSATION_ID)).thenReturn(conversation);

        ResponseObject<?> response = messageController.changeMessageState(CONVERSATION_ID, MESSAGE_ID, positiveModeration);
        assertThat(response.getStatus().getState(), equalTo(RequestState.ENTITY_NOT_FOUND));

        verify(conversationRepository).getById(CONVERSATION_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void unacceptableUserDecision_exceptionThrown() throws Exception {
        when(conversationRepository.getById(CONVERSATION_ID)).thenReturn(conversation);
        conversation.applyCommand(new AddMessageCommand(
                CONVERSATION_ID, MESSAGE_ID, MessageState.HELD, MessageDirection.BUYER_TO_SELLER,
                now, "", "", ImmutableMap.of(), ImmutableList.of(), ImmutableList.of()));

        ModerateMessagePayload timedOutMessage = new ModerateMessagePayload();
        timedOutMessage.setEditor(EDITOR_NAME);
        timedOutMessage.setNewMessageState(ModerationResultState.TIMED_OUT);

        messageController.changeMessageState(CONVERSATION_ID, MESSAGE_ID, timedOutMessage);
    }

    @Test
    public void successfulModeration() throws Exception {
        when(conversationRepository.getById(CONVERSATION_ID)).thenReturn(conversation);

        conversation.applyCommand(new AddMessageCommand(
                CONVERSATION_ID, MESSAGE_ID, MessageState.HELD, MessageDirection.BUYER_TO_SELLER,
                now, "", "", ImmutableMap.of(), ImmutableList.of(), ImmutableList.of()));

        ResponseObject<?> responseObject = messageController.changeMessageState(CONVERSATION_ID, MESSAGE_ID, positiveModeration);

        verify(moderationService).changeMessageState(conversation, MESSAGE_ID, new ModerationAction(ModerationResultState.GOOD, Optional.ofNullable(EDITOR_NAME)));

        assertThat(responseObject.getStatus().getState(), equalTo(RequestState.SUCCESS));
    }

    // Message starts out in HELD, moderated BAD in another tab, moderated GOOD
    // as part of a bulk operation in the original tab.
    @Test
    public void oldStateChanged_errorReturned() throws Exception {
        ModerateMessagePayload moderationCommandWithOldState = new ModerateMessagePayload();
        moderationCommandWithOldState.setEditor(EDITOR_NAME);
        moderationCommandWithOldState.setCurrentMessageState(MessageState.HELD);
        moderationCommandWithOldState.setNewMessageState(ModerationResultState.GOOD);

        when(conversationRepository.getById(CONVERSATION_ID)).thenReturn(conversation);
        conversation.applyCommand(new AddMessageCommand(
                CONVERSATION_ID, MESSAGE_ID, MessageState.HELD, MessageDirection.BUYER_TO_SELLER,
                now, "", "", ImmutableMap.of(), ImmutableList.of(), ImmutableList.of()));
        conversation.applyCommand(
                new MessageModeratedCommand(CONVERSATION_ID, MESSAGE_ID, now, new ModerationAction(ModerationResultState.BAD, Optional.ofNullable(EDITOR_NAME)))
        );

        ResponseObject<?> response = messageController.changeMessageState(CONVERSATION_ID, MESSAGE_ID, moderationCommandWithOldState);
        assertThat(response.getStatus().getState(), equalTo(RequestState.ENTITY_OUTDATED));
        assertThat(response.getStatus().getDetails(), equalTo("The state of the message with id " + MESSAGE_ID +" has already changed"));

        verify(conversationRepository).getById(CONVERSATION_ID);
        verifyNoMoreInteractions(moderationService);
    }
}