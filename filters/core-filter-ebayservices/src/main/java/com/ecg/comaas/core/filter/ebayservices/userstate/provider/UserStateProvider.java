package com.ecg.comaas.core.filter.ebayservices.userstate.provider;

import com.ebay.marketplace.user.v1.services.UserEnum;
import de.mobile.ebay.service.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UserStateProvider {

    private static final Logger LOG = LoggerFactory.getLogger(UserStateProvider.class);

    protected static void handleException(ServiceException e) {
        String message = e.getMessage();
        if (message == null) {
            LOG.error("Error while calling eBay Service. No Details given", e);
        } else if (!message.contains("User Not Found")) {
            LOG.warn("Error while calling ebay service, details: {}", message);
        }
    }

    public abstract UserEnum getSenderState(String sender);
}
