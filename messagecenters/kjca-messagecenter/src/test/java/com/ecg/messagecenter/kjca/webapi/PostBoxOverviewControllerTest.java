package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.core.persistence.Counter;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.kjca.persistence.ConversationThread;
import com.ecg.messagecenter.kjca.persistence.block.CassandraConversationBlockRepository;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.joda.time.DateTimeZone.UTC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxOverviewControllerTest {
    private PostBoxOverviewController controller;

    @Mock
    private SimplePostBoxRepository postBoxRepository;
    @Mock
    private CassandraConversationBlockRepository conversationBlockRepository;

    @Before
    public void setUp() throws Exception {
        controller = new PostBoxOverviewController(postBoxRepository, conversationBlockRepository, 180, false);
    }

    @Test
    public void postboxHasFilterRequest_filteredResultReturned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");

        String email = "seller@example.com";

        DateTime created = new DateTime(UTC).minusHours(4);
        DateTime now = DateTime.now(UTC);
        ConversationThread buyerThread = new ConversationThread(
                "adId0", "userIsSeller", created, now, now, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("buyer@example.com"), Optional.of(MessageDirection.BUYER_TO_SELLER.name()));

        ConversationThread sellerThread = new ConversationThread(
                "adId1", "userIsBuyer", created, now, now, false, Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(email), Optional.of(MessageDirection.SELLER_TO_BUYER.name()));

        PostBox postbox = new PostBox(email, new Counter(), Lists.newArrayList(buyerThread, sellerThread));
        when(postBoxRepository.byId(PostBoxId.fromEmail(email))).thenReturn(postbox);

        //not having role filter, return everything
        ResponseObject<PostBoxResponse> response = controller.getPostBoxByEmail(email, 100, 0, null);
        assertThat(response.getBody().getConversations().size(), equalTo(2));

        //retrieve conversation only if the user is buyer
        ResponseObject<PostBoxResponse> response1 = controller.getPostBoxByEmail(email, 100, 0, ConversationRole.Buyer);
        assertThat(response1.getBody().getConversations().size(), equalTo(1));
        assertEquals("userIsBuyer", response1.getBody().getConversations().get(0).getId());

        ResponseObject<PostBoxResponse> response2 = controller.getPostBoxByEmail(email, 100, 0, ConversationRole.Seller);
        assertThat(response2.getBody().getConversations().size(), equalTo(1));
        assertEquals("userIsSeller", response2.getBody().getConversations().get(0).getId());
    }
}
