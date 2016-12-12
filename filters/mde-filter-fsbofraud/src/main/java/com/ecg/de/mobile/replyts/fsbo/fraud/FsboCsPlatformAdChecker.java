package com.ecg.de.mobile.replyts.fsbo.fraud;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class FsboCsPlatformAdChecker implements AdChecker {
    private final Logger logger = LoggerFactory.getLogger(getClass());

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
            String msg = httpClient.execute(get, new ResponseHandler<String>() {

                @Override
                public String handleResponse(HttpResponse response) throws IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status != HttpStatus.SC_OK) {
                        if (status == HttpStatus.SC_NOT_FOUND) {
                            logger.info("Could not determine status of ad {}. Issue details: {}", adId, response.getStatusLine());
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
                }

            });

            logger.info("Fsbo-cs-platform returned {} for ad {}.", msg, adId);

            if (msg!=null && msg.equals("fraud")) {
                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return false;

    }


}
