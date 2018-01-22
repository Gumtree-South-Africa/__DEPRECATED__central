package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.webapi.ThreadPoolBuilder;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpServerFactory {
    @Value("#{environment.COMAAS_HTTP_PORT ?: 8080}")
    private int httpPortNumber;

    @Value("${replyts.http.maxAcceptRequestQueueSize:50}")
    private int httpMaxAcceptRequestQueueSize;

    @Value("${replyts.http.timeout:5000}")
    private long httpTimeoutMs;

    @Value("${replyts.http.blocking.timeout:5000}")
    private long httpBlockingTimeoutMs;

    @Value("${replyts.jetty.thread.stop.timeout:5000}")
    private long threadStopTimeoutMs;

    public Server createServer(ThreadPoolBuilder threadPoolBuilder) {
        Server server = new Server(threadPoolBuilder.build());
        HttpConnectionFactory factory = new HttpConnectionFactory();

        HttpConfiguration configuration = factory.getHttpConfiguration();

        configuration.setBlockingTimeout(httpBlockingTimeoutMs); // http://download.eclipse.org/jetty/stable-9/apidocs/org/eclipse/jetty/server/HttpConfiguration.html#setBlockingTimeout-long-

        ServerConnector connector = new ServerConnector(server, factory);

        connector.setIdleTimeout(httpTimeoutMs);
        connector.setAcceptQueueSize(httpMaxAcceptRequestQueueSize); // https://wiki.eclipse.org/Jetty/Howto/Configure_Connectors
        connector.setStopTimeout(threadStopTimeoutMs);
        connector.setPort(httpPortNumber);

        server.addConnector(connector);

        return server;
    }
}