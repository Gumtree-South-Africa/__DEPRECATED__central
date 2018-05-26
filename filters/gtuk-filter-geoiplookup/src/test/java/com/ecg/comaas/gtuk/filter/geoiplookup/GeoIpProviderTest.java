package com.ecg.comaas.gtuk.filter.geoiplookup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class GeoIpProviderTest {
    private static final String IP_GB = "89.243.51.11";
    private static final String IP_US = "198.0.2.1";
    private static final String IP_NONE = "198.3.2.1";
    private GeoIpService geoIpService;

    @Before
    public void setUp() throws Exception {
        GumtreeGeoIpLookupConfiguration gumtreeGeoIpLookupFilterConfiguration = new GumtreeGeoIpLookupConfiguration();
        this.geoIpService = gumtreeGeoIpLookupFilterConfiguration.geoIpService();
    }

    @Test
    public void itReturnsTheRightCountryForAGivenIp() throws Exception {
        String country = geoIpService.getCountryCode(IP_GB);
        String country2 = geoIpService.getCountryCode(IP_US);
        assertThat(country).isNotEqualTo("");
        assertThat(country).isEqualTo("GB");
        assertThat(country2).isNotEqualTo("");
        assertThat(country2).isEqualTo("US");
    }

    @Test
    public void itReturnBlankIfIpNotInRange() throws Exception {
        String country = geoIpService.getCountryCode(IP_NONE);
        assertThat(country).isEmpty();
    }
}
