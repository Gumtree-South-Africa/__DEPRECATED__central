package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.EnvironmentSupport;
import com.ecg.replyts.core.webapi.ssl.SSLConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Optional;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builder to create the embedded web server.
 */
public class EmbeddedWebServerBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedWebServerBuilder.class);

    private ClassPathXmlApplicationContext context;
    private Properties properties;
    private Integer httpPort;
    private String xmlConfig;
    private final EnvironmentSupport environmentSupport;

    public EmbeddedWebServerBuilder(EnvironmentSupport environmentSupport) {
        this.environmentSupport = environmentSupport;
    }

    public EmbeddedWebServerBuilder withContext(ClassPathXmlApplicationContext value) {
        context = value;
        return this;
    }

    public EmbeddedWebServerBuilder withProperties(Properties value) {
        properties = value;
        return this;
    }

    public EmbeddedWebServerBuilder withHttpPort(int value) {
        httpPort = value;
        return this;
    }

    public EmbeddedWebServerBuilder withXmlConfig(String value) {
        xmlConfig = value;
        return this;
    }

    public EmbeddedWebserver build() {

        checkNotNull(context);
        checkNotNull(properties);
        checkNotNull(httpPort);
        checkNotNull(xmlConfig);

        Optional<SSLConfiguration> sslConfig = createSSLConfig();

        Optional<Long> httpTimeoutMs = Optional.ofNullable(properties.getProperty("replyts.http.timeoutMs")).map(Long::parseLong);
        Optional<Integer> maxThreads = Optional.ofNullable(properties.getProperty("replyts.http.maxThreads")).map(Integer::parseInt);
        Optional<Integer> maxThreadQueueSize = Optional.ofNullable(properties.getProperty("replyts.http.maxThreadQueueSize")).map(Integer::parseInt);

        EmbeddedWebserver embeddedWebserver = new EmbeddedWebserver(httpPort, sslConfig, environmentSupport, httpTimeoutMs, maxThreads, maxThreadQueueSize);

        for (ContextProvider contextProvider : context.getBeansOfType(ContextProvider.class).values()) {
            LOG.info("registering API Context: {}", contextProvider.getContextPath());
            embeddedWebserver.context(contextProvider);
        }

        // the root context must be the last context added to jetty - otherwise it will override everything else
        embeddedWebserver
                .context(new SpringContextProvider("/", new String[]{xmlConfig}, context))
                .start();

        return embeddedWebserver;
    }

    private Optional<SSLConfiguration> createSSLConfig() {
        Boolean sslEnabled = Boolean.valueOf(properties.getProperty("replyts.ssl.enabled", "false"));
        return sslEnabled ? Optional.of(SSLConfiguration.createSSLConfiguration(properties)) : Optional.empty();
    }

}
