package com.ecg.comaas.core.filter.user;

import com.ecg.comaas.core.filter.user.PatternEntry;
import com.ecg.comaas.core.filter.user.Userfilter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.when;

/**
 * User: acharton
 * Date: 12/17/12
 */
@RunWith(MockitoJUnitRunner.class)
public class UserfilterTest {

    public static final String BUYER_MAIL = "buyer@test.de";
    public static final String SELLER_MAIL = "seller@test.de";
    @Mock
    private MessageProcessingContext mpc;
    @Mock
    private Conversation conv;
    private List<PatternEntry> patterns;

    @Before
    public void setUp() throws Exception {
        when(mpc.getConversation()).thenReturn(conv);
        when(conv.getBuyerId()).thenReturn(BUYER_MAIL);
        when(conv.getSellerId()).thenReturn(SELLER_MAIL);

        patterns = new ArrayList<PatternEntry>();
    }

    @Test
    public void ignoreConversationWhileNoFilterConfigs() throws Exception {
        List<FilterFeedback> feedbacks = new Userfilter(Collections.<PatternEntry>emptyList()).filter(mpc);

        assertTrue(feedbacks.isEmpty());
    }

    @Test
    public void detectBuyerPattern() throws Exception {

        patterns.add(new PatternEntry(Pattern.compile("buyer*"), 100));


        List<FilterFeedback> feedbacks = new Userfilter(patterns).filter(mpc);

        assertEquals(1, feedbacks.size());
    }

    @Test
    public void detectBuyerAndSellerPattern() throws Exception {

        patterns.add(new PatternEntry(Pattern.compile("buyer"), 100));
        patterns.add(new PatternEntry(Pattern.compile("seller"), 500));

        List<FilterFeedback> feedbacks = new Userfilter(patterns).filter(mpc);

        assertEquals(2, feedbacks.size());
    }

    @Test
    public void detectBuyerAndSellerPattern2() throws Exception {

        patterns.add(new PatternEntry(Pattern.compile("test.de"), 100));

        List<FilterFeedback> feedbacks = new Userfilter(patterns).filter(mpc);

        assertEquals(2, feedbacks.size());
    }

    @Test
    public void onDetectUserSetFeedbackStateOK() throws Exception {
        patterns.add(new PatternEntry(Pattern.compile("test.de"), 100));

        List<FilterFeedback> feedbacks = new Userfilter(patterns).filter(mpc);

        assertEquals(FilterResultState.OK, feedbacks.get(0).getResultState());

    }
}
