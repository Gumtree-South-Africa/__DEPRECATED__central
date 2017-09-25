package com.ecg.replyts.core.webapi;

import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.LoggerContext;
import com.ecg.replyts.core.runtime.MetricsService;
import com.ecg.replyts.core.webapi.util.ServerStartupLifecycleListener;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnectionStatistics;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.jetty.http.HttpMethod.*;
import static org.eclipse.jetty.http.MimeTypes.Type.*;

/**
 * Wrapper around basic Jetty webserver.
 */
@Component
public class EmbeddedWebserver {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedWebserver.class);

    private Server server;

    private final List<ContextProvider> contextProviders = new ArrayList<>();

    private final HandlerCollection handlers = new HandlerCollection();

    private boolean gzipEnabled;

    private Integer httpPort;

    private boolean isStarted = false;

    @Value("${cluster.jmx.enabled:true}")
    private boolean isJmxEnabled = false;

    private boolean instrument;

    @Autowired
    public EmbeddedWebserver(
            @Value("#{environment.COMAAS_HTTP_PORT ?: 8080}") int httpPortNumber,
            @Value("${replyts.http.timeout:5000}") long httpTimeoutMs,
            @Value("${replyts.http.blocking.timeout:5000}") long httpBlockingTimeoutMs,
            @Value("${replyts.jetty.socket.linger:-1}") int solinger,
            @Value("${replyts.jetty.thread.stop.timeout:5000}") long threadStopTimeoutMs,
            @Value("${replyts.http.maxThreads:100}") int maxThreads,
            @Value("${replyts.http.maxThreadQueueSize:200}") int maxThreadQueueSize,
            @Value("${replyts.jetty.gzip.enabled:false}") boolean gzipEnabled,
            @Value("${replyts.jetty.instrument:true}") boolean instrumented,
            @Value("${replyts.http.maxAcceptRequestQueueSize:50}") int httpMaxAcceptRequestQueueSize,
            Environment environment) {

        this.httpPort = httpPortNumber;
        this.gzipEnabled = gzipEnabled;
        this.instrument = instrumented;

        ThreadPoolBuilder builder = new ThreadPoolBuilder()
                .withMaxThreads(maxThreads)
                .withInstrumentation(instrumented)
                .withQueueSize(maxThreadQueueSize)
                .withName(EmbeddedWebserver.class.getSimpleName());

        HttpServerFactory factory = new HttpServerFactory(httpPortNumber, httpTimeoutMs, builder, httpMaxAcceptRequestQueueSize,
                httpBlockingTimeoutMs, threadStopTimeoutMs, solinger);
        server = factory.createServer();

        LOG.info("Configuring Webserver for Port {}", httpPort);
    }

    @PreDestroy
    private void shutdown() throws Exception {
        handlers.stop();
        server.stop();
        server.join();
    }

    public Integer getPort() {
        return httpPort;
    }

    public void context(ContextProvider context) {
        if (isStarted) {
            throw new IllegalStateException("Can not add (additional) contexts after the EmbeddedWebserver has been started");
        }

        contextProviders.add(context);
    }

    public void start() {
        if (isStarted) {
            throw new IllegalStateException("The EmbeddedWebserver has already been started!");
        }

        ServerStartupLifecycleListener listener = new ServerStartupLifecycleListener();

        handlers.addLifeCycleListener(listener);

        if (isJmxEnabled) {
            registerJettyOnJmx();
        }

        if (gzipEnabled) {
            LOG.info("Jetty gzip compression is enabled");
            GzipHandler gzipHandler = buildGzipHandler(createContextHandler());
            handlers.addHandler(instrument(gzipHandler));
        } else {
            handlers.addHandler(instrument(createContextHandler()));
        }

        handlers.addHandler(instrument(createAccessLoggingHandler()));

        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Test to see if all handlers were properly initialized

        listener.awaitStartup();

        contextProviders.forEach(ContextProvider::test);

        // Don't report having started until all context have been initialized

        isStarted = true;
    }

    private Handler instrument(Handler handler) {
        if (instrument) {
            HostReportingServletHandler instrumentedHandler = new HostReportingServletHandler(MetricsService.getInstance().getRegistry());
            LOG.debug("Instrumenting handler: {}", handler);
            instrumentedHandler.setHandler(handler);
            return instrumentedHandler;
        }
        return handler;
    }

    private Handler createContextHandler() {
        LOG.info("Adding {} context handler(s) to the webserver", contextProviders.size());

        HandlerCollection contextHandler = new HandlerCollection();
        contextProviders.forEach(context -> contextHandler.addHandler(context.create()));
        return contextHandler;
    }

    private void registerJettyOnJmx() {
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());

        server.addEventListener(mbContainer);
        server.addBean(mbContainer);

        ServerConnectionStatistics.addToAllConnectors(server);
    }

    private Handler createAccessLoggingHandler() {
        if (handlers.getBean(RequestLogHandler.class) != null) {
            throw new IllegalStateException("This EmbeddedWebserver already has a request-logging handler associated with it");
        }
        RequestLogImpl logger = new RequestLogImpl();
        logger.setQuiet(true);

        LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
        loggerContext.getCopyOfPropertyMap().forEach(logger::putProperty);

        logger.setResource("/logback-access.xml");
        return startRequestLogHandler(logger);
    }

    private Handler startRequestLogHandler(RequestLogImpl logger) {
        RequestLogHandler handler = new RequestLogHandler();

        handler.setRequestLog(logger);
        logger.start();

        return handler;
    }

    private GzipHandler buildGzipHandler(Handler contextHandler) {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setIncludedMethods(GET.asString(), POST.asString(), PUT.asString(), DELETE.asString());
        gzipHandler.setIncludedMimeTypes(TEXT_PLAIN.asString(), TEXT_JSON.asString(), APPLICATION_JSON.asString());
        gzipHandler.setHandler(contextHandler);
        return gzipHandler;
    }
}
