package com.ecg.messagecenter.pushmessage;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: maldana
 * Date: 18.10.13
 * Time: 11:37
 *
 * @author maldana@ebay.de
 */
public class PushMessageOnUnreadConversationCallbackTest {

    private static final String BUYER_MAIL = "buyer@buyer.de";
    private static final String SELLER_MAIL = "seller@seller.de";

    private PushMessageOnUnreadConversationCallback listener;

    private Message message;
    private PushService pushService;

    private int unreadConversations = 10;
    private String userId = "12345";

    @Before
    public void setUp() {
        pushService = mock(PushService.class);
        PushService.Result result = mock(PushService.Result.class);
        when(pushService.sendPushMessage(any(PushMessagePayload.class))).thenReturn(result);

        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn("asdf:asdf");
        when(conversation.getAdId()).thenReturn("123");
        when(conversation.getBuyerId()).thenReturn(BUYER_MAIL);
        when(conversation.getSellerId()).thenReturn(SELLER_MAIL);

        message = mock(Message.class);
        when(message.getPlainTextBody()).thenReturn("message-text");

        AdImageLookup adImageLookup = mock(AdImageLookup.class);
        when(adImageLookup.lookupAdImageUrl(anyLong())).thenReturn("");

        listener = new PushMessageOnUnreadConversationCallback(pushService, adImageLookup, conversation, message);
    }

    @Test
    public void doNotSendPushMessageIfNoRulesApply() {
        listener.success(userId, unreadConversations, false);

        verify(pushService, never()).sendPushMessage(any(PushMessagePayload.class));
    }

    @Test
    public void sendPushMessageToRecipient() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        listener.success(userId, unreadConversations, true);

        verify(pushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getUserId().equals((userId));
            }
        }));
    }
}