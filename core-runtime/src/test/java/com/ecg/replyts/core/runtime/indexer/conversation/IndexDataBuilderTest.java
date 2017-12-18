package com.ecg.replyts.core.runtime.indexer.conversation;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IndexDataBuilderTest {

    private IndexDataBuilder indexDataBuilder;

    @Before
    public void setUp() {
        MailCloakingService mailCloakingService = mock(MailCloakingService.class, RETURNS_MOCKS);
        indexDataBuilder = new IndexDataBuilder(mailCloakingService);
    }

    @Test
    public void lastModifiedAt() throws IOException {
        String expectedLastModified = "1985-05-19T08:00:00.000Z";
        Conversation conversation = mock(Conversation.class, RETURNS_MOCKS);
        when(conversation.getCreatedAt()).thenReturn(DateTime.now());
        Message message = mock(Message.class, RETURNS_MOCKS);
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getReceivedAt()).thenReturn(DateTime.now());
        when(message.getLastModifiedAt()).thenReturn(DateTime.parse(expectedLastModified));
        when(message.getLastEditor()).thenReturn(Optional.empty());

        IndexData indexData = indexDataBuilder.toIndexData(conversation, message);

        assertTrue("lastModified field is missed",
                indexData.getDocument().string().contains("\"lastModified\":\"" + expectedLastModified + "\""));
    }
}