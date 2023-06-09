package com.ecg.comaas.gtuk.listener.statsnotifier.utils;

import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

public final class Fixtures {
    private Fixtures() {
    }

    public static Message buildMessage(MessageDirection messageDirection, ProcessingFeedback processingFeedback) {
        return ImmutableMessage.Builder.aMessage().
                withMessageDirection(messageDirection).
                withId("conversationId-1").
                withReceivedAt(DateTime.now()).
                withLastModifiedAt(DateTime.now()).
                withHumanResultState(ModerationResultState.GOOD).
                withHumanResultState(ModerationResultState.GOOD).
                withHeader("Content-Type", "application/x-www-form-urlencoded").
                withProcessingFeedback(ImmutableList.of(processingFeedback)).
                withTextParts(Arrays.asList("hello message")).
                withState(MessageState.SENT).
                withHeader(GumtreeCustomHeaders.CLIENT_ID.getHeaderValue(), "abcde.fgh").
                build();
    }

    public static Conversation buildConversation(List<Message> messages) {
        return ImmutableConversation.Builder.aConversation().
                withAdId("123").
                withBuyer("buyer@test.com", "buyerSecret123").
                withMessages(messages).
                withSeller("seller@test.com", "sellerSecret123").
                withState(ConversationState.ACTIVE).
                withLastModifiedAt(DateTime.now()).
                withCreatedAt(DateTime.now()).
                build();
    }
}
