package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.MessageTerminatedCommand;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation.Builder.aConversation;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ImmutableConversationJsonTest {

    @Test
    public void serializeToJson() throws IOException {
        Conversation conversation = aConversation()
                .withId("1234")
                .withAdId("876335")
                .withBuyer("b@home.nl", null)
                .withSeller("s@work.com", null)
                .withCreatedAt(new DateTime(2012, 1, 30, 14, 23, 42, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 31, 11, 10, 2, DateTimeZone.forID("Europe/Amsterdam")))
                .withState(ConversationState.ACTIVE)
                .addCustomValue("L1_CATEGORY", "123")
                .withMessage(defaultMessage("7624"))
                .withMessage(defaultMessage("7640")).build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.writeValueAsString(conversation);

    }


    @Test
    public void addsProcessingFeedbackForTerminateMessageCommand() throws Exception {

        ImmutableConversation conversation = aConversation().withId("id").withCreatedAt(new DateTime()).withLastModifiedAt(new DateTime()).withState(ConversationState.ACTIVE).withMessage(defaultMessage("msgid")).build();
        conversation = conversation.updateMany(
                conversation.apply(
                        new MessageTerminatedCommand("id", "msgid", Object.class, "reason", MessageState.IGNORED)));

        Message message = conversation.getMessageById("msgid");
        assertThat(message.getProcessingFeedback().size(), is(1));
        assertThat(message.getState(), is(MessageState.IGNORED));
    }

    @Test
    public void addsNoProcessingFeedbackForMessagesTerminatedAsSent() throws Exception {
        ImmutableConversation conversation = aConversation().withId("id").withCreatedAt(new DateTime()).withLastModifiedAt(new DateTime()).withState(ConversationState.ACTIVE).withMessage(defaultMessage("msgid")).build();
        conversation = conversation.updateMany(
                conversation.apply(
                        new MessageTerminatedCommand("id", "msgid", Object.class, "reason", MessageState.SENT)));

        Message message = conversation.getMessageById("msgid");
        assertThat(message.getProcessingFeedback().size(), is(0));
    }


    private static Builder defaultMessage(String id) {
        return aMessage()
                .withId(id)
                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withLastModifiedAt(new DateTime(2012, 1, 30, 20, 1, 52, DateTimeZone.forID("Europe/Amsterdam")))
                .withTextParts(Arrays.asList(""));
    }
}
