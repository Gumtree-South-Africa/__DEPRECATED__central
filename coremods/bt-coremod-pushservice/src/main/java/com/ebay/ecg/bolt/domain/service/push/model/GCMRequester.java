package com.ebay.ecg.bolt.domain.service.push.model;

import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class GCMRequester extends PushRequester {
    private static final Logger LOG = LoggerFactory.getLogger(GCMRequester.class);

    private static final String FCM_ENDPOINT = "/fcm/send";

    private final String searchAlertsPriority;
    
    public GCMRequester(PushHostInfo pushHostInfo, String searchAlertsPriority) {
        super(pushHostInfo);

        this.searchAlertsPriority = searchAlertsPriority;
    }

    public HttpPost build(final PushMessagePayload payload, final String deviceToken, String notificationTitle) throws Exception {
        HttpPost post = new HttpPost(FCM_ENDPOINT);

        String alert = payload.getMessage();

        JSONObject obj = new JSONObject();
        JSONObject dataObj = new JSONObject();

        // iOS data fields
        JSONObject notificationObj = new JSONObject();

        obj.put("to", deviceToken);

        Map<String, String> data = new LinkedHashMap<>();
        Map<String, String> notification = new LinkedHashMap<>();

        if (!StringUtils.isEmpty(payload.getActivity())) { // BOLT-20697
            if (payload.getActivity().equalsIgnoreCase("SEARCHALERTS")) {
                data.put("type", "2");

                if (!StringUtils.isEmpty(payload.getDetails().get("receiverId"))) {
                    data.put("userId", payload.getDetails().get("receiverId"));
                }
            } else {
                data.put("type", payload.getActivity());
            }
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("alertId"))) {
            data.put("alertId", payload.getDetails().get("alertId"));
            notification.put("alertId", payload.getDetails().get("alertId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("notificationId"))) {
            data.put("NotificationId", payload.getDetails().get("notificationId"));
            notification.put("NotificationId", payload.getDetails().get("notificationId"));
        }

        if (!StringUtils.isEmpty(alert)) {
            if (payload.getActivity().equalsIgnoreCase("SEARCHALERTS")) {
                JSONObject titleBody = new JSONObject();

                titleBody.put("body", alert);

                data.put("title", titleBody.toString());
            } else {
                data.put("title", alert);
            }

            notification.put("body", alert);
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("conversationId"))) {
            data.put("ConversationId", payload.getDetails().get("conversationId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("adId"))) {
            data.put("AdId", payload.getDetails().get("adId"));
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("adTitle"))) {
            data.put("AdSubject",payload.getDetails().get("adTitle").toString());
            notification.put("title",payload.getDetails().get("adTitle").toString());
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("adImage"))) {
            data.put("AdImageUrl", payload.getDetails().get("adImage").toString().trim());
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("badge"))) {
            notificationObj.put("badge", payload.getDetails().get("badge").trim());
        }

        if (!StringUtils.isEmpty(payload.getDetails().get("senderId"))) {
            data.put("senderId",payload.getDetails().get("senderId"));
        }

        if (!StringUtils.isEmpty(notificationTitle)) {
            data.put("notificationTitle", notificationTitle);
        }

        // iOS data fields
        notification.put("sound", "default");
        notification.put("action-loc-key", "strViewBtn");

        if (!StringUtils.isEmpty(payload.getActivity()) && payload.getActivity().equalsIgnoreCase("CHATMESSAGE")) {
            obj.put("priority","high");
        } else {
            obj.put("priority", searchAlertsPriority);
        }

        dataObj.putAll(data);

        notificationObj.putAll(notification);

        obj.put("data", dataObj);
        obj.put("notification", notificationObj);

        LOG.debug("GCM Push Message for the APP is ::: {}", obj.toString());

        post.setEntity(new StringEntity(obj.toString(), ContentType.create("application/json", "UTF-8")));
        post.setHeader("Authorization", "key=" + pushHostInfo.getAuthHeader());

        return post;
    }

    @Override
    public HttpResponse sendPush(HttpClient httpClient, PushMessagePayload payload,  String deviceToken, String notificationTitle, PWAInfo pwaInfo) throws Exception {
        HttpPost request = build(payload, deviceToken, notificationTitle);

        LOG.debug("Request to GCM body {}", EntityUtils.toString(request.getEntity()));

        return httpClient.execute(getHttpHost(), request);
    }
}