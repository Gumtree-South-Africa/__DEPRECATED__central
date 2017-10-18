package com.ecg.messagecenter.capi;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.io.CharStreams;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class UserInfoLookup extends InfoLookup<UserInfoLookup.UserInfo> {

    private static final Counter USER_INFO_LOOKUP_FAILED = TimingReports.newCounter("message-box.userInfoLookup.failed");

    public UserInfoLookup(HttpClientConfig httpClientConfig, CommonApiConfig commonApiConfig) {
        super(httpClientConfig, commonApiConfig);
    }

    @Override
    protected HttpRequest buildRequest(String email) throws UnsupportedEncodingException {
        return new HttpGet("/api/users/" + email + "/profile.json");
    }

    @Override
    protected Counter getCounter() {
        return USER_INFO_LOOKUP_FAILED;
    }

    @Override
    protected UserInfo lookupInfoFromResponse(HttpResponse response) throws IOException {
        String jsonAsString = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

        JSONObject userIdLevel = json.getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/user/v1}user-profile").getJSONObject("value").getJSONObject("user-id");

        return new UserInfo(
                userIdLevel.getString("value")
        );
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
