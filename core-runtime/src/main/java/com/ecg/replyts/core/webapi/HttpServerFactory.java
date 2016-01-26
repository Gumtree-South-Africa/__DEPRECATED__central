package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.util.Optional;

/**
 * Factory to create a http Jetty web server and configure it.
 */
public class HttpServerFactory {

    private final int httpPortNumber;
    private final Optional<Long> httpTimeoutMs;
    private final ThreadPoolBuilder threadPoolBuilder;

    public HttpServerFactory(int httpPortNumber, Optional<Long> httpTimeoutMs, ThreadPoolBuilder threadPoolBuilder) {
        this.httpPortNumber = httpPortNumber;
        this.httpTimeoutMs = httpTimeoutMs;
        this.threadPoolBuilder = threadPoolBuilder;
    }

    public Server createServer() {

        Server server = new Server(threadPoolBuilder.build());

        HttpConnectionFactory factory = new HttpConnectionFactory();
        HttpConfiguration configuration = factory.getHttpConfiguration();
        ServerConnector connector = new ServerConnector(server, factory);

        httpTimeoutMs.ifPresent(timeoutMs -> {
            // Set blocking-timeout to 0 means to use the idle timeout,
            // see http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/HttpConfiguration.html#setBlockingTimeout-long-
            configuration.setBlockingTimeout(0L);
            connector.setIdleTimeout(timeoutMs);
        });

        connector.setPort(httpPortNumber);
        server.addConnector(connector);

        return server;
    }

}
