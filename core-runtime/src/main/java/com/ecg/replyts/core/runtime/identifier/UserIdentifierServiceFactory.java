package com.ecg.replyts.core.runtime.identifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public class UserIdentifierServiceFactory {

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

    public UserIdentifierServiceFactory() {}


    public UserIdentifierService createUserIdentifierService() {
        if (userIdentifierType == UserIdentifierType.BY_MAIL && userIdDisabledForTenant()) {
            return new UserIdentifierServiceByMailAddress();
        } else {
            return new UserIdentifierServiceByUserIdHeaders(buyerUserIdName, sellerUserIdName);
        }
    }

    /* Only enable user identification by user id for marktplaats since other tenants are not ready yet  */
    private boolean userIdDisabledForTenant() {
        return !tenant.equals("mp");
    }

}
