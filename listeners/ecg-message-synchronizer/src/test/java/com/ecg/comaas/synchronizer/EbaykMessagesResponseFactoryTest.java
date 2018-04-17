package com.ecg.comaas.synchronizer;

import com.ecg.comaas.synchronizer.extractor.EbaykMessagesResponseFactory;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.apache.tika.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EbaykMessagesResponseFactoryTest {

    @Mock
    private Message message;

    @Mock
    private Conversation conversation;

    @Test
    public void testParsingTemplate() throws IOException {
        InputStream fileStream = EbaykMessagesResponseFactoryTest.class.getResourceAsStream("/ebayk-templated-email.txt");
        String body = IOUtils.toString(fileStream);
        when(message.getId()).thenReturn("MESSAGE_ID");
        when(message.getPlainTextBody()).thenReturn(body);
        when(conversation.getMessages()).thenReturn(Collections.singletonList(message));

        EbaykMessagesResponseFactory factory = new EbaykMessagesResponseFactory();
        String result = factory.getCleanedMessage(conversation, message);
        assertEquals("My message!", result);
    }
}
