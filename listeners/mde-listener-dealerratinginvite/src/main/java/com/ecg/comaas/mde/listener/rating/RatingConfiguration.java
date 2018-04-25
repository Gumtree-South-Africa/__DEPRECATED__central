package com.ecg.comaas.mde.listener.rating;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_MDE;

@ComaasPlugin
@Profile(TENANT_MDE)
@Configuration
public class RatingConfiguration {

    @Value("${replyts.mobile.dealerRatingService.webserviceUrl}")
    private String webserviceUrl;

    @Value("${replyts.mobile.dealerRatingService.active}")
    private boolean active;

    @Bean
    public DealerRatingService dealerRatingService() {
        return new DealerRatingService(webserviceUrl, active);
    }

    @Bean
    public DealerRatingInviteListener dealerRatingInviteListener(DealerRatingService service) {
        return new DealerRatingInviteListener(service);
    }
}
