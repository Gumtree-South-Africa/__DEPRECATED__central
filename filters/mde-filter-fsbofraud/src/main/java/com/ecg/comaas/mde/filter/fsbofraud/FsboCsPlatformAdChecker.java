package com.ecg.comaas.mde.filter.fsbofraud;


import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;


@Component
public class FsboCsPlatformAdChecker implements AdChecker {
    private final static Logger LOG = LoggerFactory.getLogger(FsboCsPlatformAdChecker.class);

    private final HttpClient httpClient;

    private final String fsboCsWebserviceUrl;


    public FsboCsPlatformAdChecker(@Value("${replyts.mobile.fsbo.fraud.fsboCsWebserviceUrl}") String fsboCsWebserviceUrl) {
        this.fsboCsWebserviceUrl = fsboCsWebserviceUrl.endsWith("/") ? fsboCsWebserviceUrl : fsboCsWebserviceUrl + "/";
        this.httpClient = new DefaultHttpClient(new PoolingClientConnectionManager());
    }

    @Override
    public boolean isFraud(long adId) {
        HttpGet get = new HttpGet(fsboCsWebserviceUrl + "status/" + adId);
        try {
            String msg = httpClient.execute(get, response -> {
                int status = response.getStatusLine().getStatusCode();
                if (status != HttpStatus.SC_OK) {
                    if (status == HttpStatus.SC_NOT_FOUND) {
                        LOG.info("Could not determine status of ad {}. Issue details: {}", adId, response.getStatusLine());
                    } else {
                        throw new ClientProtocolException("Server returned status " + status + ".");
                    }
                }

                HttpEntity entity = response.getEntity();
                if (entity == null) {
                    throw new ClientProtocolException("Got empty response.");
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
                return reader.readLine();
            });

            LOG.trace("Fsbo-cs-platform returned {} for ad {}.", msg, adId);

            if (msg != null && msg.equals("fraud")) {
                return true;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return false;
    }
}
