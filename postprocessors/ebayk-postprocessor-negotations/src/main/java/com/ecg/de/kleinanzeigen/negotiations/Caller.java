package com.ecg.de.kleinanzeigen.negotiations;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

class Caller {

    enum NegotationState {
        DELAYED__FILTER_DELAYED,
        ACTIVE__OFFER_DELIVERED
    }

    public static class Payload {
        private final String stateLifecycle;
        private final String conversationId;

        public Payload(String conversationId, String stateLifecycle) {
            this.conversationId = conversationId;
            this.stateLifecycle = stateLifecycle;
        }

        public String getStateLifecycle() {
            return stateLifecycle;
        }

        public String getConversationId() {
            return conversationId;
        }
    }

    private final String basePath;
    private final DefaultHttpClient httpClient = HttpClientBuilder.buildHttpClient(4000, 4000, 8000, 40, 40);


    @Autowired
    Caller(@Value("${api.basepath:http://kapi.mobile.rz/api/}") String basePath) {
        this.basePath = basePath.endsWith("/") ? basePath : basePath+"/";
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, "DIGEST"),
                new UsernamePasswordCredentials("kreplyts", "lqOw7V63qYNuIkzr9N1vdRRo3zpJ"));
        httpClient.setCredentialsProvider(credsProvider);
    }

    void execute(NegotationState state, Conversation conversation, Message message) {

        String negotiationId = message.getHeaders().get("X-Cust-Negotiationid");
        String offerId = message.getHeaders().get("X-Offerid");

        String requestUrl = String.format("%snegotiations/%s/offers/%s", basePath, negotiationId, offerId);

        JSONObject json = new JSONObject();
        json.put("stateLifecycle", state.name());
        json.put("conversationId", conversation.getId());

        HttpPut put = new HttpPut(requestUrl);
        put.setEntity(new StringEntity(json.toString(), ContentType.APPLICATION_JSON));

        try {
            httpClient.execute(put, new ResponseHandler<Object>() {
                @Override
                public Object handleResponse(HttpResponse response) {
                    if (response.getStatusLine().getStatusCode() >= 300) {
                        throw new RuntimeException("Server responded with: " + response.getStatusLine().toString());
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("negotiation activator - error when calling '" + requestUrl + "'", e);
        }
    }


}
