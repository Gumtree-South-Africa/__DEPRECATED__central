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

import java.util.ArrayList;
import java.util.List;

public class RiakConfigurationRepository implements ConfigurationRepository {


    private static final Logger LOG = LoggerFactory.getLogger(RiakConfigurationRepository.class);
    private static final String DEFAULT_CONFIG_BUCKET_NAME = "config";
    private final ConfigurationConverter converter;

    private DomainBucket<Configurations> configurationBucket;

    public RiakConfigurationRepository(IRiakClient riakClient) throws RiakRetryFailedException {
    	converter = new ConfigurationConverter(DEFAULT_CONFIG_BUCKET_NAME);
        setupBucket(DEFAULT_CONFIG_BUCKET_NAME, riakClient);
    }
    
    public RiakConfigurationRepository(IRiakClient riakClient, String bucketNamePrefix) throws RiakRetryFailedException {
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
        List<PluginConfiguration> plugins = new ArrayList<PluginConfiguration>();


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
    public void persistConfiguration(PluginConfiguration configuration) {

        ConfigurationObject obj = new ConfigurationObject(System.currentTimeMillis(), configuration);


        Configurations mergedConfigurations = fetchConfigurations().addOrUpdate(obj);
        try {
            configurationBucket.store(mergedConfigurations);
        } catch (RiakException e) {
            String configId = configuration.getId().toString();
            throw new PersistenceException("Could not store configuration identified by " + configId, e);
        }
    }

    @Override
    public void deleteConfiguration(ConfigurationId configurationId) {
        try {
            configurationBucket.store(fetchConfigurations().delete(configurationId));
        } catch (RiakException e) {
            throw new PersistenceException("Could not store configuration identified by " + configurationId.toString(), e);
        }
    }
}
