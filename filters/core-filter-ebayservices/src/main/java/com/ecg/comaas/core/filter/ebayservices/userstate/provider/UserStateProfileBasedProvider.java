package com.ecg.comaas.core.filter.ebayservices.userstate.provider;

import com.ebay.marketplace.user.v1.services.UserEnum;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserProfileService;
import de.mobile.ebay.service.userprofile.domain.AccountStatus;
import de.mobile.ebay.service.userprofile.domain.User;

public class UserStateProfileBasedProvider extends UserStateProvider {

    private final UserProfileService userProfileService;

    public UserStateProfileBasedProvider(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    private static UserEnum fromAccountStatus(AccountStatus userAccountStatus) {
        switch (userAccountStatus) {
            case CONFIRMED:
                return UserEnum.CONFIRMED;
            case SUSPENDED:
                return UserEnum.SUSPENDED;
            default:
                return UserEnum.UNKNOWN;
        }
    }

    @Override
    public UserEnum getSenderState(String sender) {
        try {
            User fromUser = userProfileService.getUser(sender);
            if (fromUser == null || fromUser.getUserAccountStatus() == null) {
                return null;
            }
            return fromAccountStatus(fromUser.getUserAccountStatus());
        } catch (ServiceException e) {
            handleException(e);
        }
        return null;
    }
}
