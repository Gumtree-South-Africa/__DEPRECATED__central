package com.ecg.messagebox.util;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessagePreProcessorTest {

    @Test
    public void realAnswerTest() throws IOException {

        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer1.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer1_cut.txt");

        cutAndCompare(msg, expected);
    }
    
    @Test
    public void realAnswer2Test() throws IOException {

        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer2.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer2_cut.txt");

        cutAndCompare(msg, expected);
    }

    @Test
    public void realAnswer3Test() throws IOException {

        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer3.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer3_cut.txt");

        cutAndCompare(msg, expected);
    }

    
    
    private void cutAndCompare(String msg, String expected) {
        Message message = mock(Message.class);
        when(message.getPlainTextBody()).thenReturn(msg);

        Conversation conversation = mock(Conversation.class);
        String result = MessagePreProcessor.removeEmailClientReplyFragment(conversation, message);

        Assert.assertTrue(result.equals(expected));
    }

    private String loadFileAsString(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }
    }

}
