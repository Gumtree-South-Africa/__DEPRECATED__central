package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.google.common.base.Optional;
import com.google.common.io.CharStreams;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static com.ecg.de.ebayk.messagecenter.pushmessage.HttpClientBuilder.buildHttpClient;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class AdImageLookup {

    private static final Logger LOG = LoggerFactory.getLogger(AdImageLookup.class);

    private final HttpClient httpClient;
    private final HttpHost kmobilepushHost;

    public AdImageLookup(String kapiHost, Integer kapiPort) {
        // very low timeouts to not hurt backend
        this.httpClient = buildHttpClient(1000, 1000, 2000, 40, 40);
        this.kmobilepushHost = new HttpHost(kapiHost, kapiPort);
    }


    public String lookupAdImageUrl(Long adId) {

        try {

            HttpRequest request = buildRequest(adId);
            return httpClient.execute(kmobilepushHost, request, new AdImageUrlResponseHandler());

        } catch (Exception e) {
            LOG.error("Error fetching image-url for ad #" + adId + " " + e.getMessage(), e);
            return "";
        }
    }


    private HttpRequest buildRequest(Long adId) throws UnsupportedEncodingException {
        HttpGet get = new HttpGet("/api/ads/" + adId + ".json?_in=pictures");

        // auth-setting via header much easier as doing yucky handling basic auth aspect in http-client builder
        // todo: currently 'mweb' api-user is used, change this to 'kcron' after api has new user 'kcron' deployed
        get.setHeader("Authorization", "Basic bXdlYjp0aGlmZ3IzNHQ=");
        return get;
    }

    static class AdImageUrlResponseHandler implements ResponseHandler<String> {
        @Override
        public String handleResponse(HttpResponse response) throws IOException {
            int code = response.getStatusLine().getStatusCode();
            switch (code) {
                case 200:
                    return lookupImageUrlFromResponse(response);
                case 404:
                    return "";
                default:
                    return "";
            }
        }

        private static String lookupImageUrlFromResponse(HttpResponse response) throws IOException {
            String jsonAsString = CharStreams.toString(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonAsString);

            JSONObject pictureLevel = json.getJSONObject("{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad").getJSONObject("value").getJSONObject("pictures");
            if (!pictureLevel.containsKey("picture")) {
                return "";
            }

            JSONArray pictures = pictureLevel.getJSONArray("picture").getJSONObject(0).getJSONArray("link");
            for (int i = 0; i < pictures.size(); i++) {
                if (pictures.getJSONObject(i).getString("rel").equals("canonicalUrl")) {
                    return pictures.getJSONObject(i).getString("href");
                }
            }
            return "";
        }
    }

}
