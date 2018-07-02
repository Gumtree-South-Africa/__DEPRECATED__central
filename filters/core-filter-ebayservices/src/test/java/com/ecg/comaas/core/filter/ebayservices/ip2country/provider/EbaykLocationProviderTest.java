package com.ecg.comaas.core.filter.ebayservices.ip2country.provider;

import com.ebay.marketplace.personalization.v1.services.LocationType;
import de.mobile.ebay.service.ServiceException;
import de.mobile.ebay.service.TrackingGeoLocationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Different from {@link GtauLocationProviderTest}, look at the imports
 * JUnit parametrization is not really possible in this case
 */
@RunWith(MockitoJUnitRunner.class)
public class EbaykLocationProviderTest {

    private static final String DEFAULT_IP_ADDRESS = "127.0.0.1";

    private LocationProvider locationProvider;

    @Mock
    private TrackingGeoLocationService geoLocationServiceMock;

    @Mock
    private LocationType locationTypeMock;

    @Before
    public void setUp() throws ServiceException {
        when(geoLocationServiceMock.getLocation(DEFAULT_IP_ADDRESS)).thenReturn(locationTypeMock);
        when(locationTypeMock.getCountry()).thenReturn("NL");
        locationProvider = new EbaykLocationProvider(geoLocationServiceMock);
    }

    @Test
    public void whenLocationServiceThrowsException_shouldReturnEmptyOptional() throws ServiceException {
        when(geoLocationServiceMock.getLocation(anyString())).thenThrow(new ServiceException("exception"));
        Optional<String> actual = locationProvider.getCountry(DEFAULT_IP_ADDRESS);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenLocationNull_shouldReturnEmptyOptional() throws ServiceException {
        when(geoLocationServiceMock.getLocation(anyString())).thenReturn(null);
        Optional<String> actual = locationProvider.getCountry(DEFAULT_IP_ADDRESS);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenLocationCountryNull_shouldReturnEmptyOptional() throws ServiceException {
        when(locationTypeMock.getCountry()).thenReturn(null);
        Optional<String> actual = locationProvider.getCountry(DEFAULT_IP_ADDRESS);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenLocationCountryNotNull_shouldReturnOptionalWithCountry() throws ServiceException {
        Optional<String> actual = locationProvider.getCountry(DEFAULT_IP_ADDRESS);
        assertThat(actual).isPresent();
        assertThat(actual.get()).isEqualTo("NL");
    }
}
