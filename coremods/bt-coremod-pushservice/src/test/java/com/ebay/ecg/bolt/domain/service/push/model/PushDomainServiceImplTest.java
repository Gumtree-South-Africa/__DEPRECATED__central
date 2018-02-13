package com.ebay.ecg.bolt.domain.service.push.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.ebay.ecg.bolt.domain.service.push.PushMessageServiceConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.ebay.ecg.bolt.api.server.push.model.NotificationType;
import com.ebay.ecg.bolt.api.server.push.model.PushProvider;
import com.ebay.ecg.bolt.domain.service.push.PushDomainService;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PushRegistration;
import com.ebay.ecg.bolt.platform.module.push.persistence.repository.PushServiceRepository;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class PushDomainServiceImplTest {
    private static final String RECEIVED_ID = "123";

    private static final Locale LOCALE = new Locale("en","ZA");

    private static final String deviceToken = "token1";

    @Mock
    private PushMessageServiceConfig pushMessageServiceConfig;

    @Mock
    private PushServiceRepository pushServiceRepository;

    @InjectMocks
    private PushDomainService pushDomainServiceImpl;

    @Before
    public void init() {
        Map<String, String> details = new HashMap();

        details.put("locale", LOCALE.toString());
        details.put("receiverId", RECEIVED_ID);

        GCMRequester gcmRequester = new GCMRequester(new PushHostInfo("123", "123", "123"), "normal");

        when(pushServiceRepository.find("123", NotificationType.CHATMESSAGE.name(), "gcm",
          new LocaleEntity("en_ZA").getLocale())).thenReturn(getGCMChatMessagePushRegistration());
        when(pushMessageServiceConfig.findPushRequester(PushProvider.gcm)).thenReturn(gcmRequester);
        when(pushServiceRepository.find("123", NotificationType.SEARCHALERTS.name(), "gcm",
          new LocaleEntity("en_ZA").getLocale())).thenReturn(getGCMSearchAlertPushRegistration());
        MDNSRequester mdnsRequester = new MDNSRequester(new PushHostInfo("123", "123", "123"));
        when(pushMessageServiceConfig.findPushRequester(PushProvider.mdns)).thenReturn(mdnsRequester);
        when(pushMessageServiceConfig.findPushRequester(PushProvider.gcm)).thenReturn(gcmRequester);
    }

    @Test
    public void testGCMSendPushMessages() throws Exception {
        Map<String, String> details = Collections.singletonMap("locale", "en_ZA");

        PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "Hello", NotificationType.CHATMESSAGE.name() , details);
        LocaleEntity localeEntity = new LocaleEntity(pushMessagePayload.getDetails().get("locale"));

        when(pushServiceRepository.find("123", NotificationType.CHATMESSAGE.name(), localeEntity.getLocale())).thenReturn(getGCMRegistrations());

        List<Result>  results = pushDomainServiceImpl.sendPushMessages(RECEIVED_ID, pushMessagePayload, null);

        assertEquals(3, results.size());
    }

    @Test
    public void testSendGCMChatMessagePushMessage() throws Exception {
        Map<String, String> details = Collections.singletonMap("locale", LOCALE.toString());

        PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "kevin: how are youd doing?", NotificationType.CHATMESSAGE.name(), details);
        Result result = pushDomainServiceImpl.sendPushMessage(PushProvider.gcm, pushMessagePayload, deviceToken, null);

        assertEquals(deviceToken, result.getDeviceToken());
    }

    @Test
    public void testSendGCMSearchAlertPushMessage() throws Exception {
        Map<String, String> details = Collections.singletonMap("locale", LOCALE.toString());

        PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "Hello", NotificationType.SEARCHALERTS.name(), details);
        Result result = pushDomainServiceImpl.sendPushMessage(PushProvider.gcm, pushMessagePayload, deviceToken,null);

        assertNotNull(result);
        assertThat(deviceToken, is(result.getDeviceToken()));
    }

    private List<PushRegistration> getGCMChatMessagePushRegistration() {
        Date currentDate = new Date(System.currentTimeMillis());

        PushRegistration ps = new PushRegistration();

        ps.setCreationDate(currentDate);
        ps.setModificationDate(currentDate);
        ps.setRegisterUserId(RECEIVED_ID);
        ps.setPushProvider("gcm".toUpperCase());
        ps.setNotificationType(NotificationType.CHATMESSAGE.name());
        ps.setLocale(LOCALE);
        ps.setDeviceTokens(Collections.singletonList("device111"));

        return Collections.singletonList(ps);
    }

    private List<PushRegistration> getGCMSearchAlertPushRegistration() {
        Date currentDate = new Date(System.currentTimeMillis());

        PushRegistration ps = new PushRegistration();

        ps.setCreationDate(currentDate);
        ps.setModificationDate(currentDate);
        ps.setRegisterUserId(RECEIVED_ID);
        ps.setPushProvider("gcm".toUpperCase());
        ps.setNotificationType(NotificationType.SEARCHALERTS.name());
        ps.setLocale(LOCALE);
        ps.setDeviceTokens(Collections.singletonList("device111"));

        return Collections.singletonList(ps);
    }

    private List<PushRegistration> getGCMRegistrations() {
        Date currentDate = new Date(System.currentTimeMillis());

        PushRegistration ps = new PushRegistration();

        ps.setCreationDate(currentDate);
        ps.setModificationDate(currentDate);
        ps.setRegisterUserId(RECEIVED_ID);
        ps.setPushProvider("gcm");
        ps.setNotificationType(NotificationType.CHATMESSAGE.name());
        ps.setLocale(LOCALE);
        ps.setDeviceTokens(Arrays.asList("device111", "device222", "device333"));

        return Collections.singletonList(ps);
    }
}