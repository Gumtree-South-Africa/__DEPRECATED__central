package com.ecg.replyts.core.runtime;


import com.google.common.base.Preconditions;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.*;
import java.util.Properties;

import static java.util.Optional.ofNullable;

/**
 * helper class pointing to various directories and paths ReplyTS requires (e.g. config- and logging directory)
 *
 * @author mhuttar
 */
final class ConfigDirectoryEnvironmentSupport implements EnvironmentSupport { // NOSONAR

    private static final Logger LOG = LoggerFactory.getLogger(ConfigDirectoryEnvironmentSupport.class);

    private static final String CONFIG_DIR_PROP = "confDir";
    private static final String LOG_DIR_PROP = "logDir";
    private final File confDir;
    private final File logDir;

    private ConfigDirectoryEnvironmentSupport(String confDir, String logDir) {
        try {
            DefaultResourceLoader loader = new DefaultResourceLoader();
            this.confDir = loader.getResource(confDir).getFile();
            this.logDir = new File(logDir);
            validate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return environment support with confDir and logDir read from environment (settable via -D properties). Default
     * behaviour.
     */
    public static EnvironmentSupport fromEnvironmentSettings() {
        String configDirStr = System.getProperty(CONFIG_DIR_PROP);
        Preconditions.checkNotNull(configDirStr, "Please specify -D" + CONFIG_DIR_PROP);

        String logDirStr = System.getProperty(LOG_DIR_PROP);
        Preconditions.checkNotNull(logDirStr, "Please specify -D" + LOG_DIR_PROP);

        if (!configDirStr.toLowerCase().startsWith("file:")) {
            configDirStr = "file:" + configDirStr;
        }
        return new ConfigDirectoryEnvironmentSupport(configDirStr, logDirStr);
    }

    @Override
    public Properties getReplyTsProperties() {
        try (InputStream i = new BufferedInputStream(new FileInputStream(new File(confDir, "replyts.properties")))) {
            Properties p = new Properties();
            p.load(i);
            return p;
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("replyts.properties does not exist in " + confDir.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new IllegalStateException("could not load replyts.properties", e);
        }
    }

    @Override
    public Config getHazelcastConfig() {
        File file = new File(confDir, "hazelcast.xml");
        try {
            return new FileSystemXmlConfig(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("could not load Hazelcast config from " + file, e);
        }
    }


    private void validate() {
        Preconditions.checkArgument(confDir.isDirectory(), confDir.getAbsolutePath() + " does not exist");
        Preconditions.checkArgument(logDir.isDirectory(), logDir.getAbsolutePath() + " does not exist");
    }


    @Override
    public void logEnvironmentConfiguration() {
        LOG.info("Config Directory: -D{}={}", CONFIG_DIR_PROP, confDir);
        LOG.info("Log Directory:    -D{}={}", LOG_DIR_PROP, logDir);
    }

    @Override
    public int getApiHttpPort() {
        return Integer.valueOf(ofNullable(System.getProperty("replyts.http.port"))
                .orElse(getReplyTsProperties().getProperty("replyts.http.port", "8081")));
    }

    @Override
    public String getConfigurationProfile() {
        return ReplyTS.PRODUCTIVE_PROFILE;
    }

    @Override
    public boolean logbackAccessConfigExists() {
        return new File(getLogbackAccessConfigFileName()).exists();
    }

    @Override
    public String getLogbackAccessConfigFileName() {
        return confDir.getAbsolutePath() + "/logback-access.xml";
    }


}
