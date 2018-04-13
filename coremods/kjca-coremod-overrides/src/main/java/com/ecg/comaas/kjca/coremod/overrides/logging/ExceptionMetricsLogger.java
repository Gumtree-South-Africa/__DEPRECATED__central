package com.ecg.comaas.kjca.coremod.overrides.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.codahale.metrics.logback.InstrumentedAppender;
import com.ecg.replyts.core.runtime.MetricsService;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class ExceptionMetricsLogger {
    private static String hostName = "localhost";
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(ExceptionMetricsLogger.class);

    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName().replaceAll("[^a-zA-Z0-9-]", "-");
        } catch (UnknownHostException e) {
            LOG.error("can not get local host name ", e);
        }
    }

    public ExceptionMetricsLogger() {
        // Lifted almost verbatim from http://metrics.codahale.com/manual/logback/

        LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger thisLogger = factory.getLogger(ExceptionMetricsLogger.class);
        thisLogger.info("Setting up codahale logback metrics plugin");
        Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);
        MetricsService metricsService = MetricsService.getInstance();
        InstrumentedAppender metrics = new InstrumentedAppender(metricsService.getRegistry());
        metrics.setContext(root.getLoggerContext());
        metrics.setName("LOG." + hostName);
        metrics.start();
        root.addAppender(metrics);
    }
}
