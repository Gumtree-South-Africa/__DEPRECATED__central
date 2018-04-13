package com.ecg.comaas.it.filter.conversationmonitor;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;

/**
 * Created by fmaffioletti on 28/07/2014.
 */
@RunWith(MockitoJUnitRunner.class) public class ConversationMonitorFilterTest {

    @Mock private MessageProcessingContext messageProcessingContext;

    @Mock private Conversation conversation;

    @Mock private Message message;

    @Mock private Mail mail;

    private final Long WARN_THRESHOLD = 5L;
    private final Long ERROR_THRESHOLD = 10L;
    private final int UNICODE_REPLACEMENT_CHAR = 65533;
    private final List<String> TRIGGER_CHARS_LIST =
                    Lists.newArrayList(Character.toString((char) UNICODE_REPLACEMENT_CHAR));

    @Before public void setUp() throws Exception {
        when(messageProcessingContext.getMail()).thenReturn(Optional.of(mail));
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(conversation.getId()).thenReturn("1");
        when(conversation.getMessages()).thenReturn(Lists.newArrayList(message));
    }

    @Test public void testConversationMonitorBelowThreshold() throws Exception {
        when(message.getPlainTextBody()).thenReturn("12345");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(WARN_THRESHOLD, ERROR_THRESHOLD,
                                        TRIGGER_CHARS_LIST, true);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

    @Test public void testConversationBetweenWarnAndErrorThreshold() throws Exception {
        when(message.getPlainTextBody()).thenReturn("1234567");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(WARN_THRESHOLD, ERROR_THRESHOLD,
                                        TRIGGER_CHARS_LIST, true);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

    @Test public void testConversationAboveErrorThreshold() throws Exception {
        when(message.getPlainTextBody()).thenReturn("1234567890A");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(WARN_THRESHOLD, ERROR_THRESHOLD,
                                        TRIGGER_CHARS_LIST, true);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

    @Test public void testConversationAboveErrorThresholdCheckDisabled() throws Exception {
        when(message.getPlainTextBody()).thenReturn("1234567890A");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(WARN_THRESHOLD, ERROR_THRESHOLD,
                                        TRIGGER_CHARS_LIST, false);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

    @Test public void testConversationNoThresholdDefined() throws Exception {
        when(message.getPlainTextBody()).thenReturn("1234567890A");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(null, null, new ArrayList<String>(), true);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

    @Test public void testConversationContainingTriggerChar() throws Exception {
        when(message.getPlainTextBody()).thenReturn("�abc�");
        ConversationMonitorFilter conversationMonitorFilter =
                        new ConversationMonitorFilter(WARN_THRESHOLD, ERROR_THRESHOLD,
                                        TRIGGER_CHARS_LIST, true);
        List<FilterFeedback> filterFeedbackList =
                        conversationMonitorFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

}
