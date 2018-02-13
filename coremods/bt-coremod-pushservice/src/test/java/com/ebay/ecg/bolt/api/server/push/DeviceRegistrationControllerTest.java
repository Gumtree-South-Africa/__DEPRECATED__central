package com.ebay.ecg.bolt.api.server.push;

import com.ebay.ecg.bolt.api.server.push.model.NotificationType;
import com.ebay.ecg.bolt.api.server.push.model.PushProvider;
import com.ebay.ecg.bolt.api.server.push.model.RegistrationRequest;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PushRegistration;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PwaDetails;
import com.ebay.ecg.bolt.platform.module.push.persistence.repository.PushServiceRepository;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DeviceRegistrationControllerTest {
    @Mock
    private RegistrationRequest request;

    @Mock
    private PushServiceRepository repository;

    @Mock
    private PushRegistration registration;

    @InjectMocks
    private DeviceRegistrationController controller;

    @Before
    public void init(){
        when(repository.find(anyString(), anyString(), anyString(), isA(Locale.class))).thenReturn(Arrays.asList(registration));
        when(repository.find(anyString(), anyString(), any(PushProvider.class),anyString(), isA(Locale.class))).thenReturn(registration);
    }

    @Test
    public void updateUserDeviceRegistrationTest() {
        try {
            controller.createUserDeviceRegistration("123", "gcm", "CHATMESSAGE", "device555", "en_ZA");
        } catch (Exception e) {
            fail("Unexpected Exception thrown -- " + e);
        }

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void createUserDeviceRegistrationTest() throws Exception{
        controller.createUserDeviceRegistration("123", "gcm", "SEARCHALERTS", "device555","en_ZA");

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void shouldCreatePwaDeviceRegistration() throws Exception {
        when(request.getAppType()).thenReturn("pwa");
        when(request.getEndPoint()).thenReturn("device222");
        when(request.getLocale()).thenReturn("en_ZA");
        when(request.getNotificationType()).thenReturn(NotificationType.CHATMESSAGE);
        when(request.getPublicKey()).thenReturn("xcyz");
        when(request.getPushProvider()).thenReturn(PushProvider.gcm);
        when(request.getSecret()).thenReturn("abc");
        when(request.getUserId()).thenReturn("3456");

        controller.deviceRegistration(request);

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void shouldCreateAppDeviceRegistration() throws Exception{
        when(request.getDeviceId()).thenReturn("device222");
        when(request.getLocale()).thenReturn("en_ZA");
        when(request.getNotificationType()).thenReturn(NotificationType.CHATMESSAGE);
        when(request.getPushProvider()).thenReturn(PushProvider.gcm);
        when(request.getUserId()).thenReturn("3456");

        controller.deviceRegistration(request);

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void shouldUpdatePwaDeviceRegistration() throws Exception{
        when(request.getAppType()).thenReturn("pwa");
        when(request.getEndPoint()).thenReturn("device8888");
        when(request.getLocale()).thenReturn("en_ZA");
        when(request.getNotificationType()).thenReturn(NotificationType.CHATMESSAGE);
        when(request.getPublicKey()).thenReturn("xcyz");
        when(request.getPushProvider()).thenReturn(PushProvider.gcm);
        when(request.getSecret()).thenReturn("abc");
        when(request.getUserId()).thenReturn("3456");
        when(repository.find("3456", "CHATMESSAGE", PushProvider.gcm, "pwa", new LocaleEntity("en_ZA").getLocale())).thenReturn(registration);

        controller.deviceRegistration(request);

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void shouldUpdateAppDeviceRegistration() throws Exception{
        when(request.getDeviceId()).thenReturn("device8888");
        when(request.getLocale()).thenReturn("en_ZA");
        when(request.getNotificationType()).thenReturn(NotificationType.CHATMESSAGE);
        when(request.getPushProvider()).thenReturn(PushProvider.gcm);
        when(request.getUserId()).thenReturn("3456");
        when(repository.find("3456", "CHATMESSAGE", "gcm", new LocaleEntity("en_ZA").getLocale())).thenReturn(Arrays.asList(registration));

        controller.deviceRegistration(request);

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void deleteAPPDeviceRegistrationFromListOfRegistration() throws Exception{
        List<String> modifiableList = new ArrayList<>();

        modifiableList.add("device111");
        modifiableList.add("device222");

        when(registration.getDeviceTokens()).thenReturn(modifiableList);

        controller.deleteUserDeviceRegistration("123", "gcm", "CHATMESSAGE", "device111","en_ZA");

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    @Test
    public void deleteAppDeviceRegistration() throws Exception {
        List<String> modifiableList = new ArrayList<>();

        modifiableList.add("device111");

        when(registration.getDeviceTokens()).thenReturn(modifiableList);

        controller.deleteUserDeviceRegistration("555", "gcm", "CHATMESSAGE", "device111","en_ZA");

        verify(repository, times(1)).remove(isNull(String.class));
    }

    @Test
    public void deletePWADeviceRegistration() throws Exception {
        when(registration.getPwaDetails()).thenReturn(getPwaDetails("device111"));
        when(registration.getAppType()).thenReturn("abc");

        controller.deleteDeviceRegistration(getPwaUnRegisterRequest());

        verify(repository, times(1)).remove(isNull(String.class));
    }
    @Test
    public void deletePWADeviceRegistrationFromListOfRegistration() throws Exception{
        when(registration.getPwaDetails()).thenReturn(getListOfPwaDetails());
        when(registration.getAppType()).thenReturn("abc");

        controller.deleteDeviceRegistration(getPwaUnRegisterRequest());

        verify(repository, times(1)).save(isA(PushRegistration.class));
    }

    private List<PwaDetails> getPwaDetails(String endPoint){
        PwaDetails details = new PwaDetails();

        details.setEndPoint(endPoint);
        details.setPublicKey("xxxxxx");
        details.setSecret("yyyyyy");

        List<PwaDetails> modifiableResult = new ArrayList<>();

        modifiableResult.add(details);

        return modifiableResult;
    }

    private List<PwaDetails> getListOfPwaDetails(){
        List<PwaDetails> deviceTokenDetails = new ArrayList<>();

        deviceTokenDetails.addAll(getPwaDetails("device111"));
        deviceTokenDetails.addAll(getPwaDetails("device222"));

        return deviceTokenDetails;
    }

    private RegistrationRequest getPwaUnRegisterRequest(){
        RegistrationRequest pwaRequest = new RegistrationRequest();

        pwaRequest.setUserId("123");
        pwaRequest.setEndPoint("device111");
        pwaRequest.setPushProvider(PushProvider.gcm);
        pwaRequest.setNotificationType(NotificationType.CHATMESSAGE);
        pwaRequest.setLocale("en_ZA");
        pwaRequest.setAppType("pwa");

        return pwaRequest;
    }
}