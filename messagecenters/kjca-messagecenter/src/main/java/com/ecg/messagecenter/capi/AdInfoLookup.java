package com.ecg.messagecenter.capi;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.io.CharStreams;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class AdInfoLookup extends InfoLookup<AdInfoLookup.AdInfo> {

    private static final Counter AD_INFO_LOOKUP_FAILED = TimingReports.newCounter("message-box.adInfoLookup.failed");

    public AdInfoLookup(HttpClientConfig httpClientConfig, CommonApiConfig commonApiConfig) {
        super(httpClientConfig, commonApiConfig);
    }

    @Override
    protected HttpRequest buildRequest(String adId) throws UnsupportedEncodingException {
        return new HttpGet("/api/ads/" + adId + ".json?_in=title,pictures");
    }

    @Override
    protected Counter getCounter() {
        return AD_INFO_LOOKUP_FAILED;
    }

    @Override
    protected AdInfoLookup.AdInfo lookupInfoFromResponse(HttpResponse response) throws IOException {
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

    public static class AdInfo {

        private String title;
        private String imageUrl;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }
    }
}
