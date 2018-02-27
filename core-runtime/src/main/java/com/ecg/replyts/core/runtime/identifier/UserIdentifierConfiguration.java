package com.ecg.replyts.core.runtime.identifier;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class UserIdentifierConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(UserIdentifierConfiguration.class);

    private static final Set<String> TENANTS_ENABLED_FOR_USER_ID = Sets.newHashSet("mp", "mde");

    @Value("${messagebox.userid.userIdentifierStrategy:BY_USER_ID}")
    private UserIdentifierType userIdentifierType;

    @Value("${messagebox.userid.by_user_id.customValueNameForBuyer:user-id-buyer}")
    private String buyerUserIdName;

    @Value("${messagebox.userid.by_user_id.customValueNameForSeller:user-id-seller}")
    private String sellerUserIdName;

    @Value("${replyts.tenant:no-tenant}")
    private String tenant;

    @Bean
    public UserIdentifierService createUserIdentifierService() {
        LOG.info("Creating UserIdentifierService for tenant {}. Data userIdentifierType=[{}], buyerUserIdName=[{}] sellerUserIdName=[{}]", tenant, userIdentifierType, buyerUserIdName, sellerUserIdName);

        if (userIdentifierType == UserIdentifierType.BY_MAIL && !TENANTS_ENABLED_FOR_USER_ID.contains(tenant)) {
            LOG.info("Tenant {} use UserIdentifierServiceByMailAddress", tenant);

            return new UserIdentifierServiceByMailAddress();

        } else if (userIdentifierType == UserIdentifierType.BY_UK_USER_ID) {
            LOG.info("Tenant {} use UkUserIdentifierServiceByUserIdHeaders", tenant);

            return new UkUserIdentifierServiceByUserIdHeaders(buyerUserIdName, sellerUserIdName);
        } else {
            LOG.info("Tenant {} use UserIdentifierServiceByUserIdHeaders", tenant);

            return new UserIdentifierServiceByUserIdHeaders(buyerUserIdName, sellerUserIdName);
        }
    }
}