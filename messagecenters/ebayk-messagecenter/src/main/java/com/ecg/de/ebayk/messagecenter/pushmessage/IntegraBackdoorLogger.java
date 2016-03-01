package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.ecg.de.ebayk.messagecenter.util.AbsoluteBackdoorServerUrl;
import com.google.common.collect.Lists;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static com.ecg.de.ebayk.messagecenter.pushmessage.HttpClientBuilder.buildHttpClient;
import static org.apache.http.util.EntityUtils.consume;

/**
 * @author fsemrau
 */
class IntegraBackdoorLogger {
    private static final HttpClient LOGGING_CLIENT = buildHttpClient(4000, 4000, 8000, 40, 40);

    private static final Logger LOG = LoggerFactory.getLogger(IntegraBackdoorLogger.class);


    boolean isProdEnvironment() {
        String name = System.getProperty("app.env.name", "prod").toUpperCase();

        return name.equals("PROD");
    }

    void notice(PushMessagePayload payload) {
        if (isProdEnvironment()) {
            return;
        }

        try {
            List<NameValuePair> params = Lists.newArrayList();
            params.add(new BasicNameValuePair("email", payload.getEmail()));

            params.add(new BasicNameValuePair("activity", payload.getActivity()));
            params.add(new BasicNameValuePair("message", payload.getMessage()));
            if (payload.getAlertCounter().isPresent()) {
                params.add(new BasicNameValuePair("alertCounter", payload.getAlertCounter().get().toString()));
            }


            for (Map.Entry<String, String> detailEntry : payload.getDetails().entrySet()) {
                params.add(new BasicNameValuePair("details[" + detailEntry.getKey() + "]", detailEntry.getValue()));
            }

            if (payload.getApnsDetails().isPresent()) {
                for (Map.Entry<String, String> detailEntry : payload.getApnsDetails().get().entrySet()) {
                    params.add(new BasicNameValuePair("apnsDetails[" + detailEntry.getKey() + "]", detailEntry.getValue()));
                }
            }

            if (payload.getGcmDetails().isPresent()) {
                for (Map.Entry<String, String> detailEntry : payload.getGcmDetails().get().entrySet()) {
                    params.add(new BasicNameValuePair("gcmDetails[" + detailEntry.getKey() + "]", detailEntry.getValue()));
                }
            }

            HttpPost post = new HttpPost(new AbsoluteBackdoorServerUrl("/log/pushnotifications").getUrl());
            post.setEntity(new UrlEncodedFormEntity(params));
            consume(LOGGING_CLIENT.execute(post).getEntity());
        } catch (Exception e) {
            LOG.warn("backdoor server notify failed", e);
        }
    }
}
