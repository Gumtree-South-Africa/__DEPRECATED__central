package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;


import com.ebay.ecg.gumtree.australia.lbs2.ServiceException;
import com.ebay.ecg.gumtree.australia.lbs2.TrackingGeoLocationService;
import com.ebay.ecg.gumtree.australia.lbs2.config.TrackingGeolocationConfiguration;
import com.ebay.ecg.gumtree.australia.lbs2.impl.TrackingGeoLocationServiceImpl;
import com.ebay.ecg.gumtree.australia.lbs2.payload.LocationType;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * User: acharton
 * Date: 12/17/12
 */
@Component
class Ip2CountryResolver {

    private static final Logger LOG = LoggerFactory.getLogger(Ip2CountryResolver.class);

    private TrackingGeoLocationService geoLocationService;

    @Autowired
    public Ip2CountryResolver(@Qualifier("esconfig-ipc") TrackingGeolocationConfiguration config) {
        this(new TrackingGeoLocationServiceImpl(config));
    }

    Ip2CountryResolver(TrackingGeoLocationService geoLocationService) {
        this.geoLocationService = geoLocationService;
    }

    public Optional<String> resolve(String ipAddress) {

        try {
            LocationType location = geoLocationService.getLocation(ipAddress);
            if(location != null && location.getCountry() != null)
                return Optional.of(location.getCountry());
        } catch (ServiceException e) {
            LOG.warn("Error while accessing ebay geo location service, see also: " + e.getMessage());
            LOG.debug("The full error was: ", e);
        }

        return Optional.absent();

    }
}
