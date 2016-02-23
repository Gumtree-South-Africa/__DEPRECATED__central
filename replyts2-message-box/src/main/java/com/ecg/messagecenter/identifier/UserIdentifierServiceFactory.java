package com.ecg.messagecenter.identifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Created by beckart on 15.10.15.
 */
public class UserIdentifierServiceFactory {

    private UserIdentifierType userIdentifierType;

    private String buyerUserIdName;

    private String sellerUserIdName;

    @Autowired
    public UserIdentifierServiceFactory(@Value("${messagebox.userid.userIdentifierStrategy:BY_USER_ID}") UserIdentifierType userIdentifierType, // Proposed new property name
                                        @Value("${messagebox.userid.by_user_id.customValueNameForBuyer:user-id-buyer}") String buyerUserIdName,
                                        @Value("${messagebox.userid.by_user_id.customValueNameForSeller:user-id-seller}") String sellerUserIdName) {
        this.userIdentifierType = userIdentifierType;
        this.buyerUserIdName = buyerUserIdName;
        this.sellerUserIdName = sellerUserIdName;
    }

    public UserIdentifierService createUserIdentifierService() {

        if(userIdentifierType == UserIdentifierType.BY_MAIL) {
            return new UserIdentifierServiceByMailAddress();
        } else {
            return new UserIdentifierServiceByUserIdHeaders(buyerUserIdName, sellerUserIdName);
        }

    }

}
