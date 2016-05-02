package com.ecg.de.ebayk.messagecenter.capi;

import com.codahale.metrics.Counter;
import com.ecg.de.ebayk.messagecenter.pushmessage.exception.APIException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.io.CharStreams;
import net.sf.json.JSONArray;
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

import static com.ecg.de.ebayk.messagecenter.capi.HttpClientBuilder.buildHttpClient;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

public class AdInfoLookup {

    private static final Counter AD_INFO_LOOKUP_FAILED = TimingReports.newCounter("message-box.adInfoLookup.failed");
    private static final Logger LOG = LoggerFactory.getLogger(AdInfoLookup.class);

    private final HttpClient httpClient;
    private final HttpHost commonApiHost;
    private final CredentialsProvider credentialsProvider;

    public AdInfoLookup(Configuration configuration) {
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

    public Optional<AdInfo> lookupAdInfo(Long adId) {
        try {
            HttpRequest request = buildRequest(adId);
            return httpClient.execute(commonApiHost, request, new AdInfoResponseHandler(), getHttpContext());
        } catch (IOException e) {
            LOG.error("Error fetching adInfo for ad [{}]", adId, e);
            AD_INFO_LOOKUP_FAILED.inc();
            return Optional.empty();
        }
    }

    private HttpRequest buildRequest(Long adId) throws UnsupportedEncodingException {
        return new HttpGet("/api/ads/" + adId + ".json?_in=title,pictures");
    }

    static class AdInfoResponseHandler implements ResponseHandler<Optional<AdInfo>> {
        @Override
        public Optional<AdInfo> handleResponse(HttpResponse response) throws IOException {
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

        private static AdInfo lookupInfoFromResponse(HttpResponse response) throws IOException {
            AdInfo adInfo = new AdInfo();
            String jsonAsString = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

            JSONObject pictureLevel = json.getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad").getJSONObject("value").getJSONObject("pictures");

            if (pictureLevel.has("picture") && pictureLevel.get("picture") instanceof JSONArray) {
                JSONArray pictures = pictureLevel.getJSONArray("picture").getJSONObject(0).getJSONArray("link");
                for (int i = 0; i < pictures.size(); i++) {
                    if (pictures.getJSONObject(i).getString("rel").equals("large")) {
                        adInfo.setImageUrl(pictures.getJSONObject(i).getString("href"));
                    }
                }
            } else {
                adInfo.setImageUrl("");
            }

            JSONObject titleLevel = json.getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad").getJSONObject("value").getJSONObject("title");
            adInfo.setTitle(titleLevel.getString("value"));

            return adInfo;
        }
    }

    public static class AdInfo {
        private String title;
        private String imageUrl;

        public String getTitle() {
            return title;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
