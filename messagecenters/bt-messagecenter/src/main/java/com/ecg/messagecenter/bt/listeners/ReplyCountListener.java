package com.ecg.messagecenter.bt.listeners;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class ReplyCountListener implements MessageProcessedListener {
    private static final Logger LOG = LoggerFactory.getLogger(ReplyCountListener.class);

    private static final int HTTP_CONNECTION_TIMEOUT = 5000;
    private static final int HTTP_SO_TIMEOUT = 60000;
    private static final int HTTP_MAX_CONNECTIONS = 50;

    private RestTemplate restTemplate;

    private Boolean isReplyCountEnabled = false;

    private String incrementCountUrl;

    private static String jsonRequest;

    static {
        try {
            JSONObject reply = new JSONObject();
            reply.put("name", "ReplyCount");

            // the empty value property defines an increment of 1
            reply.put("value", "");

            JSONObject result = new JSONObject();
            result.put("counters", new JSONArray(Collections.singletonList(reply)));

            jsonRequest = result.toString();
        }
        catch (JSONException ex) {
            throw new RuntimeException("An exception has occurred creating the JSON request for ReplyCountListener.", ex);
        }
    }

    @Autowired
    public ReplyCountListener(@Value("${reply.count.enabled}") Boolean enableReplyCount, @Value("${bapi.increment.count.url}") String incrementCountUrl) {
        checkNotNull(enableReplyCount);
        checkNotNull(incrementCountUrl);

        LOG.debug("Reply count API enabled :{}, BAPI increment count Url :{}", enableReplyCount, incrementCountUrl);

        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient()));
        this.restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
        this.incrementCountUrl = incrementCountUrl;
        this.isReplyCountEnabled = enableReplyCount;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (message.getState() != MessageState.SENT) {
            return;
        }

        if (isReplyCountEnabled) {
            Map<String, String> headers = message.getHeaders();

            String adId = headers.get("X-Cust-Reply-Adid");
            String convId = headers.get("X-Cust-Conversation_Id");
            String locale = headers.get("X-Cust-Locale");

            if (isValidInitialRequest(convId,adId,locale)) {
                incrementReplyCount(adId, locale);
            }else{
                LOG.debug("Ignoring the reply count request for the AdId:{}, Conversation Id:{}, locale:{}, seller id:{}, buyer id:{} and message direction:{}",
                  adId,convId,locale, conversation.getCustomValues().get("mc-sellerid"), conversation.getCustomValues().get("mc-buyerid"),message.getMessageDirection().name());
            }
        }
    }

    private boolean isValidInitialRequest(String convId, String adId, String locale) {
        return convId == null && StringUtils.hasText(adId) && StringUtils.hasText(locale);
    }

    private void incrementReplyCount(String adId, String locale) {
        try {
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("X-BOLT-APPS-ID", "BOLT");
            headers.add("X-BOLT-SITE-LOCALE", locale);

            HttpEntity<String> request = new HttpEntity<>(jsonRequest, headers);

            URI uri = UriComponentsBuilder.fromHttpUrl(String.format(incrementCountUrl,adId)).build().toUri();

            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.PATCH, request, String.class);

            LOG.debug("Response code from BAPI for the increment reply count for the AdId:{} is {} ", adId, response.getStatusCode());
        } catch (Exception e) {
            LOG.error("Exception while making the increment reply count for the AdId:{}", adId, e);
        }
    }

    private HttpClient httpClient() {
        return HttpClients.custom()
          .setDefaultRequestConfig(RequestConfig.custom()
            .setConnectTimeout(HTTP_CONNECTION_TIMEOUT)
            .setSocketTimeout(HTTP_SO_TIMEOUT)
            .build())
          .setMaxConnPerRoute(HTTP_MAX_CONNECTIONS)
          .setMaxConnTotal(HTTP_MAX_CONNECTIONS * 2)
          .useSystemProperties()
          .build();
    }
}