package com.ecg.comaas.mp.filter.volume;

import com.ecg.comaas.mp.filter.volume.persistence.CassandraVolumeFilterEventRepository;
import com.ecg.comaas.mp.filter.volume.persistence.VolumeFilterEventRepository;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_BE;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MP;

@ComaasPlugin
@Profile({TENANT_MP, TENANT_BE})
@Component
@Import({ CassandraVolumeFilterEventRepository.class, VolumeFilterConfigParser.class })
public class VolumeFilterFactory implements FilterFactory {

    public static final String IDENTIFIER = "nl.marktplaats.filter.volume.VolumeFilterFactory";

    private VolumeFilterEventRepository volumeFilterEventRepository;
    private VolumeFilterConfigParser volumeFilterConfigParser;

    @Autowired
    public VolumeFilterFactory(VolumeFilterEventRepository volumeFilterEventRepository, VolumeFilterConfigParser volumeFilterConfigParser) {
        this.volumeFilterEventRepository = volumeFilterEventRepository;
        this.volumeFilterConfigParser = volumeFilterConfigParser;
    }

    @Override
    public Filter createPlugin(String instanceName, JsonNode configurationAsJson) {
        VolumeFilterConfiguration config = volumeFilterConfigParser.parse(configurationAsJson);
        return new VolumeFilter(config, volumeFilterEventRepository);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
