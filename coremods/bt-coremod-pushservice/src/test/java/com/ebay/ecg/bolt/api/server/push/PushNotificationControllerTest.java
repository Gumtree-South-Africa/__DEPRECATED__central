package com.ebay.ecg.bolt.api.server.push;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import com.ebay.ecg.bolt.api.server.push.model.MetaChatMessage;
import com.ebay.ecg.bolt.api.server.push.model.MetaSearchAlerts;
import com.ebay.ecg.bolt.api.server.push.model.NotificationRequest;
import com.ebay.ecg.bolt.api.server.push.model.NotificationType;
import com.ebay.ecg.bolt.domain.service.push.PushDomainService;
import com.ebay.ecg.bolt.platform.module.push.persistence.repository.PushServiceRepository;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;

@RunWith(MockitoJUnitRunner.class)
public class PushNotificationControllerTest {
    private static final LocaleEntity LOCALE = new LocaleEntity("en_ZA");

    private static final String RECEIVER_USER_ID = "123123";
    private static final String NOTIFICATION_TYPE = NotificationType.CHATMESSAGE.name();
    private static final String HOW_ARE_YOU = "how are you";
    private static final String TO_EMAIL = "kemo@ebay.com";
    private static final String AD_ID = "123123";
    private static final String AD_TITLE = "buying power";
    private static final String AD_IMAGE = "http://www.ebay.com/image1.jpg";
    private static final String CONVERSATION_ID = "123123";
    private static final String BADGE = "3";

    @Mock
    private PushDomainService domainServiceImpl;

    @Mock
    private PushServiceRepository repo;

    @Mock
    private MetaChatMessage metaChat;

    @Mock
    private MetaSearchAlerts metaSearchAlert;

    @Mock
    private NotificationRequest notificationRequest;

    @InjectMocks
    private PushNotificationController pushNotificationController;

    @Test
    public void testChatMessagePushNotifications() {
        ResponseEntity<?> response = null;

        try {
            when(notificationRequest.getMessage()).thenReturn(HOW_ARE_YOU);
            when(notificationRequest.getToEmail()).thenReturn(TO_EMAIL);
            when(notificationRequest.getMeta()).thenReturn(metaChat);
            when(metaChat.getAdId()).thenReturn(AD_ID);
            when(metaChat.getAdTitle()).thenReturn(AD_TITLE);
            when(metaChat.getAdThumbNail()).thenReturn(AD_IMAGE);
            when(metaChat.getReceiverUserId()).thenReturn(RECEIVER_USER_ID);
            when(metaChat.getBadge()).thenReturn(BADGE);
            when(metaChat.getConversationId()).thenReturn(CONVERSATION_ID);

            response = pushNotificationController.pushNotifications(RECEIVER_USER_ID, NOTIFICATION_TYPE, LOCALE, notificationRequest);
        } catch (Exception e) {
            fail("Unexpected Exception thrown -- " + e);
        }

        assertEquals(404, response.getStatusCode().value());
    }
}