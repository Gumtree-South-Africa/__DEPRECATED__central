package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.mock;

public class PostBoxResponseBuilderTest {

    private PostBoxResponseBuilder builder;
    private static final String USER_EMAIL = "buyerEmail@example.com";
    private PostBox postBox;

    @Before
    public void setUp() throws Exception {
        builder = new PostBoxResponseBuilder(mock(RiakConversationBlockRepository.class));
        postBox = new PostBox("" +
                USER_EMAIL,
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("1", "a", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("someOtherBuyer@example.com"), Optional.empty()),
                        new ConversationThread("2", "b", now(), now().minusHours(10), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.empty()),
                        new ConversationThread("3", "c", now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.empty()),
                        new ConversationThread("4", "d", now(), now().minusHours(10), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.empty())
                ),
                180
        );
    }

    @Test
    public void testGetNumUnreadForRole_countCorrectly() throws Exception {
        int numUnreadAsSeller = builder.getNumUnreadForRole(USER_EMAIL, ConversationRole.Seller, postBox, 5);
        int numUnreadAsBuyer = builder.getNumUnreadForRole(USER_EMAIL, ConversationRole.Buyer, postBox, 5);
        Assert.assertEquals(1, numUnreadAsSeller);
        Assert.assertEquals(2, numUnreadAsBuyer);
    }

    @Test
    public void testGetNumUnreadForRole_capCorrectly() throws Exception {
        int numUnreadAsBuyer = builder.getNumUnreadForRole(USER_EMAIL, ConversationRole.Buyer, postBox, 1);
        Assert.assertEquals(1, numUnreadAsBuyer);
    }
}
