package com.ecg.unicom.comaas.filter.mp;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlockImageAttachmentsBeforeSellerRepliesFilterTest {

    private static final String X_MESSAGE_META_DATA = loadXMessageMetaData();

    private final BlockImageAttachmentsBeforeSellerRepliesFilter filter = new BlockImageAttachmentsBeforeSellerRepliesFilter();

    @Test
    public void filteredOutWhenNoResponseFromSellerYet() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("x-MeSsaGE-metAdatA", X_MESSAGE_META_DATA);
        Message messageWithImagesAttached = mock(Message.class);
        when(messageWithImagesAttached.getCaseInsensitiveHeaders()).thenReturn(headers);

        Conversation conversation = mock(Conversation.class);
        List<Message> messages = Arrays.asList(
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                messageWithImagesAttached
        );
        when(conversation.getMessages()).thenReturn(messages);

        MessageProcessingContext context = mock(MessageProcessingContext.class);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessage()).thenReturn(messageWithImagesAttached);

        List<FilterFeedback> feedbacks = filter.filter(context);

        assertThat(feedbacks, hasSize(1));

        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getResultState(), is(FilterResultState.DROPPED));
    }

    @Test
    public void sellerHasNotReplied() {
        Conversation conversation = mock(Conversation.class);
        List<Message> messages = Arrays.asList(
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.BUYER_TO_SELLER)
        );
        when(conversation.getMessages()).thenReturn(messages);

        assertThat(filter.hasSellerReplied(conversation), is(false));
    }

    @Test
    public void hasSellerReplied() {
        Conversation conversation = mock(Conversation.class);
        List<Message> messages = Arrays.asList(
                mockMessage(MessageDirection.BUYER_TO_SELLER),
                mockMessage(MessageDirection.SELLER_TO_BUYER),
                mockMessage(MessageDirection.BUYER_TO_SELLER)
        );
        when(conversation.getMessages()).thenReturn(messages);

        assertThat(filter.hasSellerReplied(conversation), is(true));
    }


    @SuppressWarnings("UnstableApiUsage")
    private static String loadXMessageMetaData() {
        try {
            return Resources.toString(BlockImageAttachmentsBeforeSellerRepliesFilterTest.class.getResource("X-Message-Metadata.json"), Charsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Message mockMessage(MessageDirection direction) {
        Message msg = mock(Message.class);
        when(msg.getMessageDirection()).thenReturn(direction);
        return msg;
    }


    @Test
    public void doesntContainImages() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Message msg = mock(Message.class);
        when(msg.getCaseInsensitiveHeaders()).thenReturn(headers);

        assertThat(filter.containsImages(msg), is(false));
    }

    @Test
    public void containsImagesCalledWithInvalidJson() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("x-MeSsaGE-metAdatA", "INVALID JSON!");

        Message msg = mock(Message.class);
        when(msg.getCaseInsensitiveHeaders()).thenReturn(headers);

        assertThat(filter.containsImages(msg), is(false));
    }

    @Test
    public void containsImagesCalledWithValidJsonButUnexpectedStructure() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("x-MeSsaGE-metAdatA", "{\"attachment\":{}}");

        Message msg = mock(Message.class);
        when(msg.getCaseInsensitiveHeaders()).thenReturn(headers);

        assertThat(filter.containsImages(msg), is(false));
    }

    @Test
    public void containsImages() {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.put("x-MeSsaGE-metAdatA", X_MESSAGE_META_DATA);

        Message msg = mock(Message.class);
        when(msg.getCaseInsensitiveHeaders()).thenReturn(headers);

        assertThat(filter.containsImages(msg), is(true));
    }
}