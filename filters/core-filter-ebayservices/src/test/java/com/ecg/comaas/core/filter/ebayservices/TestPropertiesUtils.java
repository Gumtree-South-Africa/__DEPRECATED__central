package com.ecg.comaas.core.filter.ebayservices;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

public final class TestPropertiesUtils {

    private static final String EBAYK_PROPERTIES = "ebayservices_ebayk.properties";
    private static final String GTAU_PROPERTIES = "ebayservices_gtau.properties";

    private TestPropertiesUtils() {
    }

    public static Properties getProperties(String tenant) {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String propertyFile = TENANT_EBAYK.equalsIgnoreCase(tenant) ? EBAYK_PROPERTIES : GTAU_PROPERTIES;
        InputStream is = classloader.getResourceAsStream(propertyFile);
        try {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Properties ", e);
        }
    }
}
