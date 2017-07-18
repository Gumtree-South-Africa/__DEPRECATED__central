package com.ecg.kijijiit.quickreply;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Created by fmaffioletti on 28/07/2014.
 */
@RunWith(MockitoJUnitRunner.class) public class QuickReplyFilterTest {

    @Mock private MessageProcessingContext messageProcessingContext;

    @Mock private Conversation conversation;

    @Mock private Message message;

    @Mock private Mail mail;

    private static final HeaderEntry QUICK_REPLY_HEADER =
                    new HeaderEntry("X-CUST-QUICK_REPLY_HEADER", -10000);
    private static final HeaderEntry QUICK_REPLY_HEADER_2 =
                    new HeaderEntry("X-CUST-QUICK_REPLY_HEADER_2", 1000);

    @Before public void setUp() throws Exception {
        when(messageProcessingContext.getMail()).thenReturn(mail);

        Map<String, String> quickReplyCustomHeadersMap = Maps.newHashMap();
        quickReplyCustomHeadersMap
                        .put(QUICK_REPLY_HEADER.getHeader(), "QUICK_REPLY_HEADER_PRESENCE");
        quickReplyCustomHeadersMap
                        .put(QUICK_REPLY_HEADER_2.getHeader(), "QUICK_REPLY_HEADER_2_PRESENCE");
        when(mail.getCustomHeaders()).thenReturn(quickReplyCustomHeadersMap);

        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(conversation.getMessages()).thenReturn(Lists.newArrayList(message));
    }

    @Test public void testQuickReplyHeaderPresence() throws Exception {
        QuickReplyFilter quickReplyFilter =
                        new QuickReplyFilter(Lists.newArrayList(QUICK_REPLY_HEADER));

        List<FilterFeedback> filterFeedbackList = quickReplyFilter.filter(messageProcessingContext);
        Assert.assertFalse(filterFeedbackList.isEmpty());
        Assert.assertEquals(1, filterFeedbackList.size());

        FilterFeedback filterFeedback = filterFeedbackList.get(0);
        Assert.assertNotNull(filterFeedback);
        Assert.assertEquals(QuickReplyFilter.QUICK_REPLY_HINT, filterFeedback.getUiHint());
        Assert.assertTrue(filterFeedback.getDescription().contains(QUICK_REPLY_HEADER.getHeader()));
        Assert.assertEquals(QUICK_REPLY_HEADER.getScore(), filterFeedback.getScore());
        Assert.assertEquals(FilterResultState.OK, filterFeedback.getResultState());
    }

    @Test public void testQuickReplyHeadersPresence() throws Exception {
        QuickReplyFilter quickReplyFilter = new QuickReplyFilter(
                        Lists.newArrayList(QUICK_REPLY_HEADER, QUICK_REPLY_HEADER_2));
        List<FilterFeedback> filterFeedbackList = quickReplyFilter.filter(messageProcessingContext);
        Assert.assertFalse(filterFeedbackList.isEmpty());
        Assert.assertEquals(1, filterFeedbackList.size());

        FilterFeedback filterFeedback = filterFeedbackList.get(0);
        Assert.assertNotNull(filterFeedback);
        Assert.assertEquals(QuickReplyFilter.QUICK_REPLY_HINT, filterFeedback.getUiHint());
        Assert.assertTrue(filterFeedback.getDescription().contains(QUICK_REPLY_HEADER.getHeader()));
        Assert.assertTrue(
                        filterFeedback.getDescription().contains(QUICK_REPLY_HEADER_2.getHeader()));
        Integer expectedFinalScore =
                        QUICK_REPLY_HEADER.getScore() + QUICK_REPLY_HEADER_2.getScore();
        Assert.assertEquals(expectedFinalScore, filterFeedback.getScore());
        Assert.assertEquals(FilterResultState.OK, filterFeedback.getResultState());
    }

    @Test public void testQuickReplyHeaderAbsence() throws Exception {
        QuickReplyFilter quickReplyFilter = new QuickReplyFilter(Lists.<HeaderEntry>newArrayList());
        List<FilterFeedback> filterFeedbackList = quickReplyFilter.filter(messageProcessingContext);
        Assert.assertTrue(filterFeedbackList.isEmpty());
    }

}
