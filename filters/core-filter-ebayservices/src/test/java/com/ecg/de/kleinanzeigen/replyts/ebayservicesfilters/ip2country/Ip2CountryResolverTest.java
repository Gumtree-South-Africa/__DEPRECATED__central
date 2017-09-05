package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.ip2country;

import com.ebay.marketplace.personalization.v1.services.LocationType;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.TrackingGeoLocationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * User: acharton
 * Date: 12/17/12
 */
@RunWith(MockitoJUnitRunner.class)
public class Ip2CountryResolverTest {

    private Ip2CountryResolver ip2CountryResolver;

    @Mock
    private TrackingGeoLocationService trackingGeoLocationService;

    @Mock
    private LocationType geoLocation;


    @Before
    public void setUp() throws Exception {
        ip2CountryResolver = new Ip2CountryResolver(trackingGeoLocationService);
        when(trackingGeoLocationService.getLocation(anyString())).thenReturn(geoLocation);
    }

    @Test
    public void resolvesIp() throws Exception {
        when(geoLocation.getCountry()).thenReturn("DE");

        assertEquals("DE", ip2CountryResolver.resolve("127.0.0.1").get());
    }

    @Test
    public void absentOnServiceError() throws Exception {
        when(trackingGeoLocationService.getLocation(anyString())).thenThrow(new ServiceException("error"));

        assertFalse(ip2CountryResolver.resolve("123.123.123.123").isPresent());
    }

    @Test
    public void absentOnNullResult() throws Exception {

        when(trackingGeoLocationService.getLocation(anyString())).thenReturn(null);

        assertFalse(ip2CountryResolver.resolve("123.123.123.123").isPresent());

    }
}
