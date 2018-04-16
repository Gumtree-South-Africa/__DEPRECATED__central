package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ebay.marketplace.personalization.v1.services.LocationType;
import com.google.common.base.Stopwatch;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.TrackingGeoLocationService;
import de.mobile.ebay.service.lbs.Config;
import de.mobile.ebay.service.lbs.TrackingGeoLocationServiceImpl;
import de.mobile.ebay.service.restclient.RestClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class Ip2CountryResolver {

    private static final Logger LOG = LoggerFactory.getLogger(Ip2CountryResolver.class);

    private final TrackingGeoLocationService geoLocationService;

    @Autowired
    public Ip2CountryResolver(RestClientFactory okHttpClientFactory, @Qualifier("lbs-esconfig-ipc") Config config) {
        this(new TrackingGeoLocationServiceImpl(okHttpClientFactory, config));
    }

    Ip2CountryResolver(TrackingGeoLocationService geoLocationService) {
        this.geoLocationService = geoLocationService;
    }

    public Optional<String> resolve(String ipAddress) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            LocationType location = geoLocationService.getLocation(ipAddress);
            if(location != null && location.getCountry() != null)
                return Optional.of(location.getCountry());
        } catch (ServiceException e) {
            LOG.warn("Error while accessing ebay geo location service ({}): {}", stopwatch, e.getMessage(), e);
        }

        return Optional.empty();

    }
}