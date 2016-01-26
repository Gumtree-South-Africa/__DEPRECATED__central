package com.ecg.replyts.core.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author mhuttar
 */
public class StartupExperience {

    private static final Logger LOG = LoggerFactory.getLogger(StartupExperience.class);

    private final long begin;


    public StartupExperience() {
        begin = System.currentTimeMillis();
        LOG.info("Starting ReplyTS Runtime");
    }

    public void running(int apiHttpPort) {
        LOG.info("    ____             __     ___________");
        LOG.info("   / __ \\___  ____  / /_  _/_  __/ ___/");
        LOG.info("  / /_/ / _ \\/ __ \\/ / / / // /  \\__ \\");
        LOG.info(" / _, _/  __/ /_/ / / /_/ // /  ___/ /");
        LOG.info("/_/ |_|\\___/ .___/_/\\__, //_/  /____/");
        LOG.info("          /_/      /____/");

        LOG.info("ReplyTS Startup Complete in {}ms.", System.currentTimeMillis() - begin);
        LOG.info("Documentation: https://github.corp.ebay.com/ReplyTS/replyts2-core/wiki");


        try {
            LOG.info("Browse to: http://{}:{}", InetAddress.getLocalHost().getHostAddress(), apiHttpPort);
        } catch (UnknownHostException e) {
            LOG.warn("could not resolve local host... ", e);
        }
    }
}
