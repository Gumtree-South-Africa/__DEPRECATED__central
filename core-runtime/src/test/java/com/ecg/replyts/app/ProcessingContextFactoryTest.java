package com.ecg.replyts.app;

import com.google.common.base.Optional;
import org.junit.Test;

public class ProcessingContextFactoryTest {

    @Test
    public void canCreateDeadConversationForAbsentMail() throws Exception {
        ProcessingContextFactory factory = new ProcessingContextFactory(10L);
        factory.deadConversationForMessageIdConversationId("messageId", "conversationId", Optional.absent());
    }
}