package com.ecg.replyts.core.runtime.prometheus;

import com.ecg.replyts.core.ApplicationReadyEvent;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;

public class PrometheusExporter {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusExporter.class);
    private final int port;

    @Autowired
    public PrometheusExporter(@Value("${prometheus.port:9428}") int port) {
        this.port = port;
    }

    @EventListener
    public void onApplicationReadyEvent(ApplicationReadyEvent event) {
        try {
            Server server = new Server(port);
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");
            server.setHandler(context);
            context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
            server.start();

            LOG.info("Prometheus metrics on port {}", port);
        } catch (Exception e) {
            LOG.warn("Prometheus metrics exception", e);
        }
    }
}
