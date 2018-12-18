package com.ecg.comaas.mde.listener.rating;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

public final class DealerRatingInviteListenerTest {

    private DealerRatingService svc = mock(DealerRatingService.class);
    private Message message = mock(Message.class);
    private Conversation conversation = mock(Conversation.class);
    private final DealerRatingInviteListener listener = new DealerRatingInviteListener(svc);

    public DealerRatingInviteListenerTest() {

        when(message.getId()).thenReturn("1234-abcd");
        when(conversation.getId()).thenReturn("56789-efgh");
        listener.messageProcessed(conversation, message);
    }

    @Test
    public void processIfInitial() {
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("X-Cust-Seller_Type", "dealer");
        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);

        listener.messageProcessed(conversation, message);

        verify(svc).saveInvitation(message, "56789-efgh");
    }

    @Test
    public void doNotProcessIfNotInitial() {
        final Map<String, String> headers = Maps.newHashMap();
        headers.put("X-Cust-Buyer_Type", "dealer");
        when(message.getCaseInsensitiveHeaders()).thenReturn(headers);

        listener.messageProcessed(conversation, message);

        verify(svc, never()).saveInvitation(message, "56789-efgh");
    }

}
