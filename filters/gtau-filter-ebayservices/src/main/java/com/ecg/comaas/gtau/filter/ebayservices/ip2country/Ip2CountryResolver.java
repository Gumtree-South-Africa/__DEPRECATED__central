package com.ecg.comaas.gtau.filter.ebayservices.ip2country;

import com.ebay.ecg.gumtree.australia.lbs2.ServiceException;
import com.ebay.ecg.gumtree.australia.lbs2.TrackingGeoLocationService;
import com.ebay.ecg.gumtree.australia.lbs2.config.TrackingGeolocationConfiguration;
import com.ebay.ecg.gumtree.australia.lbs2.impl.TrackingGeoLocationServiceImpl;
import com.ebay.ecg.gumtree.australia.lbs2.payload.LocationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class Ip2CountryResolver {

    private static final Logger LOG = LoggerFactory.getLogger(Ip2CountryResolver.class);

    private TrackingGeoLocationService geoLocationService;

    public Ip2CountryResolver(TrackingGeolocationConfiguration config) {
        this(new TrackingGeoLocationServiceImpl(config));
    }

    private Ip2CountryResolver(TrackingGeoLocationService geoLocationService) {
        this.geoLocationService = geoLocationService;
    }

    Optional<String> resolve(String ipAddress) {
        try {
            LocationType location = geoLocationService.getLocation(ipAddress);
            if (location != null && location.getCountry() != null)
                return Optional.of(location.getCountry());
        } catch (ServiceException e) {
            LOG.warn("Error while accessing ebay geo location service, see also: " + e.getMessage());
            LOG.debug("The full error was: ", e);
        }

        return Optional.empty();
    }
}
