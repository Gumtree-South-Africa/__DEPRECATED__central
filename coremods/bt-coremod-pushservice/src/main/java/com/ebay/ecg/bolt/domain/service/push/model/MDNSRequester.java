package com.ebay.ecg.bolt.domain.service.push.model;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.sf.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDNSRequester extends PushRequester {
    private static final Logger LOG = LoggerFactory.getLogger(MDNSRequester.class);

    private static final String PUSH_ENDPOINT = "/mobile/mds/v1/sendMessages";

    private final static String MDNS_PAYLOAD = "<MessagePayload><AlertText>%ALRT%</AlertText><Level1Data><Data><Key>sound</Key><Value>default</Value></Data><Data><Key>badge</Key><Value>%BDG%</Value></Data><Data><Key>Type</Key><Value>3</Value></Data><Data><Key>ConversationId</Key><Value>%CONVID%</Value></Data><Data><Key>AdId</Key><Value>%ADID%</Value></Data><Data><Key>AdSubject</Key><Value>%ADTITLE%</Value></Data><Data><Key>AdImageUrl</Key><Value>%ADIMAGE%</Value></Data></Level1Data></MessagePayload>";

    private final static String MDNS_SEARCH_ALERT_PAYLOAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
      + "<MessagePayload>"
        + "<AlertText>%ALRT%</AlertText>"
        + "<Level1Data>"
          + "<Data>"
            + "<Key>type</Key>"
            + "<Value>2</Value>"
          + "</Data>"
          + "<Data>"
            + "<Key>userId</Key>"
            + "<Value>%USERID%</Value>"
          + "</Data>"
          + "<Data>"
            + "<Key>alertId</Key>"
            + "<Value>%ALRTID%</Value>"
          + "</Data>"
        + "</Level1Data>"
      + "</MessagePayload>";
    
    public MDNSRequester(PushHostInfo pushHostInfo) {
        super(pushHostInfo);
    }

    public HttpPost build(final PushMessagePayload payload, final String deviceToken, String notificationTitle) throws UnsupportedEncodingException {
        if (payload.getActivity().equalsIgnoreCase("SEARCHALERTS")){
            return buildSearchAlertPayload(payload, deviceToken);
        } else {
            HttpPost post = new HttpPost(PUSH_ENDPOINT);

            String alert = payload.getMessage();
            String template = MDNS_PAYLOAD;

            if (alert != null) {
                template = template.replaceAll("%ALRT%", Matcher.quoteReplacement(alert));
            }
            if (payload.getDetails().get("conversationId") != null) {
                template = template.replaceAll("%CONVID%", payload.getDetails().get("conversationId"));
            }
            if (payload.getDetails().get("adId") != null) {
                template = template.replaceAll("%ADID%", payload.getDetails().get("adId"));
            }
            if (payload.getDetails().get("adTitle") != null) {
                template = template.replaceAll("%ADTITLE%", Matcher.quoteReplacement(payload.getDetails().get("adTitle").toString()));
            }
            if (payload.getDetails().get("adImage") != null) {
                template = template.replaceAll("%ADIMAGE%", Matcher.quoteReplacement(payload.getDetails().get("adImage").toString().trim()));
            }
            if (payload.getDetails().get("badge") != null) {
                template = template.replaceAll("%BDG%", Matcher.quoteReplacement(payload.getDetails().get("badge").trim()));
            }

            Map<String, Object> receiver = new LinkedHashMap<>();

            receiver.put("Provider", pushHostInfo.getProvider());
            receiver.put("Type", "User");

            List<String> ids = new ArrayList<>();

            ids.add(payload.getDetails().get("receiverId"));

            receiver.put("Id", ids);
            receiver.put("EventName", "CHATMESSAGE");
            receiver.put("Domain", "ECG");

            Map<String, Object> message = new LinkedHashMap<>();

            message.put("Receiver", receiver);

            Map<String, Object> data = new LinkedHashMap();

            data.put("Payload", "<![CDATA[" + template + "]]>");
            data.put("FormatRequired", "true");

            message.put("Data", data);

            List<Map<String, Object>> list = new ArrayList<>();

            list.add(message);

            Map<String, Object> content = new LinkedHashMap<>();

            content.put("Message", list);

            JSONObject obj = new JSONObject();

            obj.putAll(content);

            LOG.debug("MDNS CHATMESSAGE Message is ::: {}", obj);

            post.setEntity(new StringEntity(obj.toString(), ContentType.create("application/json", "UTF-8")));
            post.setHeader("Authorization", pushHostInfo.getAuthHeader());

            return post;
        }
    }

    private HttpPost buildSearchAlertPayload(PushMessagePayload payload, String deviceToken) {
        HttpPost post = new HttpPost(PUSH_ENDPOINT);

        String alert = payload.getMessage();
        String template = MDNS_SEARCH_ALERT_PAYLOAD;

        if (alert != null) {
            String alertWithBodyTag ="{\"body\":\""+ alert+ "\",\"action-loc-key\":\"strViewBtn\"}";

            template = template.replaceAll("%ALRT%",Matcher.quoteReplacement(alertWithBodyTag));
        }

        if (payload.getDetails().get("receiverId") != null) {
            template = template.replaceAll("%USERID%", payload.getDetails().get("receiverId"));
        }

        if (payload.getDetails().get("alertId") != null) {
            template = template.replaceAll("%ALRTID%", payload.getDetails().get("alertId"));
        }

        List<String> ids = new ArrayList<>();

        ids.add(payload.getDetails().get("receiverId"));

        Map<String, Object> receiver = new LinkedHashMap<>();

        receiver.put("Domain", "ECG");
        receiver.put("EventName", "SEARCHALERTS");
        receiver.put("Id", ids);
        receiver.put("Provider", pushHostInfo.getProvider());
        receiver.put("Type", "User");

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("Payload", "<![CDATA[" + template + "]]>");
        data.put("FormatRequired", "true");

        Map<String, Map<String, Object>> message = new LinkedHashMap<>();

        message.put("Receiver", receiver);
        message.put("Data", data);
        
        List<Map<String, Map<String, Object>>> list = new ArrayList<>();

        list.add(message);

        Map<String, List<Map<String, Map<String, Object>>>> content = new LinkedHashMap<>();

        content.put("Message", list);

        JSONObject obj = new JSONObject();

        obj.putAll(content);
        
        LOG.debug("MDNS SEARCHALERTS Message is ::: {}", obj);
        
        post.setEntity(new StringEntity(obj.toString(), ContentType.create("application/json", "UTF-8")));
        post.setHeader("Authorization", pushHostInfo.getAuthHeader());

        return post;
    }

    @Override
    public HttpResponse sendPush(HttpClient httpClient, PushMessagePayload payload, String deviceToken, String notificationTitle, PWAInfo pwaInfo) throws Exception {
        HttpPost request = null;

        try {
            request = build(payload, deviceToken, notificationTitle);

            LOG.debug("Request to MDNS body {}", EntityUtils.toString(request.getEntity()));

            return httpClient.execute(getHttpHost(), request);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }
}
