package com.ecg.replyts.core.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class StartupExperience {
    private static final Logger LOG = LoggerFactory.getLogger(StartupExperience.class);

    private final long begin;

    public StartupExperience() {
        begin = System.currentTimeMillis();
    }

    public StartupExperience(@Value("${confDir}") String confDir, @Value("${logDir:.}") String logDir) {
        this();

        LOG.info("Starting COMaaS Runtime");

        LOG.info("Config Directory: -DconfDir={}", confDir);
        LOG.info("Log Directory:    -DlogDir={}", logDir);
    }

    public boolean running(int apiHttpPort) {
        LOG.info("   __________  __  ___            _____");
        LOG.info("  / ____/ __ \\/  |/  /___ _____ _/ ___/");
        LOG.info(" / /   / / / / /|_/ / __ `/ __ `/\\__ \\");
        LOG.info("/ /___/ /_/ / /  / / /_/ / /_/ /___/ /");
        LOG.info("\\____/\\____/_/  /_/\\__,_/\\__,_//____/");
        LOG.info("");

        LOG.info("COMaaS startup complete in {}ms.", System.currentTimeMillis() - begin);
        LOG.info("Documentation can be found here: https://github.corp.ebay.com/ecg-comaas/ecg-comaas-central/wiki");

        try {
            LOG.info("Browse to: http://{}:{}", InetAddress.getLocalHost().getHostAddress(), apiHttpPort);

            return true;
        } catch (UnknownHostException e) {
            LOG.error("Could not resolve localhost", e);

            return false;
        }
    }
}
