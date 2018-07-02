package com.ecg.comaas.core.filter.ebayservices.userstate.provider;

import com.ebay.marketplace.user.v1.services.MemberBadgeDataType;
import com.ebay.marketplace.user.v1.services.UserEnum;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.UserService;

public class UserStateBadgeBasedProvider extends UserStateProvider {

    private final UserService userService;

    public UserStateBadgeBasedProvider(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserEnum getSenderState(String sender) {
        try {
            MemberBadgeDataType badgeData = userService.getMemberBadgeData(sender);
            if (badgeData == null || badgeData.getUserState() == null) {
                return null;
            }
            return badgeData.getUserState();
        } catch (ServiceException e) {
            handleException(e);
        }
        return null;
    }
}
