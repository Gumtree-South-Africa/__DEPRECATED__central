package com.ecg.messagebox.util.messages;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public abstract class MessageResponseFactoryTestBase {

    @Mock
    protected Message message;

    protected void readMessageFromFile(String file) throws IOException {
        InputStream fileStream = MessageResponseFactoryTestBase.class.getResourceAsStream(file);
        String body = IOUtils.toString(fileStream, Charset.defaultCharset());

        when(message.getPlainTextBody()).thenReturn(body);
    }
}
