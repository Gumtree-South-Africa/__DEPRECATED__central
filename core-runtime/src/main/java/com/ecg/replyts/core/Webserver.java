package com.ecg.replyts.core;

import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.LoggerContext;
import com.ecg.replyts.core.runtime.HttpServerFactory;
import com.ecg.replyts.core.runtime.MetricsService;
import com.ecg.replyts.core.runtime.prometheus.ApiResponseExporter;
import com.ecg.replyts.core.webapi.ContextProvider;
import com.ecg.replyts.core.webapi.HostReportingServletHandler;
import com.ecg.replyts.core.webapi.SpringContextProvider;
import com.ecg.replyts.core.webapi.ThreadPoolBuilder;
import com.ecg.replyts.core.webapi.util.ServerStartupLifecycleListener;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.lang.management.ManagementFactory;
import java.util.List;

import static org.eclipse.jetty.http.HttpMethod.DELETE;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.eclipse.jetty.http.HttpMethod.PUT;
import static org.eclipse.jetty.http.MimeTypes.Type.APPLICATION_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_JSON;
import static org.eclipse.jetty.http.MimeTypes.Type.TEXT_PLAIN;

@Component
public class Webserver {
    private static final Logger LOG = LoggerFactory.getLogger(Webserver.class);

    @Autowired
    private HttpServerFactory serverFactory;

    @Value("${replyts.http.maxThreads:100}")
    private int maxThreads;

    @Value("${replyts.http.maxThreadQueueSize:200}")
    private int maxThreadQueueSize;

    @Value("${replyts.jetty.gzip.enabled:false}")
    private boolean gzipEnabled;

    @Value("${replyts.jetty.instrument:true}")
    private boolean instrumented;

    @Value("${cluster.jmx.enabled:true}")
    private boolean jmxEnabled = false;

    private boolean started = false;

    private final HandlerCollection handlers = new HandlerCollection();

    private List<SpringContextProvider> contextProviders;

    private Server server;

    public Webserver(List<SpringContextProvider> providers) {
        this.contextProviders = providers;
    }

    @PostConstruct
    public void init() {
        server = serverFactory.createServer(new ThreadPoolBuilder()
          .withMaxThreads(maxThreads)
          .withInstrumentation(instrumented)
          .withQueueSize(maxThreadQueueSize)
          .withName(Webserver.class.getSimpleName()));

        start();
    }

    @PreDestroy
    private void shutdown() throws Exception {
        handlers.stop();
        server.stop();
        server.join();
    }

    private void start() {
        if (started) {
            throw new IllegalStateException("The Webserver has already been started");
        }

        ServerStartupLifecycleListener listener = new ServerStartupLifecycleListener();

        handlers.addLifeCycleListener(listener);

        if (jmxEnabled) {
            registerJettyOnJmx();
        }

        for (SpringContextProvider springContextProvider : contextProviders) {
            Handler handler = gzip(springContextProvider.create());
            String path = StringUtils.removeStart(springContextProvider.getPath(), "/");
            if (!path.isEmpty()) { // Do not instrument home
                handler = instrument(path, handler);
            }
            handlers.addHandler(handler);
        }
        handlers.addHandler(createAccessLoggingHandler());
        handlers.addHandler(new ApiResponseExporter());

        server.setHandler(handlers);

        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        listener.awaitStartup();

        contextProviders.forEach(ContextProvider::test);

        started = true;

        LOG.info("The Webserver has been started with {} context providers", contextProviders.size());
    }

    public boolean isStarted() {
        return started;
    }

    private Handler instrument(String path, Handler handler) {
        if (instrumented) {
            HostReportingServletHandler instrumentedHandler = new HostReportingServletHandler(MetricsService.getInstance().getRegistry());
            LOG.debug("Instrumenting handler: {}", handler);

            instrumentedHandler.setName(path);

            instrumentedHandler.setHandler(handler);

            return instrumentedHandler;
        } else {
            return handler;
        }
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

    private Handler gzip(Handler contextHandler) {
        if (gzipEnabled) {
            GzipHandler gzipHandler = new GzipHandler();

            gzipHandler.setIncludedMethods(GET.asString(), POST.asString(), PUT.asString(), DELETE.asString());
            gzipHandler.setIncludedMimeTypes(TEXT_PLAIN.asString(), TEXT_JSON.asString(), APPLICATION_JSON.asString());
            gzipHandler.setHandler(contextHandler);

            return gzipHandler;
        } else {
            return contextHandler;
        }
    }

    public int getThreads() {
        return server.getThreadPool().getThreads();
    }
}