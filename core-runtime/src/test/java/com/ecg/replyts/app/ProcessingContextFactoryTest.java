package com.ecg.replyts.app;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "replyts.maxMessageProcessingTimeSeconds = 10")
@Import(ProcessingContextFactory.class)
public class ProcessingContextFactoryTest {
    @Autowired
    private ProcessingContextFactory factory;

    @Test
    public void canCreateDeadConversationForAbsentMail() throws Exception {
        factory.deadConversationForMessageIdConversationId("messageId", "conversationId", Optional.empty());
    }
}