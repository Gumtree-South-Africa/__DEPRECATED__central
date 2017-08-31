package com.ecg.de.kleinanzeigen.replyts.graphite;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.ecg.replyts.core.runtime.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GraphiteExporter {
    private static final Logger LOG = LoggerFactory.getLogger(GraphiteExporter.class);

    private final String prefix;
    private final Optional<ScheduledReporter> reporter;
    private final Optional<Graphite> graphite;

    public GraphiteExporter(boolean enabled, String hostname, int port, int timePeriod, String prefix) throws UnknownHostException {
        this.prefix = prefix;

        if (!enabled) {
            LOG.info("Graphite reporting disabled via config.");
            this.reporter = Optional.empty();
            this.graphite = Optional.empty();
        } else {
            Assert.notNull(hostname);
            LOG.info("Set graphite server endpoint to {}:{}, default prefix is: '{}'", hostname, port, this.prefix);

            InetSocketAddress address = new InetSocketAddress(hostname, port);
            // Detect resolution problems early on, or a NPE will be thrown when inside Graphite's reporting loop.
            // When that happens reporting silently stops. :(
            if (address.isUnresolved()) {
                throw new UnknownHostException("Couldn't resolve hostname " + hostname);
            }
            Graphite endpoint = new Graphite(address);

            GraphiteReporter reporter = createReporter(MetricsService.getInstance().getRegistry(), endpoint);
            reporter.start(timePeriod, TimeUnit.SECONDS);

            this.reporter = Optional.of(reporter);

            LOG.info("Graphite reporter started and send statistics every {} {}", timePeriod, TimeUnit.SECONDS);
            this.graphite = Optional.of(endpoint);
        }
    }

    private GraphiteReporter createReporter(MetricRegistry registry, Graphite graphiteEndpoint) {
        return GraphiteReporter.forRegistry(registry)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphiteEndpoint);
    }

    @PreDestroy
    public void stop() {
        graphite.ifPresent(g -> {
            try {
                g.flush();
                g.close();
            } catch (Exception ignored) {
                LOG.debug("Exception caught trying to flush the Graphite, some events could be missing");
            }
        });
        reporter.ifPresent(ScheduledReporter::stop);
    }
}
