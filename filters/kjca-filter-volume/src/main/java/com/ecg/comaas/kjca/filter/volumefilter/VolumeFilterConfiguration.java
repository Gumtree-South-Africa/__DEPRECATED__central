package com.ecg.comaas.kjca.filter.volumefilter;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import({ VolumeFilterFactory.class, SharedBrain.class })
public class VolumeFilterConfiguration {
}