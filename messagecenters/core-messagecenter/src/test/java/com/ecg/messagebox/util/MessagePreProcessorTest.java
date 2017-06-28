package com.ecg.messagebox.util;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

abstract class MessagePreProcessorTest {

    protected boolean isStripHtmlTagsEnabled() {
        return false;
    }

    protected void cutAndCompare(List<String> patterns, String msg, String expected) {
        Message message = mock(Message.class);
        when(message.getPlainTextBody()).thenReturn(msg);

        AbstractEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("Test properties", toProperties(patterns)));

        Conversation conversation = mock(Conversation.class);

        MessagePreProcessor messagePreProcessor = new MessagePreProcessor(environment);

        ReflectionTestUtils.setField(messagePreProcessor, "stripHtmlTagsEnabled", isStripHtmlTagsEnabled());

        assertEquals("Message matches expected result", expected, messagePreProcessor.removeEmailClientReplyFragment(conversation, message));
    }

    private Map<String, Object> toProperties(List<String> patterns) {
        Map<String, Object> result = new HashMap<>();

        for (int i = 0; i < patterns.size(); i++) {
            result.put("message.normalization.pattern." + i, patterns.get(i));
        }

        return result;
    }

    protected String loadFileAsString(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }
    }
}