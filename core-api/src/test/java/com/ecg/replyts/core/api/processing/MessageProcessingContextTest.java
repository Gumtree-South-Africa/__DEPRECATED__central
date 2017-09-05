package com.ecg.replyts.core.api.processing;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import org.junit.Test;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageProcessingContext}.
 */
public class MessageProcessingContextTest {

    @Test
    public void testGetInResponseToMessageId_SunnyDay() throws Exception {
        MutableConversation conversation = mockConversation(
                mockMessage("1:1", "<ABC@abc.com>"),
                mockMessage("2:2", "<DEF@abc.com>"));

        MessageProcessingContext context = createContextUnderTest(conversation, Optional.of("1:1"));

        assertThat(context.getInResponseToMessageId(), is("1:1"));
    }

    @Test
    public void testGetInResponseToMessageId_NoReference() throws Exception {
        MutableConversation conversation = mockConversation(
                mockMessage("1:1", "<ABC@abc.com>"),
                mockMessage("2:2", "<DEF@abc.com>"));

        MessageProcessingContext context = createContextUnderTest(conversation, Optional.empty());

        assertNull(context.getInResponseToMessageId());
    }

    @Test
    public void testGetInResponseToMessageId_TakeLastWithDuplicateMessageIds() throws Exception {
        MutableConversation conversation = mockConversation(
                mockMessage("1:1", "<ABC@abc.com>"),
                mockMessage("2:2", "<ABC@abc.com>"));

        MessageProcessingContext context = createContextUnderTest(conversation, Optional.of("2:2"));

        assertThat("id must be of last matching message in conversation", context.getInResponseToMessageId(), is("2:2"));
    }

    @Test
    public void testGetInResponseToMessageId_NullForUnknownReference() throws Exception {
        MutableConversation conversation = mockConversation(
                mockMessage("1:1", "<DEF@abc.com>"),
                mockMessage("2:2", "<GHI@abc.com>"));

        MessageProcessingContext context = createContextUnderTest(conversation, Optional.of("<ABC@abc.com>"));

        assertNull(context.getInResponseToMessageId());
    }


    private MessageProcessingContext createContextUnderTest(MutableConversation conversation, Optional<String> lastReferenceMessageId) {
        Mail mail = mock(Mail.class);
        when(mail.getLastReferencedMessageId()).thenReturn(lastReferenceMessageId);

        MessageProcessingContext context = new MessageProcessingContext(mail, "3:3", new ProcessingTimeGuard(0L));
        context.setConversation(conversation);
        return context;
    }

    private MutableConversation mockConversation(Message... messages) {
        MutableConversation conversation = mock(MutableConversation.class);
        when(conversation.getMessages()).thenReturn(Arrays.asList(messages));
        return conversation;
    }

    private Message mockMessage(String messageId, String senderMessageIdHeader) {
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(messageId);
        when(message.getSenderMessageIdHeader()).thenReturn(senderMessageIdHeader);
        return message;
    }

}
