package com.ecg.comaas.mp.preprocessor.emailoptout;

import com.ecg.comaas.mp.preprocessor.emailoptout.MpEmailOptOutPreProcessorFilter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MpEmailOptOutPreProcessorFilterTest {

    @Mock
    private Conversation conversationMock;
    @Mock
    private MessageProcessingContext messageProcessingContextMock;

    private MpEmailOptOutPreProcessorFilter victim;

    @Before
    public void setUp() {
        victim = new MpEmailOptOutPreProcessorFilter();

        when(messageProcessingContextMock.getConversation()).thenReturn(conversationMock);
    }

    @Test
    public void filterKlussenAd() {
        when(conversationMock.getAdId()).thenReturn("k123");
        assertTrue(victim.filter(messageProcessingContextMock));
    }

    @Test
    public void doNotFilterNonKlussenAd() {
        when(conversationMock.getAdId()).thenReturn("m123");
        assertFalse(victim.filter(messageProcessingContextMock));
    }
}