package com.ebay.ecg.bolt.domain.service.push;

import com.ebay.ecg.bolt.api.server.push.model.PushProvider;
import com.ebay.ecg.bolt.domain.service.push.model.PushMessagePayload;
import com.ebay.ecg.bolt.domain.service.push.model.Result;
import com.ebay.ecg.bolt.domain.service.push.model.PushRequester;
import com.ebay.ecg.bolt.domain.service.push.model.PWAInfo;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PushRegistration;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PwaDetails;
import com.ebay.ecg.bolt.platform.module.push.persistence.repository.PushServiceRepository;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
public class PushDomainService {
    private final static Logger LOG = LoggerFactory.getLogger(PushDomainService.class);

    private static final int HTTP_CONNECTION_TIMEOUT = 5000;

    private static final int HTTP_SO_TIMEOUT = 60000;

    private static final int HTTP_MAX_CONNECTIONS = 50;

    @Autowired
    private PushMessageServiceConfig pushMessageServiceConfig;

    @Autowired
    private PushServiceRepository pushServiceRepository;

    @Value("${chat.msg.notification.title}")
    private String chatMessageTitle;

    @Value("${saved.search.msg.notification.title}")
    private String savedSearchMessageTitle;

    private HttpClient gcmHttpClient = buildSystemAwareHttpClient();

    private HttpClient pwaHttpClient = buildSystemAwareHttpClient();

    @PostConstruct
    public void addSecurityProvider(){
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            LOG.debug("Adding BC Security Provider");

            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    public Result sendPushMessage(final PushProvider pushProvider, final PushMessagePayload payload, final String deviceToken, PWAInfo pwaInfo) {
        try {
            LOG.debug("PushServiceProvider sending: {}", payload);

            PushRequester pushRequester = pushMessageServiceConfig.findPushRequester(pushProvider);

            HttpClient httpClient = pushProvider.name().equalsIgnoreCase("gcm") ? gcmHttpClient : pwaHttpClient;

            HttpResponse response = pushRequester.sendPush(httpClient, payload, deviceToken, getNotificationTitle(payload), pwaInfo);

            return handleResponse(response, pushProvider, payload, deviceToken, pwaInfo);
        } catch (Exception e) {
            LOG.error("Error sending out push message {} payload {}", deviceToken, payload, e);

            return Result.error(payload, deviceToken, e);
        }
    }

    private Result handleResponse(HttpResponse response, PushProvider pushProvider, PushMessagePayload payload, String deviceToken, PWAInfo pwaInfo) throws IOException{
        int code = response.getStatusLine().getStatusCode();

        if (pushProvider.equals(PushProvider.gcm)) {
            LOG.info("GCMPushService response: {}", code);
        } else if (pushProvider.equals(PushProvider.mdns)) {
            LOG.info("MDNSPushService response: {}", code);
        } else if(pushProvider.equals(PushProvider.pwa)) {
            LOG.info("PWA response for the end point {} is {}", pwaInfo.getEndPoint(), code);
        }

        HttpEntity entity = response.getEntity();

        LOG.debug("Response payload {}", EntityUtils.toString(entity, "UTF-8"));

        switch (code) {
            case 200:
                return Result.ok(payload, deviceToken);
            case 404:
                return Result.notFound(payload, deviceToken);
            default:
                return Result.error(payload, deviceToken, new RuntimeException(format("Unexpected response: %s", response.getStatusLine())));
        }

    }

    private String getNotificationTitle(PushMessagePayload payload) {
        if (payload.getActivity().equalsIgnoreCase("SEARCHALERTS")) {
            return savedSearchMessageTitle;
        } else {
            return chatMessageTitle;
        }
    }

    public List<Result> sendPushMessages(final String receiverdId, final PushMessagePayload payload, String appType) {
        Assert.notNull(payload);
        Assert.notNull(payload.getDetails());
        Assert.notNull(payload.getDetails().get("locale"));
        Assert.notNull(payload.getActivity());

        LocaleEntity localeEntity = new LocaleEntity(payload.getDetails().get("locale"));

        LOG.debug("Fetching the registration details of <{}>,<{}>,<{}> and <{}>", receiverdId, payload.getActivity(), appType, localeEntity.getLocale());

        List<PushRegistration> pushRegistrations = getPushRegistrations(receiverdId, payload.getActivity(), appType, localeEntity);

        if (CollectionUtils.isNotEmpty(pushRegistrations) ) {
            LOG.debug("Found {} registrations for {}", pushRegistrations.size(), receiverdId);

            return pushRegistrations.parallelStream().map(s -> processPushRegistration(s,payload)).findFirst().orElseThrow(RuntimeException::new);
        } else {
            LOG.debug("No registration found for {}", receiverdId);

            return Collections.emptyList();
        }
    }

    private List<Result> processPushRegistration(PushRegistration pushRegistration,final PushMessagePayload payload){
        try {
            PushProvider pushProvider = PushProvider.valueOf(pushRegistration.getPushProvider().toString());

            List<Result> results = new ArrayList<>();

            // old app push notifications
            List<String> deviceTokens = pushRegistration.getDeviceTokens();

            if (CollectionUtils.isNotEmpty(deviceTokens)) {
                results.addAll(deviceTokens.stream().map(s -> sendPushMessage(pushProvider,payload,s,null)).collect(toList()));
            }

            List<PwaDetails> pwaDetails = pushRegistration.getPwaDetails();

            if (CollectionUtils.isNotEmpty(pwaDetails)) {
                for (PwaDetails pwaDetail : pwaDetails) {
                    PWAInfo pwaInfo = new PWAInfo(pwaDetail.getEndPoint(), pwaDetail.getPublicKey(), pwaDetail.getSecret(), pushRegistration.getAppType());

                    LOG.debug("Calling PWA push message for {}", pwaDetail.getEndPoint());

                    results.add(sendPushMessage(PushProvider.pwa, payload, null, pwaInfo));
                }
            }

            return results;
        } catch (Exception e) {
            LOG.error("Exception while sending the push notification ", e);

            return Collections.emptyList();
        }
    }

    // assumption: saved search alert send only to the channel it got registered; chat message should send to both APP and PWA
    private List<PushRegistration> getPushRegistrations(String receiverdId, String notificationType, String appType, LocaleEntity localeEntity) {
        if (notificationType.equalsIgnoreCase("SEARCHALERTS")) {
            return pushServiceRepository.find(receiverdId, notificationType, appType, localeEntity.getLocale());
        } else {
            return pushServiceRepository.find(receiverdId, notificationType, localeEntity.getLocale());
        }
    }

    private static HttpClient buildSystemAwareHttpClient() {
        return HttpClients.custom()
          .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
            .setSocketTimeout(HTTP_SO_TIMEOUT).build())
          .setMaxConnPerRoute(HTTP_MAX_CONNECTIONS)
          .setMaxConnTotal(HTTP_MAX_CONNECTIONS * 2)
          .useSystemProperties().build();
    }
}