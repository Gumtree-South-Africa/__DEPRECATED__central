package com.ecg.comaas.mde.postprocessor.addmetadata;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddMetadataPostProcessorTest {
    private MessageProcessingContext msgContext;

    @Before
    public void setUp() {
        msgContext = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(msgContext.getConversation().getId()).thenReturn("conv1234");
        when(msgContext.getMessageId()).thenReturn("msgId1234");
        when(msgContext.getOutgoingMail().getFrom()).thenReturn("sender@example.com");
    }

    @Test
    public void replaceSenderMailAddress() {
        TypedContent<String> typedContent = mock(TypedContent.class);

        List<TypedContent<String>> typedContents = Lists.newArrayList(typedContent);

        when(msgContext.getOutgoingMail().getTextParts(false)).thenReturn(typedContents);

        when(typedContent.getContent()).thenReturn("foo $ANONYMIZED_SENDER_ADDRESS$ bar");

        new AddMetadataPostProcessor(500).postProcess(msgContext);

        verify(typedContent).overrideContent("foo sender@example.com bar");
    }

    @Test
    public void addMessageId() {

        TypedContent<String> typedContent = mock(TypedContent.class);

        List<TypedContent<String>> typedContents = Lists.newArrayList(typedContent);

        when(msgContext.getOutgoingMail().getTextParts(false)).thenReturn(typedContents);

        when(typedContent.getContent()).thenReturn("foo $CONVERSATION_ID$ bar $MESSAGE_ID$ foo bar");

        new AddMetadataPostProcessor(500).postProcess(msgContext);

        verify(typedContent).overrideContent("foo conv1234 bar msgId1234 foo bar");
    }

    @Test
    public void addMessageIdMultipleOccurrences() {

        TypedContent<String> typedContent = mock(TypedContent.class);

        List<TypedContent<String>> typedContents = Lists.newArrayList(typedContent);

        when(msgContext.getOutgoingMail().getTextParts(false)).thenReturn(typedContents);

        when(typedContent.getContent()).thenReturn("foo bar $MESSAGE_ID$ foo bar $MESSAGE_ID$");

        new AddMetadataPostProcessor(500).postProcess(msgContext);

        verify(typedContent).overrideContent("foo bar msgId1234 foo bar msgId1234");
    }


    @Test
    public void addMessageIdNoOccurrences() {

        TypedContent<String> typedContent = mock(TypedContent.class);

        List<TypedContent<String>> typedContents = Lists.newArrayList(typedContent);

        when(msgContext.getOutgoingMail().getTextParts(false)).thenReturn(typedContents);

        when(typedContent.getContent()).thenReturn("foo bar $MESSAGE_ID foo bar $MESSAGE_ID");

        new AddMetadataPostProcessor(500).postProcess(msgContext);

        verify(typedContent).overrideContent("foo bar $MESSAGE_ID foo bar $MESSAGE_ID");
    }

    @Test
    public void testNoContent() {

        TypedContent<String> typedContent = mock(TypedContent.class);

        List<TypedContent<String>> typedContents = Lists.newArrayList(typedContent);

        when(msgContext.getOutgoingMail().getTextParts(false)).thenReturn(typedContents);

        when(typedContent.getContent()).thenReturn(null);

        new AddMetadataPostProcessor(500).postProcess(msgContext);

        verify(typedContent).overrideContent(null);
    }
}
