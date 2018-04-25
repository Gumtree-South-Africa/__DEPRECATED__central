package com.ecg.comaas.kjca.filter.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;

@ComaasPlugin
@Profile(TENANT_KJCA)
@Configuration
@Import({ VolumeFilterFactory.class, SharedBrain.class })
public class VolumeFilterConfiguration {
}
