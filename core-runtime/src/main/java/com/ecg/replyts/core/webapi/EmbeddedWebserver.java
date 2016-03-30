package com.ecg.replyts.core.webapi;


import ch.qos.logback.access.jetty.RequestLogImpl;
import com.ecg.replyts.core.webapi.ssl.SSLConfiguration;
import com.ecg.replyts.core.webapi.ssl.SSLServerFactory;
import com.ecg.replyts.core.webapi.util.ServerStartupLifecycleListener;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
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

/**
 * Wrapper around basic Jetty webserver.
 */
@Component
public class EmbeddedWebserver {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedWebserver.class);

    private Server server;

    private final List<ContextProvider> contextProviders = new ArrayList<>();

    private final HandlerCollection handlers = new HandlerCollection();

    private Integer httpPort;

    private boolean isStarted = false;

    @Value("${cluster.jmx.enabled:true}")
    private boolean isJmxEnabled = false;

    @Value("${confDir:conf}/logback-access.xml")
    private String logbackAccessFileName;

    @Autowired
    public EmbeddedWebserver(
            @Value("${replyts.ssl.enabled:false}") boolean isSSLEnabled,
            @Value("${replyts.http.port:8081}") Integer httpPortNumber,
            @Value("${replyts.http.timeout:null}") Long httpTimeoutMs,
            @Value("${replyts.http.maxThreads:null}") Integer maxThreads,
            @Value("${replyts.http.maxThreadQueueSize:null}") Integer maxThreadQueueSize,
            Environment environment) {
        this.httpPort = httpPortNumber;

        ThreadPoolBuilder builder = new ThreadPoolBuilder()
                .withMaxThreads(Optional.ofNullable(maxThreads))
                .withQueueSize(Optional.ofNullable(maxThreadQueueSize));

        if (isSSLEnabled) {
            SSLConfiguration sslConfiguration = SSLConfiguration.createSSLConfiguration(environment);
            SSLServerFactory factory = new SSLServerFactory(httpPortNumber, Optional.ofNullable(httpTimeoutMs), builder, sslConfiguration);
            server = factory.createServer();
        } else {
            HttpServerFactory factory = new HttpServerFactory(httpPortNumber, Optional.ofNullable(httpTimeoutMs), builder);
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
        if (isStarted)
            throw new IllegalStateException("Can not add (additional) contexts after the EmbeddedWebserver has been started");

        contextProviders.add(context);
    }

    public void start() {
        if (isStarted)
            throw new IllegalStateException("The EmbeddedWebserver has already been started!");

        ServerStartupLifecycleListener listener = new ServerStartupLifecycleListener();

        handlers.addLifeCycleListener(listener);

        if (isJmxEnabled) {
            registerJettyOnJmx();
        }

        addContextHandlers();
        addAccessLoggingHandler();

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

    private void addContextHandlers() {
        LOG.info("Adding {} context handler(s) to the webserver", contextProviders.size());

        contextProviders.forEach(context -> handlers.addHandler(context.create()));
    }

    private void registerJettyOnJmx() {
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());

        server.addEventListener(mbContainer);
        server.addBean(mbContainer);

        ConnectorStatistics.addToAllConnectors(server);
    }

    private void addAccessLoggingHandler() {
        if (handlers.getBean(RequestLogHandler.class) != null)
            throw new IllegalStateException("This EmbeddedWebserver already has a request-logging handler associated with it");

        if (new File(logbackAccessFileName).exists()) {
            LOG.info("Found config file for access logging: " + logbackAccessFileName);

            RequestLogHandler requestLogHandler = new RequestLogHandler();
            RequestLogImpl logbackLogger = new RequestLogImpl();

            logbackLogger.setFileName(logbackAccessFileName);
            requestLogHandler.setRequestLog(logbackLogger);

            // Have to call start(), logger seems not to be started automatically on server start (bug?).

            logbackLogger.start();

            handlers.addHandler(requestLogHandler);
        } else {
            LOG.info("Did not find config file for access logging, access logging disabled. To enable, provide {}", logbackAccessFileName);
        }
    }
}
