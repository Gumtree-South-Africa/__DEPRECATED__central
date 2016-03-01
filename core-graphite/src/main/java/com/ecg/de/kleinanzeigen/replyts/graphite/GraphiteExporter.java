package com.ecg.de.kleinanzeigen.replyts.graphite;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.ecg.replyts.core.runtime.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * User: acharton
 * Date: 4/29/13
 */
public class GraphiteExporter {

    private static final Logger LOG = LoggerFactory.getLogger(GraphiteExporter.class);
    public static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private final String hostname;
    private final int port;
    private final int timePeriod;
    private final String prefix;

    public GraphiteExporter(boolean enabled, String hostname, int port, int timePeriod, String prefix) throws UnknownHostException {
        this.hostname = hostname;
        this.port = port;
        this.timePeriod = timePeriod;
        this.prefix = prefix;

        if (!enabled) {
            LOG.info("Graphite reporting disabled via config.");
            return;
        }

        Assert.notNull(hostname);

        init(MetricsService.getInstance());
    }

    private void init(MetricsService metricsService) throws UnknownHostException {
        GraphiteReporter reporter = createReporter(metricsService.getRegistry(), graphiteEndpoint());

        reporter.start(timePeriod, TIME_UNIT);

        LOG.info("Graphite reporter started and send statistics every {} {}", timePeriod, TIME_UNIT);

    }

    private Graphite graphiteEndpoint() throws UnknownHostException {
        LOG.info("Set graphite server endpoint to {}:{}, default prefix is: '{}'", hostname, port, prefix);

        InetSocketAddress address = new InetSocketAddress(hostname, port);
        // Detect resolution problems early on, or a NPE will be thrown when inside Graphite's reporting loop.
        // When that happens reporting silently stops. :(
        if (address.isUnresolved()) {
            throw new UnknownHostException("Couldn't resolve hostname " + hostname);
        }
        return new Graphite(address);
    }


    private GraphiteReporter createReporter(MetricRegistry registry, Graphite graphiteEndpoint) {
        return GraphiteReporter.forRegistry(registry)
                .prefixedWith(prefix)
                .convertRatesTo(TIME_UNIT)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphiteEndpoint);
    }


}
