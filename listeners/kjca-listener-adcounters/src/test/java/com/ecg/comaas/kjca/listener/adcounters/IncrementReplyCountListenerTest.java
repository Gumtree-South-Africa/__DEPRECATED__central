package com.ecg.comaas.kjca.listener.adcounters;

import com.ecg.comaas.kjca.coremod.shared.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationEvent;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.model.conversation.ProcessingFeedbackBuilder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.api.model.mail.Mail.ADID_HEADER;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class IncrementReplyCountListenerTest {

    private IncrementReplyCountListener incrementReplyCountListener;
    private Conversation conversation;

    @Mock
    private TnsApiClient tnsApiClient;

    private String adId = "1";
    private String conversationId = "c1";

    @Before
    public void setup() {
        incrementReplyCountListener = new IncrementReplyCountListener(tnsApiClient);
        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(conversationId).withAdId(adId).build();
        List<ConversationEvent> events = ImmutableConversation.apply(newConversationBuilderCommand);
        ImmutableConversation conv = ImmutableConversation.replay(events);

        ConversationCommand addMessageCommand = anAddMessageCommand(conversationId, "messageId").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                build();
        events = conv.apply(addMessageCommand);

        conversation = conv.updateMany(events);
    }

    @Test
    public void firstMsgInConv_messageStateIsSent_countIncremented() throws Exception {
        Message message = newMessage(MessageState.SENT);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, times(1)).incrementReplyCount(adId);
    }

    @Test
    public void firstMsgInConv_messageStateIsNotSent_countNotIncremented() throws Exception {
        Message message = newMessage(MessageState.BLOCKED);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    @Test
    public void followUpMsg_messageStateIsSent_countNotIncremented() throws Exception {
        ImmutableConversation conv = ((ImmutableConversation) conversation);
        ConversationCommand addMessageCommand = anAddMessageCommand(conversationId, "messageId2").
                withMessageDirection(MessageDirection.BUYER_TO_SELLER).
                withReceivedAt(new DateTime()).
                build();
        List<ConversationEvent> events = conv.apply(addMessageCommand);
        ImmutableConversation conversationWith2Messages = conv.updateMany(events);

        Message message = newMessage(MessageState.SENT);
        incrementReplyCountListener.messageProcessed(conversationWith2Messages, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    @Test
    public void followUpMsg_messageStateNotSent_countNotIncremented() throws Exception {
        Message message = newMessage(MessageState.HELD);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    public Message newMessage(MessageState state) {
        return ImmutableMessage.Builder.aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(state)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.GOOD)
                .withHeader(ADID_HEADER, "adid")
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                        .withFilterName("filterName")
                        .withFilterInstance("filterInstantce"))
                .build();
    }
}
