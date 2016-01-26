package com.ecg.replyts.core.runtime;

import com.hazelcast.config.Config;

import java.util.Properties;

public interface EnvironmentSupport {

    Properties getReplyTsProperties();

    Config getHazelcastConfig();

    int getApiHttpPort();

    String getConfigurationProfile();

    boolean logbackAccessConfigExists();

    String getLogbackAccessConfigFileName();

    void logEnvironmentConfiguration();
}
