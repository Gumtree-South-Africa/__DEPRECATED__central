package com.ecg.comaas.gtuk.filter.geoiplookup;

public interface GeoIpService {

    /**
     * Get the country code of an IP address
     * @param ip the IP address
     * @return The country code e.g. 'GB'
     */
    String getCountryCode(String ip);
}