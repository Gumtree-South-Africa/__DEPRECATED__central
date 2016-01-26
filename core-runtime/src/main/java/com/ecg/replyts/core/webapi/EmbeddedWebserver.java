package com.ecg.replyts.core.webapi;


import ch.qos.logback.access.jetty.RequestLogImpl;
import com.ecg.replyts.core.runtime.EnvironmentSupport;
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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Wrapper for the embedded Jetty web server that will startup Jetty and allow deployment of several contexts (or handlers in jetty notation) provided by
 * {@link ContextProvider} implementations.
 *
 * @author mhuttar
 */
public class EmbeddedWebserver {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedWebserver.class);

    private final Server server;
    private final List<ContextProvider> childHandlers = new ArrayList<>();
    private final HandlerCollection handlers = new HandlerCollection();
    private final EnvironmentSupport environmentSupport;

    public EmbeddedWebserver(int httpPortNumber, Optional<SSLConfiguration> sslConfig,
                             EnvironmentSupport environmentSupport, Optional<Long> httpTimeoutMs, Optional<Integer> maxThreads, Optional<Integer> maxThreadQueueSize) {

        checkNotNull(sslConfig);
        checkNotNull(environmentSupport);

        this.environmentSupport = environmentSupport;

        ThreadPoolBuilder builder = new ThreadPoolBuilder()
                .withMaxThreads(maxThreads)
                .withQueueSize(maxThreadQueueSize);

        if (sslConfig.isPresent()) {
            SSLServerFactory factory = new SSLServerFactory(httpPortNumber, httpTimeoutMs, builder, sslConfig.get());
            server = factory.createServer();
        } else {
            HttpServerFactory factory = new HttpServerFactory(httpPortNumber, httpTimeoutMs, builder);
            server = factory.createServer();
        }
        LOG.info("Configuring Webserver for Port {}", httpPortNumber);
    }

    private void addAccessLoggingHandler() {

        String configFileName = environmentSupport.getLogbackAccessConfigFileName();

        if (environmentSupport.logbackAccessConfigExists()) {
            LOG.info("Found config file for access logging: " + configFileName);

            RequestLogHandler requestLogHandler = new RequestLogHandler();
            RequestLogImpl logbackLogger = new RequestLogImpl();
            logbackLogger.setFileName(configFileName);
            requestLogHandler.setRequestLog(logbackLogger);
            // Have to call start(), logger seems not to be started automatically on server start (bug?).
            logbackLogger.start();

            handlers.addHandler(requestLogHandler);
        } else {
            LOG.info("Did not find config file for access logging, access logging disabled. To enable, provide {}", configFileName);
        }
    }

    private void registerJettyOnJmx() {
        MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
        server.addEventListener(mbContainer);
        server.addBean(mbContainer);
        ConnectorStatistics.addToAllConnectors(server);
    }

    /**
     * registers a new context to the embedded webserver. MUST be done before invoking start.
     */
    public EmbeddedWebserver context(ContextProvider context) {
        childHandlers.add(context);
        return this;
    }

    /**
     * starts the embedded webserver with all contexts registered so far (see context()).
     */
    public EmbeddedWebserver start() {

        ServerStartupLifecycleListener listener = new ServerStartupLifecycleListener();
        handlers.addLifeCycleListener(listener);

        registerJettyOnJmx();

        addContextHandler();
        addAccessLoggingHandler();

        server.setHandler(handlers);
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        listener.awaitStartup();
        for (ContextProvider h : childHandlers) {
            h.test();
        }
        return this;
    }

    private void addContextHandler() {
        for (ContextProvider h : childHandlers) {
            handlers.addHandler(h.createContext());
        }
    }


    /**
     * stops the embedded server, unregistering all contexts.
     */
    public void shutdown() throws Exception {
        handlers.stop();
        server.stop();
        server.join();
    }

}
