package com.ecg.comaas.core.filter.ebayservices.ip2country.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public abstract class LocationProvider {

    private static final Logger LOG = LoggerFactory.getLogger(LocationProvider.class);

    protected static void handleException(Exception e) {
        LOG.warn("Error while accessing ebay geo location service, see also: " + e.getMessage());
        LOG.debug("The full error was: ", e);
    }

    public abstract Optional<String> getCountry(String ipAddress);
}
