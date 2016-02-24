package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.runtime.EnvironmentSupport;
import com.ecg.replyts.core.runtime.ReplyTS;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClassPathEnvironmentSupport implements EnvironmentSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ClassPathEnvironmentSupport.class);
    private final String baseFolderInClasspath;
    private final int httpApiPort;

    public ClassPathEnvironmentSupport(String baseFolderInClasspath, int httpApiPort) {
        this.baseFolderInClasspath = baseFolderInClasspath;
        this.httpApiPort = httpApiPort;
    }

    @Override
    public Properties getReplyTsProperties() {
        try (InputStream is = new ClassPathResource(baseFolderInClasspath + "/replyts.properties").getInputStream()) {
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            throw new IllegalStateException("could not read replyts.properties", e);
        }
    }

    @Override
    public Config getHazelcastConfig() {
        try (InputStream is = new ClassPathResource(baseFolderInClasspath + "/hazelcast.xml").getInputStream()) {
            return new XmlConfigBuilder(is).build();
        } catch (IOException e) {
            throw new IllegalStateException("could not read hazelcast.xml", e);
        }
    }

    @Override
    public void logEnvironmentConfiguration() {
        LOG.debug("Reading configuration from Classpath: {}/{replyts.properties,hazelcast.xml}", baseFolderInClasspath);
    }

    @Override
    public int getApiHttpPort() {
        return httpApiPort;
    }

    @Override
    public String getConfigurationProfile() {
        return ReplyTS.EMBEDDED_PROFILE;
    }

    @Override
    public boolean logbackAccessConfigExists() {
        return false;
    }

    @Override
    public String getLogbackAccessConfigFileName() {
        return null;
    }
}
