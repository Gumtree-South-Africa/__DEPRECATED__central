package com.ecg.comaas.gtuk.filter.geoiplookup;

import com.gumtree.common.geoip.GeoIpService;
import com.gumtree.common.geoip.MaxMindGeoIpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

@Configuration
public class GumtreeGeoIpLookupConfiguration {
    @Bean
    GeoIpService geoIpService() {
        try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("GeoIPCountryCSV.zip");
            if (in == null) {
                throw new IllegalStateException("GeoIPCountryCSV.zip not found or not readable.");
            }
            ZipInputStream zipInputStream = new ZipInputStream(in);
            zipInputStream.getNextEntry();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(zipInputStream, "UTF-8"));
            return new MaxMindGeoIpService(bufferedReader);
        } catch (IOException e) {
            throw new RuntimeException("Could not open GeoIP country CSV", e);
        }
    }

}
