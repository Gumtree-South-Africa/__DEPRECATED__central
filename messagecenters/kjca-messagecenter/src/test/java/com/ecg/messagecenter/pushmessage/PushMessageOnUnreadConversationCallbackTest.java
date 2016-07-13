package com.ecg.messagecenter.pushmessage;

import ca.kijiji.replyts.BoxHeaders;
import com.ecg.messagecenter.capi.AdInfoLookup;
import com.ecg.messagecenter.capi.UserInfoLookup;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.pushmessage.send.SendPushService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PushMessageOnUnreadConversationCallbackTest {

    private static final String BUYER_MAIL = "buyer@buyer.de";
    private static final String SELLER_MAIL = "seller@seller.de";

    private PushMessageOnUnreadConversationCallback listener;

    private Message message;
    private PushService amqPushService;
    private PushService sendPushService;
    private PostBox postBox;
    private Conversation conversation;
    private AdInfoLookup adInfoLookup;
    private UserInfoLookup userInfoLookup;

    @Before
    public void setUp() {
        amqPushService = mock(PushService.class);
        sendPushService = mock(SendPushService.class);
        PushService.Result result = mock(PushService.Result.class);
        when(amqPushService.sendPushMessage(any(PushMessagePayload.class))).thenReturn(result);
        when(sendPushService.sendPushMessage(any(PushMessagePayload.class))).thenReturn(result);

        Map<String, ConversationThread> unreadConvs = mock(Map.class);
        when(unreadConvs.size()).thenReturn(10);

        postBox = mock(PostBox.class, RETURNS_DEEP_STUBS);
        when(postBox.getEmail()).thenReturn("foo@bar.de");

        when(postBox.getUnreadConversations()).thenReturn(unreadConvs);
        message = mock(Message.class);
        when(message.getPlainTextBody()).thenReturn("message-text");

        conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn("asdf:asdf");
        when(conversation.getAdId()).thenReturn("123");
        when(conversation.getBuyerId()).thenReturn(BUYER_MAIL);
        when(conversation.getSellerId()).thenReturn(SELLER_MAIL);
        when(conversation.getMessages()).thenReturn(ImmutableList.of(message));


        adInfoLookup = mock(AdInfoLookup.class);
        AdInfoLookup.AdInfo adInfo = new AdInfoLookup.AdInfo();
        adInfo.setImageUrl("http://image_url");
        adInfo.setTitle("Ad Title");
        when(adInfoLookup.lookupAdInfo(anyLong())).thenReturn(Optional.ofNullable(adInfo));

        userInfoLookup = mock(UserInfoLookup.class);
        when(userInfoLookup.lookupUserInfo(anyString())).thenReturn(Optional.of(new UserInfoLookup.UserInfo("123")));

        listener = new PushMessageOnUnreadConversationCallback(0, amqPushService, sendPushService, adInfoLookup, userInfoLookup, conversation, message);
    }

    @Test
    public void doNotSendPushMessageIfNoRulesApply() {
        listener.success(postBox, false);

        verify(amqPushService, never()).sendPushMessage(any(PushMessagePayload.class));
    }

    @Test
    public void canSwitchToSendService() throws Exception {
        listener = new PushMessageOnUnreadConversationCallback(100, amqPushService, sendPushService, adInfoLookup, userInfoLookup, conversation, message);
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", SELLER_MAIL));
        listener.success(postBox, true);
        verify(sendPushService).sendPushMessage(any(PushMessagePayload.class));
        verify(amqPushService, never()).sendPushMessage(any(PushMessagePayload.class));
    }

    @Test
    public void sendPushMessageToBuyer() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "Poster display name <" + SELLER_MAIL + ">"));
        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"))
                        && payload.getMessage().equals("Poster display name: message-text")
                        && payload.getDetails().isPresent()
                        && payload.getDetails().get().containsKey("sound");
            }
        }));
    }

    @Test
    public void senderFromHasOnlyEmail_englishAd_useEnglishDefaultName() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", SELLER_MAIL));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"))
                        && payload.getMessage().equals("Kijiji User: message-text")
                        && payload.getDetails().isPresent()
                        && payload.getDetails().get().containsKey("sound");
            }
        }));
    }

    @Test
    public void senderFromHasOnlyEmail_frenchAd_useFrenchDefaultName() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", SELLER_MAIL, BoxHeaders.LOCALE.getHeaderName(), "fr_CA"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"))
                        && payload.getMessage().equals("Utilisateur Kijiji: message-text")
                        && payload.getDetails().isPresent()
                        && payload.getDetails().get().containsKey("sound");
            }
        }));
    }

    @Test
    public void sendPushMessageToSeller() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "Replier display name <" + BUYER_MAIL + ">"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"))
                        && payload.getMessage().equals("Replier display name: message-text")
                        && payload.getDetails().isPresent()
                        && payload.getDetails().get().containsKey("sound");
            }
        }));
    }

    @Test
    public void quotesInSenderDisplayName_noQuotesInPushMessage() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "\"Replier display name\" <" + BUYER_MAIL + ">"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getEmail().equals(("foo@bar.de"))
                        && payload.getMessage().equals("Replier display name: message-text")
                        && payload.getDetails().isPresent()
                        && payload.getDetails().get().containsKey("sound");
            }
        }));
    }

    @Test
    public void indicateUnreadConversationsInBase() {
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "Replier display name <" + BUYER_MAIL + ">"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                return payload.getDetails().isPresent() && payload.getDetails().get().get("badge").equals("10");
            }
        }));
    }

    @Test
    @Ignore("Removing truncation until apps can handle it properly.")
    public void longMessagesTruncatedForPush() {
        final String unnecessarilyLongMessage = "Some days you get the bear, and some days the bear gets you. Maybe if we felt any human loss as keenly as we feel one of those close to us, human history would be far less bloody. We could cause a diplomatic crisis. Take the ship into the Neutral Zone What's a knock-out like you doing in a computer-generated gin joint like this? Smooth as an android's bottom, eh, Data?";
        when(message.getPlainTextBody()).thenReturn(unnecessarilyLongMessage);
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "Poster display name <" + BUYER_MAIL + ">"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            private String expectedValue = "Poster display name: " + PushMessageOnUnreadConversationCallback.truncateText(unnecessarilyLongMessage, 50);
            private String payloadMessage;

            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                payloadMessage = payload.getMessage();
                return payloadMessage.equals(expectedValue);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText(expectedValue + " but got " + payloadMessage);
            }
        }));
    }

    @Test
    public void longMessagesNotTruncatedForPush() {
        final String unnecessarilyLongMessage = "Some days you get the bear, and some days the bear gets you. Maybe if we felt any human loss as keenly as we feel one of those close to us, human history would be far less bloody. We could cause a diplomatic crisis. Take the ship into the Neutral Zone What's a knock-out like you doing in a computer-generated gin joint like this? Smooth as an android's bottom, eh, Data?";
        when(message.getPlainTextBody()).thenReturn(unnecessarilyLongMessage);
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(ImmutableMap.of("From", "Poster display name <" + BUYER_MAIL + ">"));

        listener.success(postBox, true);

        verify(amqPushService).sendPushMessage(argThat(new ArgumentMatcher<PushMessagePayload>() {
            private String expectedValue = "Poster display name: " + unnecessarilyLongMessage;
            private String payloadMessage;

            @Override
            public boolean matches(Object o) {
                PushMessagePayload payload = (PushMessagePayload) o;
                payloadMessage = payload.getMessage();
                return payloadMessage.equals(expectedValue);
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText(expectedValue + " but got " + payloadMessage);
            }
        }));
    }
}
