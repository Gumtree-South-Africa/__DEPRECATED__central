package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.*;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
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
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostBoxOverviewControllerTest {
    public static final int MAX_CONVERSATION_AGE_DAYS = 180;

    private PostBoxOverviewController controller;

    @Mock
    private PostBoxRepository postBoxRepository;
    @Mock
    private ConversationBlockRepository conversationBlockRepository;

    @Before
    public void setUp() throws Exception {
        controller = new PostBoxOverviewController(postBoxRepository, conversationBlockRepository, 180);
    }

    @Test
    public void postboxOnlyHasOldConversations_noneReturned() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");

        String email = "seller@example.com";

        DateTime tooLongAgo = new DateTime(UTC).minusDays(MAX_CONVERSATION_AGE_DAYS);
        DateTime now = DateTime.now(UTC);
        ConversationThread oldConversationThread = new ConversationThread(
                "oldAdId", "oldConversationId", tooLongAgo, now, now, false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("buyer@example.com"),
                Optional.of(MessageDirection.BUYER_TO_SELLER.name()));

        PostBox postbox = new PostBox(email, new Counter(), Lists.newArrayList(oldConversationThread));
        when(postBoxRepository.byId(email)).thenReturn(postbox);

        ResponseObject<PostBoxResponse> response = controller.getPostBoxByEmail(email, false, 100, 0, request);
        assertThat(response.getBody().getConversations().size(), equalTo(0));
    }
}
