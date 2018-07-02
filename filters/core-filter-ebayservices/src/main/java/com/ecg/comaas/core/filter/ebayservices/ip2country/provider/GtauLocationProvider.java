package com.ecg.comaas.core.filter.ebayservices.ip2country.provider;

import com.ebay.ecg.gumtree.australia.lbs2.ServiceException;
import com.ebay.ecg.gumtree.australia.lbs2.TrackingGeoLocationService;
import com.ebay.ecg.gumtree.australia.lbs2.payload.LocationType;

import java.util.Optional;

/**
 * Different from {@link EbaykLocationProvider}, look at the imports
 */
public class GtauLocationProvider extends LocationProvider {

    private final TrackingGeoLocationService geoLocationService;

    public GtauLocationProvider(TrackingGeoLocationService geoLocationService) {
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
