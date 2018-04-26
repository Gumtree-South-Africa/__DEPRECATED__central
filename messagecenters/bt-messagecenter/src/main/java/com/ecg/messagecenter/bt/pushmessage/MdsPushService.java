package com.ecg.messagecenter.bt.pushmessage;

import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static com.ecg.messagecenter.bt.pushmessage.HttpClientBuilder.buildHttpClient;

public class MdsPushService extends PushService {

    private static final Logger LOG = LoggerFactory.getLogger(MdsPushService.class);

    private final HttpClient httpClient;
    private final HttpHost mdsPushHost;
    private final String mdnsAuthHeader;
    private final String mdnsProvider;
    private final static String mdnsPayload = "<MessagePayload><AlertText>%ALRT%</AlertText><Level1Data><Data><Key>sound</Key><Value>default</Value></Data><Data><Key>badge</Key><Value>%BDG%</Value></Data><Data><Key>Type</Key><Value>3</Value></Data><Data><Key>ConversationId</Key><Value>%CONVID%</Value></Data><Data><Key>AdId</Key><Value>%ADID%</Value></Data><Data><Key>AdSubject</Key><Value>%ADTITLE%</Value></Data><Data><Key>AdImageUrl</Key><Value>%ADIMAGE%</Value></Data></Level1Data></MessagePayload>";

    public MdsPushService(String pushHost, String mdnsAuthHeader, String mdnsProvider) {
        this.httpClient = buildHttpClient(4000, 4000, 8000, 40, 40);
        this.mdsPushHost = new HttpHost(pushHost, 443, "https");
        this.mdnsAuthHeader = mdnsAuthHeader;
        this.mdnsProvider = mdnsProvider;
    }

    public Result sendPushMessage(final PushMessagePayload payload) {
    	HttpPost request = null; 
        try {
        	request = buildRequest(payload);
            LOG.debug("MdsPushService sending: " + payload);
            return httpClient.execute(mdsPushHost, request, new ResponseHandler<Result>() {
                @Override
                public Result handleResponse(HttpResponse response) throws IOException {
                    int code = response.getStatusLine().getStatusCode();
                    
                    LOG.debug("MdsPushService response: " + code);
                    HttpEntity entity = response.getEntity();
                    LOG.debug("response payload" + EntityUtils.toString(entity, "UTF-8"));

                    switch (code) {
                        case 200:
                            return Result.ok(payload);
                        case 404:
                            return Result.notFound(payload);
                        default:
                            // application wise only 200 (sending success) + 404 (non-registered device) make sense to us
                            return Result.error(payload, new RuntimeException("Unexpected response: " + response.getStatusLine()));
                    }
                }
            });
        } catch (Exception e) {
            return Result.error(payload, e);
        } finally {
        	request.releaseConnection();
        }
    }

    private HttpPost buildRequest(PushMessagePayload postData) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("/mobile/mds/v1/sendMessages");
        
        
		String fromEmail = postData.getMessage() !=null ? postData.getMessage().split(":")[0].trim() : null;
		String alert = postData.getMessage() !=null ? postData.getMessage().split(":")[1].trim() : null;
		
		
		String template = mdnsPayload;
		
        //populate payload
		if (alert != null) 
			template = template.replaceAll("%ALRT%",Matcher.quoteReplacement(alert));
		if (postData.getDetails().get("conversationId") != null) 
			template = template.replaceAll("%CONVID%",postData.getDetails().get("conversationId"));
		if (postData.getDetails().get("adId") != null) 
			template = template.replaceAll("%ADID%",postData.getDetails().get("adId"));
		if (postData.getDetails().get("adTitle") != null) 
			template = template.replaceAll("%ADTITLE%",Matcher.quoteReplacement(postData.getDetails().get("adTitle").toString()));
		if (postData.getDetails().get("adImage") != null) 
			template = template.replaceAll("%ADIMAGE%",Matcher.quoteReplacement(postData.getDetails().get("adImage").toString().trim()));
                if (postData.getDetails().get("badge") != null)
			template = template.replaceAll("%BDG%",Matcher.quoteReplacement(postData.getDetails().get("badge").trim()));
                
        Map content = new LinkedHashMap();
        Map message = new LinkedHashMap();
        Map receiver = new LinkedHashMap();
        ArrayList list = new ArrayList();
        
        Map data = new LinkedHashMap();
        receiver.put("Provider", mdnsProvider);
        receiver.put("Type", "User");
        List ids = new ArrayList();
        ids.add(postData.getDetails().get("receiverId"));
        receiver.put("Id", ids);
        receiver.put("EventName", "CHATMESSAGE");
        receiver.put("Domain", "ECG");
        message.put("Receiver", receiver);
        message.put("Data", data);
        data.put("Payload", "<![CDATA[" + template + "]]>");
        data.put("FormatRequired", "true");
        list.add(message);
        content.put("Message", list);
        
        JSONObject obj = new JSONObject();
        obj.putAll(content);
        
        post.setEntity(new StringEntity(obj.toString(), ContentType.create("application/json", "UTF-8")));
        post.setHeader("Authorization", mdnsAuthHeader);
        return post;
    }

}
