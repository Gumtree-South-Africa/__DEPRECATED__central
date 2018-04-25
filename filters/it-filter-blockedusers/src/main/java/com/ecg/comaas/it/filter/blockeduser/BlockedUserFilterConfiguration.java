package com.ecg.comaas.it.filter.blockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_IT;

@ComaasPlugin
@Profile(TENANT_IT)
@Configuration
@ComponentScan
public class BlockedUserFilterConfiguration { }
