package com.ecg.messagecenter.webapi.responses;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.mock;

public class PostBoxResponseBuilderTest {

    private PostBoxResponseBuilder builder;
    private static final String USER_EMAIL = "buyerEmail@example.com";
    private static final String OTHER_USER_EMAIL = "otherBuyerEmail@example.com";

    private PostBox<ConversationThread> postBox;

    @Before
    public void setUp() throws Exception {
        builder = new PostBoxResponseBuilder(mock(RiakConversationBlockRepository.class), 30);
        postBox = new PostBox<>("" +
                USER_EMAIL,
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("1", "a", now(), now().minusHours(100), now(), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(OTHER_USER_EMAIL), Optional.of("SELLER_TO_BUYER")),
                        new ConversationThread("2", "b", now(), now().minusHours(10), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER")),
                        new ConversationThread("3", "c", now(), now().minusHours(10), now().minusHours(4), false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER")),
                        new ConversationThread("4", "d", now(), now().minusHours(10), now().minusHours(4), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER"))
                )
        );
    }

    @Test
    public void testFilterByRole_filteredAndNumFoundIsCorrect() throws Exception {
        ResponseObject<PostBoxResponse> postBoxResponseResponseObject = builder.buildPostBoxResponse(USER_EMAIL, 1, 1, ConversationRole.Buyer, postBox);
        Assert.assertEquals(1, postBoxResponseResponseObject.getBody().getConversations().size());
        Assert.assertEquals(3, postBoxResponseResponseObject.getBody().get_meta().getNumFound());
    }

    @Test
    public void testGetNumUnreadForRole_countCorrectly() throws Exception {
        final Map<ConversationRole, Integer> numUnreadPerRole = builder.buildPostBoxResponse(USER_EMAIL, 5, 0, ConversationRole.Seller, postBox).getBody().getNumUnreadPerRole();
        Assert.assertEquals(1, (int) numUnreadPerRole.get(ConversationRole.Seller));
        Assert.assertEquals(2, (int) numUnreadPerRole.get(ConversationRole.Buyer));
    }

    @Test
    public void testGetNumUnreadForRole_unreadCountIsGlobal() throws Exception {
        // Ignore pagination
        int numUnreadAsBuyer = builder.buildPostBoxResponse(USER_EMAIL, 1, 10, ConversationRole.Buyer, postBox).getBody().getNumUnreadPerRole().get(ConversationRole.Buyer);

        Assert.assertEquals(2, numUnreadAsBuyer);
    }

    @Test
    public void testExpiredConversationsInPostBox() throws Exception {

        PostBox expiredConvsPostBox = new PostBox<>(
                USER_EMAIL,
                Optional.of(0L),
                Lists.newArrayList(
                        new ConversationThread("2", "b", now(), now().minusDays(05), now().minusDays(04), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(OTHER_USER_EMAIL), Optional.of("SELLER_TO_BUYER")),
                        new ConversationThread("3", "c", now(), now().minusDays(29), now().minusDays(29), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER")),
                        new ConversationThread("4", "d", now(), now().minusDays(30), now().minusDays(30), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER")),
                        new ConversationThread("5", "e", now(), now().minusDays(31), now().minusDays(31), true, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(USER_EMAIL), Optional.of("BUYER_TO_SELLER"))
                )
        );
        ResponseObject<PostBoxResponse> postBoxResponseResponseObject = builder.buildPostBoxResponse(USER_EMAIL, 5, 0, expiredConvsPostBox);
        Assert.assertEquals(3, postBoxResponseResponseObject.getBody().getConversations().size());
        Assert.assertEquals(3, (int) postBoxResponseResponseObject.getBody().getNumUnread());
        Assert.assertEquals(2, (int) postBoxResponseResponseObject.getBody().getNumUnreadPerRole().get(ConversationRole.Buyer));
        Assert.assertEquals(1, (int) postBoxResponseResponseObject.getBody().getNumUnreadPerRole().get(ConversationRole.Seller));
    }
}
