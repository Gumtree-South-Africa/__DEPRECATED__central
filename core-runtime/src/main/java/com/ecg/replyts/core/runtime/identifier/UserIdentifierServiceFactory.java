package com.ecg.replyts.core.runtime.identifier;

import com.ecg.replyts.core.runtime.cluster.ClusterModeManager;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.Set;

public class UserIdentifierServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterModeManager.class);
    private final Set<String> TENANTS_ENABLED_FOR_USER_ID = Sets.newHashSet("mp", "mde");

    @Value("${messagebox.userid.userIdentifierStrategy:BY_USER_ID}")
    private UserIdentifierType userIdentifierType;
    @Value("${messagebox.userid.by_user_id.customValueNameForBuyer:user-id-buyer}")
    private String buyerUserIdName;
    @Value("${messagebox.userid.by_user_id.customValueNameForSeller:user-id-seller}")
    private String sellerUserIdName;
    @Value("${replyts.tenant:no-tenant}")
    private String tenant;

    public UserIdentifierServiceFactory(UserIdentifierType type, String tenant) {
        this.tenant = tenant;
        this.userIdentifierType = type;
    }

    public UserIdentifierServiceFactory() {
    }

    public UserIdentifierService createUserIdentifierService() {
        LOG.info("creating UserIdentifierService for tenant {}. Data userIdentifierType=[{}], buyerUserIdName=[{}] sellerUserIdName=[{}]", tenant, userIdentifierType, buyerUserIdName, sellerUserIdName);
        if (userIdentifierType == UserIdentifierType.BY_MAIL && userIdDisabledForTenant()) {
            LOG.info("tenant {} use UserIdentifierServiceByMailAddress", tenant);
            return new UserIdentifierServiceByMailAddress();
        } else {
            LOG.info("tenant {} use UserIdentifierServiceByUserIdHeaders", tenant);
            return new UserIdentifierServiceByUserIdHeaders(buyerUserIdName, sellerUserIdName);
        }
    }

    private boolean userIdDisabledForTenant() {
        return !TENANTS_ENABLED_FOR_USER_ID.contains(tenant);
    }
}