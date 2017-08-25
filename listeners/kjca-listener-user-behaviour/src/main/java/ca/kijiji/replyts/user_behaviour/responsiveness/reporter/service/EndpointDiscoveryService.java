package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.LookupRequest;
import ca.kijiji.discovery.SelectAll;
import ca.kijiji.discovery.ServiceDirectory;
import ca.kijiji.discovery.ServiceEndpoint;
import com.codahale.metrics.Counter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.retry.RetryException;
import com.ecg.replyts.core.runtime.retry.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EndpointDiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(EndpointDiscoveryService.class);

    private final ServiceDirectory serviceDirectory;
    private final LookupRequest lookupRequest;
    private final Counter discoveryFailedCounter;

    @Autowired
    public EndpointDiscoveryService(ServiceDirectory serviceDirectory, LookupRequest lookupRequest) {
        this.serviceDirectory = serviceDirectory;
        this.lookupRequest = lookupRequest;
        this.discoveryFailedCounter = TimingReports.newCounter("user-behaviour.responsiveness.discovery.failed");
    }

    /**
     * Consul lookup can accidentally fail and that is why we retry it once again in case of failure.
     * Currently we assume that if 2 consecutive lookups failed, other attempts will bring only overhead and, thus, we consider endpoint discovery failed
     *
     * @return - list of {@link ServiceEndpoint}
     * @throws RetryException - thrown if all tries (currently 2) failed, wraps original exception, if occurred, inside
     */
    public List<ServiceEndpoint> discoverEndpoints() throws RetryException {
        try {
            return RetryService.execute(() -> serviceDirectory.lookup(new SelectAll(), lookupRequest).all(), 2);
        } catch (RetryException e) {
            LOG.error("Consul lookup request failed. Responsiveness record would not be send to HTTP service", e);
            discoveryFailedCounter.inc();
            throw e;
        }
    }
}
