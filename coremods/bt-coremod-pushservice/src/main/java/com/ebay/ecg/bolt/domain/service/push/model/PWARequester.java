package com.ebay.ecg.bolt.domain.service.push.model;

import com.google.common.io.BaseEncoding;
import net.sf.json.JSONObject;
import nl.martijndwars.webpush.Encrypted;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Utils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;

public class PWARequester extends PushRequester {
    private static final Logger LOG = LoggerFactory.getLogger(PWARequester.class);

    public PWARequester(PushHostInfo pushHostInfo) {
        super(pushHostInfo);
    }

    @Override
    public HttpResponse sendPush(HttpClient httpClient, PushMessagePayload payload,  String deviceToken, String notificationTitle, PWAInfo pwaInfo) throws Exception {
        String payloadForPush = buildPayload(payload,notificationTitle);

        LOG.debug("PWA Payload {}", payloadForPush);

        PublicKey userPublicKey = Utils.loadPublicKey(pwaInfo.getPublicKey());
        byte[] userAuth = BaseEncoding.base64Url().decode(pwaInfo.getSecret());

        return send(httpClient, new Notification(pwaInfo.getEndPoint(), userPublicKey, userAuth, payloadForPush.getBytes()));
    }

    private HttpResponse send(HttpClient httpClient,Notification notification) throws Exception {
        HttpPost httpPost = null;

        try {
            BaseEncoding base64url = BaseEncoding.base64Url();
            Encrypted encrypted = PushService.encrypt(
              notification.getPayload(),
              notification.getUserPublicKey(),
              notification.getUserAuth(),
              notification.getPadSize());

            byte[] dh = Utils.savePublicKey((ECPublicKey) encrypted.getPublicKey());
            byte[] salt = encrypted.getSalt();

            httpPost = new HttpPost(notification.getEndpoint());
            httpPost.addHeader("TTL", String.valueOf(notification.getTTL()));

            Map<String, String> headers = new HashMap<>();

            if (notification.hasPayload()) {
                headers.put("Content-Type", "application/octet-stream");
                headers.put("Content-Encoding", "aesgcm");
                headers.put("Encryption", format("keyid=p256dh;salt=%s", base64url.omitPadding().encode(salt)));
                headers.put("Crypto-Key", format("keyid=p256dh;dh=%s", base64url.encode(dh)));

                httpPost.setEntity(new ByteArrayEntity(encrypted.getCiphertext()));
            }
            headers.put("Authorization", format("key=%s", getPushHostInfo().getAuthHeader()));

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpPost.addHeader(new BasicHeader(entry.getKey(), entry.getValue()));
            }

            LOG.debug("Request to PWA body {}", EntityUtils.toString(httpPost.getEntity()));

            return httpClient.execute(httpPost);
        } finally {
            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
    }

    public String buildPayload(PushMessagePayload payload, String notificationTitle) {
        Map<String, String> data = new LinkedHashMap<>();

        String alert = payload.getMessage();

        if (!StringUtils.isEmpty(payload.getActivity())) {
            if (payload.getActivity().equalsIgnoreCase("SEARCHALERTS")) {
                data.put("type", "SEARCHALERTS");

                if (!StringUtils.isEmpty(payload.getDetails().get("receiverId"))) {
                    data.put("userId", payload.getDetails().get("receiverId"));
                }
            } else {
                data.put("type", payload.getActivity());
            }
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("alertId"))) {
            data.put("alertId", payload.getDetails().get("alertId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("notificationId"))) {
            data.put("NotificationId", payload.getDetails().get("notificationId"));
        }

        if (!StringUtils.isEmpty(alert)) {
            data.put("title", alert);
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("conversationId"))) {
            data.put("ConversationId", payload.getDetails().get("conversationId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("adId"))) {
            data.put("AdId", payload.getDetails().get("adId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("adTitle"))) {
            data.put("AdSubject", payload.getDetails().get("adTitle").toString());
        }

        if(!StringUtils.isEmpty(payload.getDetails().get("senderId"))) {
            data.put("senderId", payload.getDetails().get("senderId"));
        }

        JSONObject pushDataObj = new JSONObject();

        pushDataObj.putAll(data);

        return pushDataObj.toString();
    }
}