package com.ecg.replyts.app;

import com.ecg.replyts.core.runtime.mailcloaking.AnonymizedMailConverter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "replyts.maxMessageProcessingTimeSeconds = 10")
@Import(ProcessingContextFactory.class)
public class ProcessingContextFactoryTest {

    @MockBean
    private AnonymizedMailConverter anonymizedMailConverter;

    @Autowired
    private ProcessingContextFactory factory;

    @Test
    public void canCreateDeadConversationForAbsentMail() throws Exception {
        factory.deadConversationForMessageIdConversationId("messageId", "conversationId", Optional.empty());
    }
}