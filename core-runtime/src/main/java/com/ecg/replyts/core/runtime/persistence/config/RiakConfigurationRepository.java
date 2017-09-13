package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.bucket.DomainBucket;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import com.ecg.replyts.core.api.persistence.ConfigurationRepository;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

public class RiakConfigurationRepository implements ConfigurationRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RiakConfigurationRepository.class);

    private static final String DEFAULT_CONFIG_BUCKET_NAME = "config";

    @Value("${logDir:.}")
    private String logDir;

    private final ConfigurationConverter converter;

    private DomainBucket<Configurations> configurationBucket;

    public RiakConfigurationRepository(IRiakClient riakClient, String bucketNamePrefix) throws RiakRetryFailedException {
        checkNotNull(bucketNamePrefix, "Please provide a bucketNamePrefix (can be an empty string)");
        converter = new ConfigurationConverter(bucketNamePrefix + DEFAULT_CONFIG_BUCKET_NAME);
        setupBucket(bucketNamePrefix + DEFAULT_CONFIG_BUCKET_NAME, riakClient);
    }

    private void setupBucket(String bucketName, IRiakClient riakClient) throws RiakRetryFailedException {
        Bucket rawBucket = riakClient.fetchBucket(bucketName).execute();
        configurationBucket = DomainBucket.builder(rawBucket, Configurations.class)
                .withConverter(converter)
                .withResolver(new NewestTimestampConflictResolver())
                .w(Quora.ALL)
                .dw(Quora.ALL)
                .r(Quora.QUORUM)
                .rw(Quora.QUORUM)
                .retrier(new DefaultRetrier(4))
                .returnBody(false)
                .build();
    }

    @Override
    public List<PluginConfiguration> getConfigurations() {
        List<PluginConfiguration> plugins = new ArrayList<>();

        List<ConfigurationObject> configurationObjects = fetchConfigurations().getConfigurationObjects();
        for (ConfigurationObject co : configurationObjects) {
            plugins.add(co.getPluginConfiguration());
        }

        return plugins;
    }

    private Configurations fetchConfigurations() {
        try {
            Configurations configurations = configurationBucket.fetch(ConfigurationConverter.KEY);
            if (configurations == null) {
                LOG.warn("No Filter Configurations available - no message filtering will be performed. If you don't have any filter rules configured, this is normal, otherwise there might be a problem with accessing Riak. (Could not find key '{}' in configuration bucket) ",
                        ConfigurationConverter.KEY);
                return Configurations.EMPTY_CONFIG_SET;
            }
            return configurations;
        } catch (RiakException e) {
            throw new PersistenceException("Could not retrieve configurations ", e);
        }
    }

    @Override
    public void persistConfiguration(PluginConfiguration configuration, String remoteAddress) {
        ConfigurationObject obj = new ConfigurationObject(System.currentTimeMillis(), configuration);
        final Configurations currentConfigurations = fetchConfigurations();
        backupConfigurations(currentConfigurations, remoteAddress);

        Configurations mergedConfigurations = currentConfigurations.addOrUpdate(obj);
        try {
            configurationBucket.store(mergedConfigurations);
        } catch (RiakException e) {
            String configId = configuration.getId().toString();
            throw new PersistenceException("Could not store configuration identified by " + configId, e);
        }
    }

    private void backupConfigurations(Configurations currentConfigurations, String remoteAddress) {
        final String currentTime = new SimpleDateFormat("yyyyMMdd-HH.mm.ss.SSS").format(new Date());
        final File configLocation = new File(logDir, format("configuration_%s_%s.json", remoteAddress, currentTime));
        LOG.info("Saving previous configurations as {}", configLocation);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(configLocation))) {
            out.write(String.valueOf(currentConfigurations));
        } catch (IOException e) {
            LOG.error("Could not save old configuration to file", e);
        }
    }

    @Override
    public void deleteConfiguration(String pluginFactory, String instanceId, String remoteAddress) {
        try {
            final Configurations currentConfigurations = fetchConfigurations();
            backupConfigurations(currentConfigurations, remoteAddress);

            configurationBucket.store(currentConfigurations.delete(new ConfigurationId(pluginFactory, instanceId)));
        } catch (RiakException e) {
            throw new PersistenceException("Could not store configuration identified by " + pluginFactory + ", " + instanceId, e);
        }
    }

    @Override
    public void replaceConfigurations(List<PluginConfiguration> pluginConfigurations, String remoteAddress) {
        List<PluginConfiguration> currentConfigurations = getConfigurations();

        try {
            currentConfigurations.forEach(c ->
                    deleteConfiguration(c.getId().getInstanceId(), c.getId().getPluginFactory(), remoteAddress));
            pluginConfigurations.forEach(configuration -> persistConfiguration(configuration, remoteAddress));
        } catch (Exception e) {
            LOG.error("Could not save new configurations, attempting to re-save the old ones", e);
            currentConfigurations.forEach(configuration -> persistConfiguration(configuration, remoteAddress));
            throw e;
        }
    }
}
