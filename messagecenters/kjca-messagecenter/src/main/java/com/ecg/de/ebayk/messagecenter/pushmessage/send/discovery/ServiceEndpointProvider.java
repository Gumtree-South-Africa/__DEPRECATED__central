package com.ecg.de.ebayk.messagecenter.pushmessage.send.discovery;

import ca.kijiji.discovery.DiscoveryFailedException;
import ca.kijiji.discovery.LookupRequest;
import ca.kijiji.discovery.LookupResult;
import ca.kijiji.discovery.Protocol;
import ca.kijiji.discovery.SelectAll;
import ca.kijiji.discovery.SelectionStrategy;
import ca.kijiji.discovery.ServiceDirectory;
import ca.kijiji.discovery.ServiceEndpoint;
import com.codahale.metrics.Timer;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * A Box-specific wrapper for service discovery. Developers are encouraged to
 * make as few calls to this service as possible; however, the results should
 * not be retained/reused as they may become stale in quick order.
 */
public class ServiceEndpointProvider {
    private static final Protocol HTTP = new Protocol("http");
    private static final SelectionStrategy SELECT_ALL_STRATEGY = new SelectAll();

    private final ServiceDirectory serviceDirectory;
    private final ServiceLookupMetrics metrics;

    protected ServiceEndpointProvider() {
        this.serviceDirectory = null;
        this.metrics = null;
    }

    public ServiceEndpointProvider(ServiceDirectory serviceDirectory, ServiceLookupMetrics metrics) {
        this.serviceDirectory = Objects.requireNonNull(serviceDirectory);
        this.metrics = Objects.requireNonNull(metrics);
    }

    /**
     * Get the first endpoint for the given service. Usage of this
     * implementation is discouraged in favour of {@link #getServiceEndpoints},
     * which returns a number of results that may be iterated over to perform
     * retries.
     *
     * @param serviceName
     * @throws IllegalArgumentException If no endpoints are available.
     * @throws DiscoveryFailedException If discovery cannot be performed.
     * @deprecated Use {@link #getServiceEndpoints instead}.
     */
    @Deprecated
    public ServiceEndpoint getServiceEndpoint(@Nonnull ServiceName serviceName) throws DiscoveryFailedException {
        return getServiceEndpoints(serviceName).get(0);
    }

    /**
     * Get a number of endpoint for the given service. While the results
     * returned should not be reused or cached, the collection may be
     * used when creating a retry plan for a single execution.
     *
     * @param serviceName
     * @throws IllegalArgumentException If no endpoints are available.
     * @throws DiscoveryFailedException If discovery cannot be performed.
     */
    public List<ServiceEndpoint> getServiceEndpoints(@Nonnull ServiceName serviceName) throws DiscoveryFailedException {
        try (final Timer.Context timer = this.metrics.timer()) {
            final LookupRequest request = new LookupRequest(serviceName.asServiceName(), HTTP);
            final LookupResult result = serviceDirectory.lookup(SELECT_ALL_STRATEGY, request);
            if (result.isEmpty()) {
                metrics.notFound(serviceName);
                throw new IllegalStateException("Unable to find service: " + serviceName.asServiceName());
            }
            metrics.found(serviceName);
            return result.all();
        } catch (DiscoveryFailedException e) {
            metrics.failed(serviceName);
            throw e;
        }
    }
}
