package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ImmutableMessageTest {

    private static final String X_SOME_HEADER = "X-HEADER-KEY";
    private static final String HEADER_VALUE = "header value";
    private static final String LAST_EDITOR = "Last Editor";

    @Test
    public void buildMessageWithCaseInsensitiveHeader() {
        ImmutableMessage.Builder builder = minimalValidMessageBuilder();

        Message message = builder
                .withHeader(X_SOME_HEADER, HEADER_VALUE)
                .build();

        assertEquals(HEADER_VALUE, message.getCaseInsensitiveHeaders().get(X_SOME_HEADER.toLowerCase()));
    }

    @Test
    public void buildMessageWithCaseInsensitiveHeaders() {
        ImmutableMessage.Builder builder = minimalValidMessageBuilder();

        Message message = builder
                .withHeaders(Collections.singletonMap(X_SOME_HEADER, HEADER_VALUE))
                .build();

        assertEquals(HEADER_VALUE, message.getCaseInsensitiveHeaders().get(X_SOME_HEADER.toLowerCase()));
    }

    private ImmutableMessage.Builder minimalValidMessageBuilder() {
        return ImmutableMessage.Builder.aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now());
    }

    @Test
    public void buildMessageWithLastEditor() {
        ImmutableMessage.Builder builder = minimalValidMessageBuilder();

        Message message = builder
                .withLastEditor(LAST_EDITOR)
                .build();

        assertEquals(LAST_EDITOR, message.getLastEditor().orElse(null));
    }

    @Test
    public void buildMessageWithoutLastEditor() {
        ImmutableMessage.Builder builder = minimalValidMessageBuilder();

        Message message = builder.build();

        assertFalse(message.getLastEditor().isPresent());
    }

    @Test
    public void buildMessageWithNullLastEditor() {
        ImmutableMessage.Builder builder = minimalValidMessageBuilder();

        Message message = builder
                .withLastEditor(null)
                .build();

        assertFalse(message.getLastEditor().isPresent());
    }
}