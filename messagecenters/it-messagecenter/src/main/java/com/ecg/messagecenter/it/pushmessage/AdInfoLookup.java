package com.ecg.messagecenter.it.pushmessage;

import com.ecg.messagecenter.it.util.AdUtil;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import com.google.common.io.CharStreams;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class AdInfoLookup {

    private static final Logger LOG = LoggerFactory.getLogger(AdInfoLookup.class);

    private final CloseableHttpClient httpClient;
    private final HttpHost capiHost;
    private final String basePath;
    private final String username;
    private final String password;
    private String adIdPrefix;

    public AdInfoLookup(String capiHost, Integer capiPort, String capiProtocol,
                        String capiProxyHost, Integer capiProxyPort,
                        String basePath, String adIdPrefix,
                        Integer connectionTimeout, Integer connectionManagerTimeout,
                        Integer socketTimeout, Integer maxConnectionsPerHost,
                        Integer maxTotalConnections, String username, String password) {
        this.httpClient = HttpClientFactory.createCloseableHttpClientWithProxy(
                connectionTimeout,
                connectionManagerTimeout,
                socketTimeout,
                maxConnectionsPerHost,
                maxTotalConnections,
                capiProxyHost,
                capiProxyPort);
        this.capiHost = HttpHost.create(capiProtocol + "://" + capiHost + ":" + capiPort);
        this.basePath = basePath;
        this.adIdPrefix = adIdPrefix;
        this.username = username;
        this.password = password;
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }

    public Optional<AdInfo> lookupAdIInfo(String adId) {
        try {
            HttpRequest request = buildRequest(getAdIdFrom(adId));
            HttpClientContext clientContext = HttpClientContext.create();

            LOG.debug("Setting user [{}] for basic auth on [{}]", username, capiHost.toURI());
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(capiHost.getHostName(), capiHost.getPort()),
                    new UsernamePasswordCredentials(username, password));
            clientContext.setCredentialsProvider(credentialsProvider);

            LOG.trace("Calling Capi service [{}] - Request: [{}]", capiHost.toURI(), request);
            return httpClient.execute(capiHost, request, new AdInfoResponseHandler(), clientContext);
        } catch (Exception e) {
            LOG.warn("Error fetching image-url for ad #{}: {}", adId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private HttpRequest buildRequest(Long adId) throws UnsupportedEncodingException {
        return new HttpGet("/" + basePath + "/ads/" + adId + ".json?_in=title,pictures");
    }

    public long getAdIdFrom(String adId) {
        return AdUtil.getAdFromMail(adId, adIdPrefix);
    }

    static class AdInfoResponseHandler implements ResponseHandler<Optional<AdInfo>> {
        @Override public Optional<AdInfo> handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode();
            LOG.debug("Response code from api: {}", code);
            switch (code) {
                case 200:
                    return lookupInfoFromResponse(response);
                case 404:
                    return Optional.empty();
                default:
                    return Optional.empty();
            }
        }

        private static Optional<AdInfo> lookupInfoFromResponse(HttpResponse response)
                        throws IOException {
            AdInfo adInfo = new AdInfo();
            String jsonAsString = CharStreams.toString(
                            new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            LOG.debug("AdInfo lookup json: {}", jsonAsString);
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

            JSONObject titleLevel = json.getJSONObject(
                            "{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad")
                            .getJSONObject("value").getJSONObject("title");
            JSONObject pictureLevel = json.getJSONObject(
                            "{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad")
                            .getJSONObject("value").getJSONObject("pictures");

            if (pictureLevel.has("picture") && pictureLevel.get("picture") instanceof JSONArray) {
                JSONArray pictures = pictureLevel.getJSONArray("picture").getJSONObject(0)
                                .getJSONArray("link");
                for (int i = 0; i < pictures.size(); i++) {
                    if (pictures.getJSONObject(i).getString("rel").equals("big")) {
                        adInfo.setImageUrl(pictures.getJSONObject(i).getString("href"));
                    }
                }
            } else {
                adInfo.setImageUrl("");
            }
            adInfo.setTitle(titleLevel.getString("value"));

            return Optional.of(adInfo);
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
