package com.ecg.messagebox.util.messages;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GtukMessageResponseFactoryTest {

    @Mock
    private Message message;

    @Test
    public void testParsingTemplate() throws IOException {
        InputStream fileStream = GtukMessageResponseFactoryTest.class.getResourceAsStream("/gtuk-templated-email.txt");
        String body = IOUtils.toString(fileStream, Charset.defaultCharset());
        when(message.getPlainTextBody()).thenReturn(body);

        GtukMessageResponseFactory factory = new GtukMessageResponseFactory();
        String message = factory.getCleanedMessage(null, this.message);

        assertEquals("test 1st message", message);
    }
}
