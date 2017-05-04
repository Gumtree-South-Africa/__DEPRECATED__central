package com.ecg.replyts.core.webapi;


import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import com.ecg.replyts.core.runtime.MetricsService;
import com.ecg.replyts.core.webapi.ssl.SSLConfiguration;
import com.ecg.replyts.core.webapi.ssl.SSLServerFactory;
import com.ecg.replyts.core.webapi.util.ServerStartupLifecycleListener;
import com.github.danielwegener.logback.kafka.KafkaAppender;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
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
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.eclipse.jetty.http.HttpMethod.DELETE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpMethod.PUT;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;

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

    @Autowired(required = false)
    private KafkaAppender accessLogAppender;

    @Value("${cluster.jmx.enabled:true}")
    private boolean isJmxEnabled = false;

    @Value("${confDir:conf}/logback-access.xml")
    private String logbackAccessFileName;

    private boolean instrument;

    @Autowired
    public EmbeddedWebserver(
            @Value("${replyts.ssl.enabled:false}") boolean isSSLEnabled,
            @Value("${replyts.http.port:8081}") Integer httpPortNumber,
            @Value("${replyts.http.timeout:5000}") Long httpTimeoutMs,
            @Value("${replyts.http.maxThreads:100}") Integer maxThreads,
            @Value("${replyts.http.maxThreadQueueSize:200}") Integer maxThreadQueueSize,
            @Value("${replyts.jetty.gzip.enabled:false}") boolean gzipEnabled,
            @Value("${replyts.jetty.instrument:true}") boolean instrumented,
            Environment environment) {
        this.httpPort = httpPortNumber;
        this.gzipEnabled = gzipEnabled;
        this.instrument = instrumented;

        ThreadPoolBuilder builder = new ThreadPoolBuilder()
                .withMaxThreads(maxThreads)
                .withInstrumentation(instrumented)
                .withQueueSize(maxThreadQueueSize);

        if (isSSLEnabled) {
            SSLConfiguration sslConfiguration = SSLConfiguration.createSSLConfiguration(environment);
            SSLServerFactory factory = new SSLServerFactory(httpPortNumber, httpTimeoutMs, builder, sslConfiguration);
            server = factory.createServer();
        } else {
            HttpServerFactory factory = new HttpServerFactory(httpPortNumber, httpTimeoutMs, builder);
            server = factory.createServer();
        }

        LOG.info("Configuring Webserver for Port {}", httpPortNumber);
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

        Handler accessLoggingHandler = createAccessLoggingHandler();
        if (accessLoggingHandler != null) {
            LOG.info("Access logging is enabled");
            handlers.addHandler(instrument(accessLoggingHandler));
        }

        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Test to see if all handlers were properly initialized

        listener.awaitStartup();

        contextProviders.forEach(context -> context.test());

        // Don't report having started until all context have been initialized

        isStarted = true;
    }

    private Handler instrument(Handler handler) {
        if (instrument) {
            HostReportingServletHandler instrumentedHandler = new HostReportingServletHandler(MetricsService.getInstance().getRegistry());
            LOG.debug("Instrumenting handler: ", handler);
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

        ConnectorStatistics.addToAllConnectors(server);
    }

    private Handler createAccessLoggingHandler() {
        if (handlers.getBean(RequestLogHandler.class) != null) {
            throw new IllegalStateException("This EmbeddedWebserver already has a request-logging handler associated with it");
        }


        if (accessLogAppender != null && accessLogAppender.isStarted()) {
            LOG.info("Found valid Kafka appender for access logging");

            RequestLogImpl logger = new RequestLogImpl();

            logger.getStatusManager().add(new OnConsoleStatusListener());
            logger.addAppender(accessLogAppender);

            // Also start a file logger if configured
            if (new File(logbackAccessFileName).exists()) {
                LOG.info("Found config file for access logging: " + logbackAccessFileName);
                logger.setFileName(logbackAccessFileName);
            }

            return startRequestLogHandler(logger);
        } else if (new File(logbackAccessFileName).exists()) {

            RequestLogImpl logger = new RequestLogImpl();

            LOG.info("Found config file for access logging: " + logbackAccessFileName);
            logger.setFileName(logbackAccessFileName);

            return startRequestLogHandler(logger);
        } else {
            LOG.info("Did not find config file {} for access logging and Kafka (access) logging is disabled", logbackAccessFileName);

            return null;
        }
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
