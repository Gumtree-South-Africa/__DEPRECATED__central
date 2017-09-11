package com.ecg.messagecenter.pushmessage;

import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import com.google.common.io.CharStreams;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

public class AdInfoLookup {
    private static final Logger LOG = LoggerFactory.getLogger(AdInfoLookup.class);

    private final CloseableHttpClient httpClient;
    private final HttpHost kmobilepushHost;

    public AdInfoLookup(String kapiHost, Integer kapiPort) {
        // very low timeouts to not hurt backend
        this.httpClient = HttpClientFactory.createCloseableHttpClient(1000, 1000, 2000, 40, 40);
        this.kmobilepushHost = new HttpHost(kapiHost, kapiPort);
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }

    Optional<AdInfo> lookupAdIInfo(Long adId) {
        try {
            HttpRequest request = buildRequest(adId);
            return httpClient.execute(kmobilepushHost, request, new AdInfoResponseHandler());
        } catch (Exception e) {
            LOG.error("Error fetching image-url for ad #" + adId + " " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    private HttpRequest buildRequest(Long adId) throws UnsupportedEncodingException {
        //TODO how to get pictures and title only
        //HttpGet get = new HttpGet("/api/ads/" + adId + ".json?_in=pictures");

        return new HttpGet("/api/ads/" + adId + ".json");
    }

    static class AdInfoResponseHandler implements ResponseHandler<Optional<AdInfo>> {
        @Override
        public Optional<AdInfo> handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode();
            switch (code) {
                case 200:
                    return lookupInfoFromResponse(response);
                case 404:
                    return Optional.empty();
                default:
                    return Optional.empty();
            }
        }

        private static Optional<AdInfo> lookupInfoFromResponse(HttpResponse response) throws IOException {
            AdInfo adInfo = new AdInfo();
            String jsonAsString = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

            JSONObject titleLevel = json
                    .getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad")
                    .getJSONObject("value")
                    .getJSONObject("title");
            JSONObject pictureLevel = json
                    .getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad")
                    .getJSONObject("value")
                    .getJSONObject("pictures");

            if (pictureLevel.containsKey("picture")) {
                JSONArray pictures = pictureLevel.getJSONArray("picture").getJSONObject(0).getJSONArray("link");
                for (int i = 0; i < pictures.size(); i++) {
                    if (pictures.getJSONObject(i).getString("rel").equals("canonicalUrl")) {
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