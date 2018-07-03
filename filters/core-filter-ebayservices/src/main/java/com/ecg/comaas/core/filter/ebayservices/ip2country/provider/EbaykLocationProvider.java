package com.ecg.comaas.core.filter.ebayservices.ip2country.provider;

import com.ebay.marketplace.personalization.v1.services.LocationType;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.TrackingGeoLocationService;

import java.util.Optional;

/**
 * Different from {@link GtauLocationProvider}, look at the imports
 */
public class EbaykLocationProvider extends LocationProvider {

    private final TrackingGeoLocationService geoLocationService;

    public EbaykLocationProvider(TrackingGeoLocationService geoLocationService) {
        this.geoLocationService = geoLocationService;
    }

    @Override
    public Optional<String> getCountry(String ipAddress) {
        try {
            LocationType location = geoLocationService.getLocation(ipAddress);
            if (location != null && location.getCountry() != null) {
                return Optional.of(location.getCountry());
            }
        } catch (ServiceException e) {
            handleException(e);
        }
        return Optional.empty();
    }
}
