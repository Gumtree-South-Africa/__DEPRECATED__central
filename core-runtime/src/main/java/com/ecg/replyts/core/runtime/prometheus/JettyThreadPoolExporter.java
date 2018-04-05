package com.ecg.replyts.core.runtime.prometheus;

import com.ecg.replyts.core.Webserver;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.GaugeMetricFamily;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;

@Service
public class JettyThreadPoolExporter {

    private final Webserver webserver;

    public JettyThreadPoolExporter(Webserver webserver) {
        this.webserver = webserver;
    }

    @PostConstruct
    public void onPostConstruct() {
        CollectorRegistry.defaultRegistry.register(new JettyThreadPoolCollector());
    }

    private final class JettyThreadPoolCollector extends Collector {
        @Override
        public List<MetricFamilySamples> collect() {
            return Collections.singletonList(
                new GaugeMetricFamily("jetty_threads", "The total number of threads currently in the Jetty thread pool",
                        webserver.getThreads()));
        }
    }
}
