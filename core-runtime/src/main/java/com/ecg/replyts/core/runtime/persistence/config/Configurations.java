package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.convert.RiakKey;
import com.ecg.replyts.core.api.configadmin.ConfigurationId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

class Configurations {

    static final Configurations EMPTY_CONFIG_SET = new Configurations(Lists.<ConfigurationObject>newArrayList(), false);

    @RiakKey
    private String key = ConfigurationConverter.KEY; // NOSONAR

    private final boolean compressed;

    private final List<ConfigurationObject> configurationObjects;


    /**
     * Constructor if compression flag not have to be passed because the storage don't need this information.
     * {@code #isCompressed} is then {@code false}.
     *
     * @param existingConfigurations list of configurations
     */
    Configurations(List<ConfigurationObject> existingConfigurations) {
        this(existingConfigurations, false);
    }

    /**
     * @param existingConfigurations list of configurations
     * @param compressed             If the configuration was stored in a compressed way.
     */
    Configurations(List<ConfigurationObject> existingConfigurations, boolean compressed) {
        this.compressed = compressed;
        configurationObjects = ImmutableList.copyOf(existingConfigurations);
    }

    List<ConfigurationObject> getConfigurationObjects() {
        return configurationObjects;
    }

    public Configurations delete(ConfigurationId configurationId) {
        ImmutableList.Builder<ConfigurationObject> bdr = ImmutableList.builder();
        for (ConfigurationObject configurationObject : configurationObjects) {
            boolean isConfigIdToRemove = configurationObject.getPluginConfiguration().getId().equals(configurationId);
            if (!isConfigIdToRemove) {
                bdr.add(configurationObject);
            }

        }

        return new Configurations(bdr.build(), compressed);
    }

    Configurations addOrUpdate(ConfigurationObject toUpdate) {
        ImmutableList.Builder<ConfigurationObject> bdr = ImmutableList.builder();

        ConfigurationId updatedConfigurationsId = toUpdate.getPluginConfiguration().getId();

        boolean configurationWasUpdated = false;

        for (ConfigurationObject existingConfig : configurationObjects) {
            ConfigurationId currentConfigurationId = existingConfig.getPluginConfiguration().getId();
            boolean replaceThisWithNewVersion = currentConfigurationId.equals(updatedConfigurationsId);

            if (replaceThisWithNewVersion) {
                configurationWasUpdated = true;
                bdr.add(toUpdate);
            } else {
                bdr.add(existingConfig);
            }
        }

        if (!configurationWasUpdated) {
            bdr.add(toUpdate);
        }

        return new Configurations(bdr.build(), compressed);
    }

    boolean isCompressed() {
        return compressed;
    }

    public long getTimestamp() {
        long maxTs = -1;
        for (ConfigurationObject c : getConfigurationObjects()) {
            maxTs = Math.max(maxTs, c.getTimestamp());
        }
        return maxTs;
    }

    Configurations setCompressed(boolean compressed) {
        return new Configurations(configurationObjects, compressed);
    }
}
