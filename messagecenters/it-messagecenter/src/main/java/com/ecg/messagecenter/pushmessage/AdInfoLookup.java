package com.ecg.messagecenter.pushmessage;

import com.ecg.messagecenter.util.AdUtil;
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
import org.apache.http.client.AuthCache;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HTTP;
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
    private final HttpHost kmobilepushHost;
    private final String basePath;
    private final String username;
    private final String password;
    private String adIdPrefix;
    private String kapiVirtualHost;

    public AdInfoLookup(String kapiVirtualHost, String kapiIp, Integer kapiPort, String basePath, String adIdPrefix,
                    Integer connectionTimeout, Integer connectionManagerTimeout,
                    Integer socketTimeout, Integer maxConnectionsPerHost,
                    Integer maxTotalConnections, String username, String password) {
        this.kapiVirtualHost = kapiVirtualHost;
        this.httpClient = HttpClientFactory.createCloseableHttpClient(connectionTimeout, connectionManagerTimeout, socketTimeout,
                                maxConnectionsPerHost, maxTotalConnections);
        this.kmobilepushHost = new HttpHost(kapiIp, kapiPort);
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
            request.setHeader(HTTP.TARGET_HOST, kapiVirtualHost);
            AuthCache authCache = new BasicAuthCache();
            DigestScheme scheme = new DigestScheme();
            authCache.put(kmobilepushHost, scheme);
            HttpClientContext localcontext = HttpClientContext.create();
            localcontext.getCredentialsProvider().setCredentials(new AuthScope(kmobilepushHost.getHostName(),
                            kmobilepushHost.getPort()),
                    new UsernamePasswordCredentials(username, password));
            localcontext.setAttribute("http.auth.auth-cache", authCache);

            LOG.debug("Calling api host(uri):" + kmobilepushHost.toURI() + " request: " + request);
            return httpClient.execute(kmobilepushHost, request, new AdInfoResponseHandler(),
                            localcontext);
        } catch (Exception e) {
            LOG.error("Error fetching image-url for ad #" + adId + " " + e.getMessage(), e);
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
            LOG.debug("Response code from api: " + code);
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
            LOG.debug("AdInfo lookup json: " + jsonAsString);
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
