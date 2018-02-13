package com.ebay.ecg.bolt.domain.service.push.model;

import com.google.common.base.Stopwatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Security;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@RunWith(MockitoJUnitRunner.class)
public class PWARequesterTest {
    private static final Logger LOG = LoggerFactory.getLogger(PWARequesterTest.class);

    private static final Locale LOCALE = new Locale("en","ZA");

    private static final String DEVICE_TOKEN = "token1";
    private static final String NOTIFICATION_TITLE = "You got a message!";

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse response;

    @Before
    public void init() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void testPWASendPushMessages() throws Exception {
        Map<String, String> details = Collections.singletonMap("locale", LOCALE.toString());

        PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "Hello", "heya", details);

        PWAInfo pwaInfo = new PWAInfo(
          "https://android.googleapis.com/gcm/send/d_msK35UBT0:APA91bH1SjLpPri6_fmKm6-m2BOc-vtcrh-1TZukXfhi62WpRRLmnMtJRWViFXqjQYO0rqoLXkzHyodGwV49mRuvSXFLt6sOuNUf87urndNOgxi_6UyhFf4wG4O8dN4dUdy24FCIzplS",
          "BB5bKjcRawntzacxKXRVMhfS60h_48ZVHWZDTEbrVufrtwsol4dMNxKvGw8HSpd770MkWi76ovbBj_mJBiLQ1SA=",
          "px9ZH3w7m8tk8zuJxmeEng==",
          "pwa");

        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        PWARequester pwaRequester = new PWARequester(new PushHostInfo("123", "123", "123"));

        ExecutorService executor = Executors.newFixedThreadPool(5);

        List<Future<HttpResponse>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            futures.add(executor.submit(() -> {
                Stopwatch timer = Stopwatch.createStarted();

                HttpResponse response = pwaRequester.sendPush(httpClient, pushMessagePayload, DEVICE_TOKEN, NOTIFICATION_TITLE, pwaInfo);

                LOG.info("Test :: Time take for the pwa push is {}", timer.stop());

                return response;
            }));
        }

        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                LOG.error("Unable to retrieve future", e);
            } finally {
                executor.shutdown();
            }
        });
    }
}