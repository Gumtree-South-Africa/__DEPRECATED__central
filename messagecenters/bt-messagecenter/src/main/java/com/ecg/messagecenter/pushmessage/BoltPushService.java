package com.ecg.messagecenter.pushmessage;



import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ecg.messagecenter.util.JsonUtils;

/*
 * kemo
 */
public class BoltPushService extends PushService {

    private static final Logger LOG = LoggerFactory.getLogger(BoltPushService.class);

    private final HttpClient httpClient;
    private final HttpHost pushServiceHost;

    public BoltPushService(String pushServiceHost, Integer pushServicePort) {
        this.httpClient = HttpClientBuilder.buildHttpClient(4000, 4000, 8000, 40, 40);
        this.pushServiceHost = new HttpHost(pushServiceHost, pushServicePort);
    }

    public Result sendPushMessage(final PushMessagePayload payload) {

        try {

            LOG.debug("BoltPushService sending: " + payload.asJson());
            HttpRequest request = buildRequest(payload);
            LOG.debug("TargetURL:" + request.getRequestLine().getUri() + "BoltPushService sending: " + payload.asJson());
            return httpClient.execute(pushServiceHost, request, new ResponseHandler<Result>() {
                @Override
                public Result handleResponse(HttpResponse response) throws IOException {
                    int code = response.getStatusLine().getStatusCode();
                    
                    LOG.debug("BoltPushService response: " + code);
                    
                    HttpEntity entity = response.getEntity();
                    LOG.debug("BoltPushService response payload" + EntityUtils.toString(entity, "UTF-8"));
                    
                    switch (code) {
                        case 200:
                        case 201:
                            return Result.ok(payload);
                        case 404:
                            return Result.notFound(payload);
                        default:
                            // application wise only 200 (sending success) + 404 (non-registered device) make sense to us
                            return Result.error(payload, new IllegalStateException ("Unexpected response: " + response.getStatusLine()));
                    }
                }
            });


        } catch (Exception e) {
            return Result.error(payload, e);
        }
    }

    private HttpRequest buildRequest(PushMessagePayload payload) throws UnsupportedEncodingException {
    	String receivedId = payload.getDetails().get("receiverId");
    	String locale = payload.getDetails().get("locale");
    	
		if(StringUtils.isEmpty(receivedId)) {
    		LOG.debug("No received Id found ");
    		throw new IllegalArgumentException("No receivedId found");
    	}
		
		if(StringUtils.isEmpty(locale)) {
    		LOG.debug("No locale found ");
    		throw new IllegalArgumentException("No locale found");
    	}
		
        HttpPost post = new HttpPost("/v1.0.0/ps/" + receivedId.trim() +"/CHATMESSAGE/"+ locale.trim()+"/notifications");

        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setToEmail(payload.getEmail());
        notificationRequest.setMessage(payload.getMessage());
        
        Meta meta = new Meta();
        meta.setAdId(payload.getDetails().get("adId"));
        meta.setAdThumbNail(payload.getDetails().get("adImage"));
        meta.setAdTitle(payload.getDetails().get("adTitle"));
        meta.setBadge(payload.getDetails().get("badge"));
        meta.setConversationId(payload.getDetails().get("conversationId"));
        meta.setReceiverUserId(receivedId);
        meta.setSenderId(payload.getDetails().get("senderId"));
        
        if (payload.getMessage().contains(":")) {
           String [] token = payload.getMessage().split(":");
           meta.setSenderDisplayName(token[0]);
        } else {
           meta.setSenderDisplayName("Gumtree User");
        }
        
        notificationRequest.setMeta(meta);
        
        String content = JsonUtils.toJson(notificationRequest);
        post.setEntity(new StringEntity(content, ContentType.create("application/json", "UTF-8")));
        return post;
    }

}
