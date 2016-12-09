package ca.kijiji.replyts.adcounters;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.*;
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
import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.api.model.mail.Mail.ADID_HEADER;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class IncrementReplyCountListenerTest {

    private IncrementReplyCountListener incrementReplyCountListener;
    private Conversation conversation;

    @Mock
    private TnsApiClient tnsApiClient;

    private String adId = "1";

    @Before
    public void setup() {
        String newConversationId = "c1";

        incrementReplyCountListener = new IncrementReplyCountListener(tnsApiClient);
        NewConversationCommand newConversationBuilderCommand = aNewConversationCommand(newConversationId).withAdId(adId).build();
        List<ConversationEvent> events = ImmutableConversation.apply(newConversationBuilderCommand);
        conversation = ImmutableConversation.replay(events);
    }

    @Test
    public void firstMsgInConv_messageStateIsSent_countIncremented() throws Exception {
        Message message = newMessage(MessageState.SENT, true);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, times(1)).incrementReplyCount(adId);
    }

    @Test
    public void firstMsgInConv_messageStateIsNotSent_countNotIncremented() throws Exception {
        Message message = newMessage(MessageState.BLOCKED, true);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    @Test
    public void followUpMsg_messageStateIsSent_countNotIncremented() throws Exception {
        Message message = newMessage(MessageState.SENT, false);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    @Test
    public void followUpMsg_messageStateNotSent_countNotIncremented() throws Exception {
        Message message = newMessage(MessageState.HELD, false);
        incrementReplyCountListener.messageProcessed(conversation, message);
        verify(tnsApiClient, never()).incrementReplyCount(adId);
    }

    public Message newMessage(MessageState state, boolean useAdIdHeader) {
        return ImmutableMessage.Builder.aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(state)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withFilterResultState(FilterResultState.OK)
                .withHumanResultState(ModerationResultState.GOOD)
                .withHeader(useAdIdHeader ? ADID_HEADER : "X-Whatever", "adid")
                .withProcessingFeedback(ProcessingFeedbackBuilder.aProcessingFeedback()
                        .withFilterName("filterName")
                        .withFilterInstance("filterInstantce"))
                .withLastEditor(Optional.empty())
                .build();
    }
}
