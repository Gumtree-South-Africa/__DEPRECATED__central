package com.ecg.messagecenter.capi;

import com.codahale.metrics.Counter;
import com.ecg.messagecenter.pushmessage.exception.APIException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.io.CharStreams;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import static com.ecg.messagecenter.capi.HttpClientBuilder.buildHttpClient;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;


public class UserInfoLookup {

    private static final Counter USER_INFO_LOOKUP_FAILED = TimingReports.newCounter("message-box.userInfoLookup.failed");
    private static final Logger LOG = LoggerFactory.getLogger(UserInfoLookup.class);

    private final HttpClient httpClient;
    private final HttpHost commonApiHost;
    private final CredentialsProvider credentialsProvider;

    public UserInfoLookup(Configuration configuration) {
        this.commonApiHost = new HttpHost(configuration.commonApi.hostname, configuration.commonApi.port);
        this.credentialsProvider = new BasicCredentialsProvider();
        this.credentialsProvider.setCredentials(
                new AuthScope(commonApiHost),
                new UsernamePasswordCredentials(configuration.commonApi.username, configuration.commonApi.password)
        );

        this.httpClient = buildHttpClient(configuration.httpClient);
    }

    private HttpContext getHttpContext() {
        HttpContext context = new BasicHttpContext();
        context.setAttribute(ClientContext.CREDS_PROVIDER, credentialsProvider);
        return context;
    }

    public Optional<UserInfo> lookupUserInfo(String email) {
        try {
            HttpRequest request = buildRequest(email);
            return httpClient.execute(commonApiHost, request, new UserInfoResponseHandler(), getHttpContext());
        } catch (IOException e) {
            LOG.error("Error fetching userInfo for user [{}]", email, e);
            USER_INFO_LOOKUP_FAILED.inc();
            return Optional.empty();
        }
    }


    private HttpRequest buildRequest(String email) throws UnsupportedEncodingException {
        return new HttpGet("/api/users/" + email + "/profile.json");
    }

    public static class UserInfoResponseHandler implements ResponseHandler<Optional<UserInfo>> {

        @Override
        public Optional<UserInfo> handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode();
            switch (code) {
                case SC_OK:
                    return Optional.of(lookupInfoFromResponse(response));
                case SC_NOT_FOUND:
                    return Optional.empty();
                default:
                    throw new APIException(EntityUtils.toString(response.getEntity()));
            }
        }

        private static UserInfo lookupInfoFromResponse(HttpResponse response) throws IOException {
            String jsonAsString = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

            JSONObject userIdLevel = json.getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/user/v1}user-profile").getJSONObject("value").getJSONObject("user-id");

            return new UserInfo(
                    userIdLevel.getString("value")
            );
        }
    }

    public static class UserInfo {
        private String userId;

        public UserInfo(String userId) {
            this.userId = userId;
        }

        public String getUserId() {
            return userId;
        }

    }
}
