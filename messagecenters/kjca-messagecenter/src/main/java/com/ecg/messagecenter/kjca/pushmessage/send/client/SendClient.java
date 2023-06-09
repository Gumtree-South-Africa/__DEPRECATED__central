package com.ecg.messagecenter.kjca.pushmessage.send.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.kjca.pushmessage.send.model.SendMessage;
import com.ecg.messagecenter.kjca.pushmessage.send.model.Subscription;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;
import java.util.List;

import static com.ecg.messagecenter.kjca.pushmessage.send.client.CloseableHttpClientBuilder.aCloseableHttpClient;

/**
 * Service layer to manage subscription requests with SEND
 */
public class SendClient {
    private static final String ACTION_SEND_MESSAGE = "send.client.send-message";
    private static final String ACTION_HAS_SUBSCRIPTION = "send.client.check-subscription";

    private final int hystrixTimeout;
    private final HttpClient httpClient;
    private final HttpHost httpHost;

    private final Timer sendMessageTimer;
    private final Timer checkSubscriptionTimer;

    public SendClient(
            Integer maxRetries,
            Integer maxConnections,
            Integer hystrixTimeout,
            Integer socketTimeout,
            Integer connectTimeout,
            Integer connectionRequestTimeout,
            String httpSchema,
            String httpEndpoint,
            Integer httpPort) {

        this.hystrixTimeout = hystrixTimeout;
        this.httpClient = createPooledHttpClient(maxRetries, TimingReports.newCounter("send.client.general.retries"), maxConnections, socketTimeout, connectTimeout, connectionRequestTimeout);
        this.httpHost = new HttpHost(httpEndpoint, httpPort, httpSchema);

        sendMessageTimer = TimingReports.newTimer(ACTION_SEND_MESSAGE);
        checkSubscriptionTimer = TimingReports.newTimer(ACTION_HAS_SUBSCRIPTION);
    }

    private CloseableHttpClient createPooledHttpClient(int maxRetries, Counter retryCounter, int maxConnections, int socketTimeout, int connectTimeout, int connectionRequestTimeout) {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(maxConnections);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnections);

        TimingReports.newGauge("send.client.conn-available", () -> poolingHttpClientConnectionManager.getTotalStats().getAvailable());
        TimingReports.newGauge("send.client.conn-leased", () -> poolingHttpClientConnectionManager.getTotalStats().getLeased());
        TimingReports.newGauge("send.client.conn-max", () -> poolingHttpClientConnectionManager.getTotalStats().getMax());

        final DefaultHttpRequestRetryHandler retryHandler = new ZealousHttpRequestRetryHandler(maxRetries, retryCounter);

        return aCloseableHttpClient()
                .withConnectionManager(poolingHttpClientConnectionManager)
                .withHttpRequestRetryHandler(retryHandler)
                .withSocketTimeout(socketTimeout)
                .withConnectionTimeout(connectTimeout)
                .withConnectionManagerTimeout(connectionRequestTimeout)
                .build();
    }

    public SendMessage sendMessage(@Nonnull SendMessage messageRequest) {
        try (Timer.Context ignored = sendMessageTimer.time()) {
            SendMessageCommand command = new SendMessageCommand(httpClient, httpHost, messageRequest, hystrixTimeout);
            final SendMessage response = command.execute();
            if (response != null) {
                return response;
            }

            throw handleFailure(command, ACTION_SEND_MESSAGE);
        }
    }

    /**
     * method to find out if there is any available subscription. It will silently return false if it doesn't return a successful status code or the response is null in case it gets too noisy.
     *
     * @param messageRequest
     * @return
     */
    public boolean hasSubscription(SendMessage messageRequest) {
        LookupSubscriptionCommand command = null;
        try (Timer.Context ignored = checkSubscriptionTimer.time()) {
            command = new LookupSubscriptionCommand.Builder(httpClient, httpHost)
                    .setUserId(messageRequest.getUserId())
                    .setType(NotificationType.CHATMESSAGE)
                    .setEnabled(true)
                    .setLimit(1L)
                    .setHystrixTimeout(hystrixTimeout)
                    .build();
            final List<Subscription> response = command.execute();
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            throw handleFailure(command, ACTION_HAS_SUBSCRIPTION);
        }
    }

    private SendException handleFailure(FailureAwareCommand command, String meterName) {
        TimingReports.newCounter(meterName + ".exceptions").inc();
        if (command.getFailure() != null) {
            return command.getFailure();
        }

        if (command.isCircuitBreakerOpen()) {
            // Hystrix's circuit breaker tripped; we have no recorded failure, but something's still wrong.
            return new SendException(SendException.Cause.UNKNOWN, "Hystrix circuit open", null);
        }

        if (command.isResponseTimedOut()) {
            return new SendException(SendException.Cause.TIMEOUT, "Hystrix command timed out", null);
        }

        if (command.isFailedExecution()) {
            return new SendException(SendException.Cause.UNKNOWN, "Hystrix command failed", command.getFailedExecutionException());
        }

        return new SendException(SendException.Cause.UNKNOWN, "Hystrix command did not succeed, unsure why", null);
    }

    public enum NotificationType {
        CHATMESSAGE
    }
}
