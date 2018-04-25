package com.ecg.messagecenter.bt.pushmessage;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author kemo
 */
public class InfoPoolPushService extends PushService {

    private static final Logger LOG = LoggerFactory.getLogger(InfoPoolPushService.class);

    private final HttpClient httpClient;
    private final HttpClient authClient;
    private final HttpHost pushHost;
    private final String username = "kpi_api1";
    private final String password = "12asDeA$";

    public InfoPoolPushService(String pushHost, String proxyHost, Integer proxyPort) {
        this.httpClient = HttpClientFactory.createCloseableHttpClientWithProxy(4000, 4000, 8000, 40, 40, proxyHost, proxyPort);
        this.authClient = HttpClientFactory.createCloseableHttpClientWithProxy(4000, 4000, 8000, 40, 40, proxyHost, proxyPort);
        this.pushHost = new HttpHost(pushHost);
    }

    public Result sendPushMessage(final PushMessagePayload payload) {
    	
        try {
        	final HttpPost request = buildRequest(payload);
            return httpClient.execute(pushHost, request, new ResponseHandler<Result>() {
                @Override
                public Result handleResponse(HttpResponse response) throws IOException {
                    int code = response.getStatusLine().getStatusCode();
                    switch (code) {
                        case 200:
                            return Result.ok(payload);
                        case 404:
                            return Result.notFound(payload);
                        case 401:
                        	return sendCredential(request, response, payload);
                        default:
                            // application wise only 200 (sending success) + 404 (non-registered device) make sense to us
                            return Result.error(payload, new RuntimeException("Unexpected response: " + response.getStatusLine()));
                    }
                }
            });


        } catch (Exception e) {
            return Result.error(payload, e);
        } 
    }
    
    private Result sendCredential(HttpPost request, HttpResponse response, PushMessagePayload payload) {
    	try {
    		// send the credential
    		Header authHeader = response.getFirstHeader(AUTH.WWW_AUTH);
    		LOG.debug("authHeader = " + authHeader);

    		DigestScheme digestScheme = new DigestScheme();

    		//Parse realm, nonce sent by server. 
    		digestScheme.processChallenge(authHeader);

    		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, password);
    		request.addHeader(digestScheme.authenticate(creds, request));
    		
    		LOG.debug("PushMessagePayload: " + payload.toString());
    		
    		String fromEmail = payload.getMessage() !=null ? payload.getMessage().split(":")[0].trim() : null;
    		String message = payload.getMessage() !=null ? payload.getMessage().split(":")[1].trim() : null;
    		
    		LOG.debug("Push: fromEmail: " + fromEmail + " message: " + message);

    		String body = "<notification:notify-to-user xmlns:notification=\"http://www.ebayclassifiedsgroup.com/schema/notification/v1\">" +
    				"<notification:from-email>" + fromEmail + "</notification:from-email>" +
    				"<notification:to-email>" + payload.getEmail() + "</notification:to-email>" + 
    				"<notification:message>" + message + "</notification:message>"+ 
    				"<notification:meta>" +
    				"<notification:meta-key-value key=\"action\">MESSAGE</notification:meta-key-value>"+ 
    				"<notification:meta-key-value key=\"conversation-id\">" + payload.getDetails().get("conversationId") + "</notification:meta-key-value>"+ 
    				"<notification:meta-key-value key=\"ad-id\">" + payload.getDetails().get("adId") + "</notification:meta-key-value>"+ 
    				"</notification:meta>"+ 
    				"</notification:notify-to-user>";
    		
    		LOG.debug("InfoPool outgoing message body" + body);

    		request.setEntity(new StringEntity(body, ContentType.create("application/xml", "UTF-8")));

    		ResponseHandler<String> responseHandler = new BasicResponseHandler();

    		String responseBody = authClient.execute(pushHost, request, responseHandler);
    		if (responseBody !=null && responseBody.contains("SUCCESS")) {
    			return Result.ok(payload);
    		} else {
    			return Result.error(payload, new RuntimeException("Push Failed - responseBody!!" + responseBody));
    		}
    	} catch (Exception e) {
            return Result.error(payload, new RuntimeException("Push Failed !!" + payload.toString() + " " + e.getMessage() ));    		
    	} finally {
    		request.releaseConnection();
    	}
    }

    private HttpPost buildRequest(PushMessagePayload payload) throws UnsupportedEncodingException {
        HttpPost post = new HttpPost("/kpi/v1/notifications/notify-to-user");
        return post;
    }

}
