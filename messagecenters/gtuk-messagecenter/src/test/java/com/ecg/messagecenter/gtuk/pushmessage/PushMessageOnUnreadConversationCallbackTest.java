package com.ecg.messagecenter.gtuk.pushmessage;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.gtuk.persistence.ConversationThread;
import com.ecg.messagecenter.gtuk.pushmessage.AdInfoLookup;
import com.ecg.messagecenter.gtuk.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.gtuk.pushmessage.PushMessagePayload;
import com.ecg.messagecenter.gtuk.pushmessage.PushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class PushMessageOnUnreadConversationCallbackTest {
    private static final String CONV_ID = "asd1:2312";
    private static final String BUYER_MAIL = "buyer@buyer.de";
    private static final String SELLER_MAIL = "seller@seller.de";

    private PushMessageOnUnreadConversationCallback listener;

    private Message message;
    private PushService pushService;
    private PostBox postBox;

    @Before
    public void setUp() {
        pushService = mock(PushService.class);
        PushService.Result result = PushService.Result.ok(null);
        when(pushService.sendPushMessage(any(PushMessagePayload.class))).thenReturn(result);

        postBox = mock(PostBox.class, RETURNS_DEEP_STUBS);
        when(postBox.getEmail()).thenReturn("foo@bar.de");
        Map<String, ConversationThread> unreadConvs = mock(Map.class);
        when(unreadConvs.size()).thenReturn(10);

        when(postBox.getUnreadConversationsCapped()).thenReturn(unreadConvs);
        when(postBox.getNewRepliesCounter().getValue()).thenReturn(2L);

        Map<String, String> customHeaders = new HashMap<String, String>();
        customHeaders.put("seller-name", "Seller via Gumtree");
        customHeaders.put("buyer-name", "Buyer via Gumtree");

        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn("asdf:asdf");
        when(conversation.getAdId()).thenReturn("123");
        when(conversation.getBuyerId()).thenReturn(BUYER_MAIL);
        when(conversation.getSellerId()).thenReturn(SELLER_MAIL);
        when(conversation.getCustomValues()).thenReturn(customHeaders);

        message = mock(Message.class);
        when(message.getPlainTextBody()).thenReturn("message-text");

        AdInfoLookup adInfoLookup = mock(AdInfoLookup.class);
        AdInfoLookup.AdInfo adInfo = new AdInfoLookup.AdInfo();
        adInfo.setImageUrl("http://image_url");
        adInfo.setTitle("Ad Title");
        when(adInfoLookup.lookupAdIInfo(anyLong())).thenReturn(Optional.ofNullable(adInfo));

        listener = new PushMessageOnUnreadConversationCallback(pushService, adInfoLookup, conversation, message);
    }

    @Test
    public void doNotSendPushMessageIfNoRulesApply() {
        listener.success(postBox.getEmail(), 0L, false);

        verify(pushService, never()).sendPushMessage(any(PushMessagePayload.class));
    }

    @Test
    public void sendPushMessageToSeller() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        listener.success(postBox.getEmail(), 1L, true);

        verify(pushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"));
            }
        }));
    }

    @Test
    public void sendPushMessageToBuyerIfRulesApply() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        listener.success(postBox.getEmail(), 1L, true);

        verify(pushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"));
            }
        }));
    }

    @Test
    public void testPushMessagePayload() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        listener.success(postBox.getEmail(), 1L, true);

        verify(pushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                JSONObject json = JSONObject.fromObject(payload.asJson());
                return json.getString("message").equals("Buyer via Gumtree: message-text");
            }
        }));
    }

    private void listenerCallback() {
        listener.sendPushMessage(postBox.getEmail(), 1L);
    }
}