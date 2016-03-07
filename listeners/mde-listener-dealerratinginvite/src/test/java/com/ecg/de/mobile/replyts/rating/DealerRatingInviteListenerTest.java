package com.ecg.de.mobile.replyts.rating;

import com.ecg.de.mobile.replyts.rating.listener.DealerRatingInviteListener;
import com.ecg.de.mobile.replyts.rating.svc.DealerRatingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

/**
 * Created by vbalaramiah on 4/28/15.
 */
@RunWith(MockitoJUnitRunner.class)
public class DealerRatingInviteListenerTest {

    @Mock
    private DealerRatingService svc;

    @Mock
    private Message message;

    @Mock
    private Conversation conversation;

    @Test
    public void processIfInitial() {
        DealerRatingInviteListener listener = new DealerRatingInviteListener(svc);
        Map<String, String> headers = Maps.newHashMap();
        headers.put("X-Cust-Seller_Type","dealer");
        when(message.getHeaders()).thenReturn(headers);
        when(message.getId()).thenReturn("1234-abcd");
        when(conversation.getId()).thenReturn("56789-efgh");
        listener.messageProcessed(conversation, message);
        verify(svc).saveInvitation(message, "56789-efgh");
    }

    @Test
    public void doNotProcessIfNotInitial() {
        DealerRatingInviteListener listener = new DealerRatingInviteListener(svc);
        Map<String, String> headers = Maps.newHashMap();
        headers.put("X-Cust-Buyer_Type","dealer");
        when(message.getHeaders()).thenReturn(headers);
        when(message.getId()).thenReturn("1234-abcd");
        when(conversation.getId()).thenReturn("56789-efgh");
        listener.messageProcessed(conversation, message);
        verify(svc, never()).saveInvitation(message, "56789-efgh");
    }

}
