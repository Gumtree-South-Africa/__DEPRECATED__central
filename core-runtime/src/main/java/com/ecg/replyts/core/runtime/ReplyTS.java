package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.webapi.EmbeddedWebserver;
import com.ecg.replyts.core.webapi.EmbeddedWebServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Main Class for ReplyTS. Starts the full application.
 *
 * @author mhuttar
 */
public final class ReplyTS {

    /**
     * name for the spring configuration profile for production systems
     */
    public static final String PRODUCTIVE_PROFILE = "productive";

    /**
     * name for the spring configuration profile for automation tests
     */
    public static final String EMBEDDED_PROFILE = "embedded";

    private static final Logger LOG = LoggerFactory.getLogger(ReplyTS.class);

    private AbstractApplicationContext ctx;

    private ClassPathXmlApplicationContext subCtx;

    private EmbeddedWebserver embeddedWebserver;

    /**
     * Initialize ReplyTS and start it up. read environmental settings from a config directory in the file system, specified as <code>-DconfDir</code> parameter.
     */
    public ReplyTS() {
        this(ConfigDirectoryEnvironmentSupport.fromEnvironmentSettings());
    }

    /**
     * Initialize ReplyTS and start it up. References to the external system will be read from the passed
     * {@link ConfigDirectoryEnvironmentSupport} object.
     */
    public ReplyTS(EnvironmentSupport environmentSupport) {

        System.setProperty("spring.profiles.active", environmentSupport.getConfigurationProfile());
        try {
            StartupExperience startupExperience = new StartupExperience();

            environmentSupport.logEnvironmentConfiguration();

            startup(environmentSupport);

            startupExperience.running(environmentSupport.getApiHttpPort());
            Runtime.getRuntime().addShutdownHook(new ApplicationShutdownHook(this));
        } catch (Exception ex) {
            LOG.error("ReplyTS failed to start up", ex);
            shutdown();
            throw new IllegalStateException("ReplyTS failed to start up", ex);
        }
    }

    private void startup(EnvironmentSupport environment) {
        LOG.info("Launching ReplyTS runtime");

        ctx = buildPreconfiguredParentContext(environment);
        subCtx = new ClassPathXmlApplicationContext(new String[]{
                "classpath:replyts-runtime-context.xml",
                "classpath*:/plugin-inf/*.xml",
        }, ctx);


        int httpPort = environment.getApiHttpPort();

        EmbeddedWebServerBuilder webServerBuilder = new EmbeddedWebServerBuilder(environment)
                .withContext(subCtx)
                .withProperties(environment.getReplyTsProperties())
                .withHttpPort(httpPort)
                .withXmlConfig("classpath:replyts-control-context.xml");

        embeddedWebserver = webServerBuilder.build();
    }

    private AbstractApplicationContext buildPreconfiguredParentContext(EnvironmentSupport es) {

        // need to set this property to make hazelcast log via slf4j. needs to be invoked before hazelcast config is created.
        System.setProperty("hazelcast.logging.type", "slf4j");

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("replyts-properties", es.getReplyTsProperties());
        beanFactory.registerSingleton("hazelcast-config", es.getHazelcastConfig());
        GenericApplicationContext genericApplicationContext = new GenericApplicationContext(beanFactory);

        genericApplicationContext.refresh();
        return genericApplicationContext;
    }

    /**
     * shut down ReplyTS
     */
    public void shutdown() {
        LOG.info("Stopping ReplyTS");

        // first, stop incoming requests, prevent ugly exceptions on shutdown
        shutdownWebserver();
        // and then the rest
        shutdownSpring();

        LOG.info("Shutdown complete");
    }

    private void shutdownSpring() {
        if (ctx != null) {
            ctx.close();
        }
        if (subCtx != null) {
            subCtx.close();
        }
    }

    private void shutdownWebserver() {
        if (embeddedWebserver != null) {
            try {
                embeddedWebserver.shutdown();
            } catch (Exception e) {
                LOG.warn("Could not stop webapi", e);
            }
        }
    }

    public static void main(String[] args) throws Exception {

        try {
            new ReplyTS();
        } catch (Exception e) {
            LOG.error("ReplyTS Abnormal Shutdown", e);
            throw e;
        }
    }
}
