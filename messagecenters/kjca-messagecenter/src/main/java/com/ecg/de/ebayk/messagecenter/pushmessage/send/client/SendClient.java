package com.ecg.de.ebayk.messagecenter.pushmessage.send.client;

import ca.kijiji.discovery.DiscoveryFailedException;
import ca.kijiji.discovery.ServiceEndpoint;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery.ServiceDiscoveryConfig;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery.ServiceEndpointProvider;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery.ServiceName;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.model.SendMessage;
import com.ecg.de.ebayk.messagecenter.pushmessage.send.model.Subscription;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Nonnull;
import java.net.UnknownHostException;
import java.util.List;

import static com.ecg.de.ebayk.messagecenter.pushmessage.send.client.CloseableHttpClientBuilder.aCloseableHttpClient;

/**
 * Service layer to manage subscription requests with SEND
 */
public class SendClient {
    private static final Logger LOG = LoggerFactory.getLogger(SendClient.class);
    private static final int MAX_DISCOVERY_TRIES = 3;

    //TODO will need to hook this in once we have some real traffic.
    private final int hystrixTimeout;
    private final HttpClient httpClient;
    private final MetricRegistry metricRegistry;
    private final ServiceDiscoveryConfig serviceDiscoveryConfig;
    private ServiceEndpointProvider serviceEndpointProvider;

    private static final String ACTION_SEND_MESSAGE = "send.client.send-message";
    private static final String ACTION_HAS_SUBSCRIPTION = "send.client.check-subscription";

    @Autowired
    public SendClient(MetricRegistry metricRegistry,
                      ServiceDiscoveryConfig serviceDiscoveryConfig,
                      @Value("${send.client.max.retries:0}") final Integer maxRetries,
                      @Value("${send.client.max.connections:16}") final Integer maxConnections,
                      @Value("${send.client.timeout.hystrix.ms:1000}") final Integer hystrixTimeout,
                      @Value("${send.client.timeout.socket.millis:1000}") final Integer socketTimeout,
                      @Value("${send.client.timeout.connect.millis:1000}") final Integer connectTimeout,
                      @Value("${send.client.timeout.connectionRequest.millis:1000}") final Integer connectionRequestTimeout) {
        this.hystrixTimeout = hystrixTimeout;
        this.metricRegistry = metricRegistry;
        this.serviceDiscoveryConfig = serviceDiscoveryConfig;
        this.httpClient = createPooledHttpClient(maxRetries, metricRegistry.counter("send.client.general.retries"), maxConnections, socketTimeout, connectTimeout, connectionRequestTimeout);
        this.serviceEndpointProvider = setupDiscovery();
    }

    private CloseableHttpClient createPooledHttpClient(int maxRetries, Counter retryCounter, int maxConnections, int socketTimeout, int connectTimeout, int connectionRequestTimeout) {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(maxConnections);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(maxConnections);

        final DefaultHttpRequestRetryHandler retryHandler = new ZealousHttpRequestRetryHandler(maxRetries, retryCounter);

        return aCloseableHttpClient()
                .withConnectionManager(poolingHttpClientConnectionManager)
                .withHttpRequestRetryHandler(retryHandler)
                .withSocketTimeout(socketTimeout)
                .withConnectionTimeout(connectTimeout)
                .withConnectionManagerTimeout(connectionRequestTimeout)
                .build();
    }

    private ServiceEndpointProvider setupDiscovery() {
        try {
            return serviceDiscoveryConfig.serviceEndpointProvider();
        } catch (UnknownHostException e) {
            LOG.warn("Unable to get service catalog. SEND client will continue to fail until this is resolved.", e);
            return null;
        }
    }

    private List<ServiceEndpoint> sendEndpoints() {
        final ServiceEndpointProvider provider = getEndpointProvider();
        DiscoveryFailedException exception = null;
        for (int i = MAX_DISCOVERY_TRIES; i != 0; --i) {
            try {
                return provider.getServiceEndpoints(ServiceName.SEND_API);
            } catch (DiscoveryFailedException e) {
                exception = e;
                metricRegistry.counter("send.client.discovery.failures").inc();
                LOG.warn("Unable to get service catalog. Conversation Service will continue to fail until this is resolved. Retrying " + (i - 1) + " more time(s).", e);
            }
        }
        throw new SendException(SendException.Cause.DISCOVERY, "Discovery of Conversation Service failed", exception);
    }

    private ServiceEndpointProvider getEndpointProvider() {
        if (serviceEndpointProvider == null) {
            serviceEndpointProvider = setupDiscovery();
            if (serviceEndpointProvider == null) {
                throw new SendException(SendException.Cause.DISCOVERY, "Could not find service discovery provider", null);
            }
        }

        return serviceEndpointProvider;
    }

    public SendMessage sendMessage(@Nonnull SendMessage messageRequest) {
        try (Timer.Context ignored = metricRegistry.timer(ACTION_SEND_MESSAGE).time()) {
            SendMessageCommand command = new SendMessageCommand(httpClient, sendEndpoints(), messageRequest);
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
        try (Timer.Context ignored = metricRegistry.timer(ACTION_HAS_SUBSCRIPTION).time()) {
            command = new LookupSubscriptionCommand.Builder(httpClient, sendEndpoints())
                    .setUserId(messageRequest.getUserId())
                    .setType(NotificationType.CHATMESSAGE)
                    .setEnabled(true)
                    .setLimit(1L)
                    .build();
            final List<Subscription> response = command.execute();
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            throw handleFailure(command, ACTION_HAS_SUBSCRIPTION);
        }
    }

    private SendException handleFailure(FailureAwareCommand command, String meterName) {
        metricRegistry.meter(meterName + ".exceptions").mark();
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

    public enum DeliveryService {
        MDNS,
        FCM
    }

    public enum NotificationType {
        CHATMESSAGE
    }
}
