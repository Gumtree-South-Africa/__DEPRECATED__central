package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.codahale.metrics.Timer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsApiNotifier {
    private static final Logger LOG = LoggerFactory.getLogger(Replyts2StatsNotifier.class);
    private final AsyncHttpClient asyncHttpClient;

    private final String statsUrl;

    public StatsApiNotifier(String statsUrl, AsyncHttpClient asyncHttpClient) {
        this.statsUrl = statsUrl;
        this.asyncHttpClient = asyncHttpClient;
    }

    public void sendAsyncNotification(String buyerId, String adId, Timer requestStatsTimer) {
        Timer.Context timerContext = requestStatsTimer.time();

        String url = String.format("%s/advert-stats/%s/counts/reply", statsUrl, adId);

        asyncHttpClient.preparePost(url)
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .setFormParams(ImmutableMap.of("from", ImmutableList.of(buyerId)))
                .execute(createCompletionHandler(timerContext));
    }

    private AsyncCompletionHandler<Response> createCompletionHandler(final Timer.Context timerContext) {
        return new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                int statusCode = response.getStatusCode();
                if (statusCode != 200) {
                    LOG.warn("Stats response, while trying to send advert stats, was not ok. " +
                            "Status code is: {}", statusCode);
                }
                timerContext.stop();
                return response;
            }

            @Override
            public void onThrowable(Throwable t) {
                LOG.warn(t.getMessage());
                timerContext.stop();
                super.onThrowable(t);
            }
        };
    }
}
