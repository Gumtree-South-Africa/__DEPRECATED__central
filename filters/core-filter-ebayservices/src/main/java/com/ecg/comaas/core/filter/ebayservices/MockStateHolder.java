package com.ecg.comaas.core.filter.ebayservices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MockStateHolder {

    private static final Logger logger = LoggerFactory.getLogger(MockStateHolder.class);

    private static final Map<String, String> mockResponses = new HashMap<>();

    public static void loadFromFile(InputStream resource) {
        try (InputStreamReader streamReader = new InputStreamReader(resource);
             BufferedReader bufferedReader = new BufferedReader(streamReader);) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split("\\|");
                mockResponses.put(split[0], split[1]);
            }
        } catch (IOException e) {
            logger.error("Can't load mock data", e);
        }
    }

    public static boolean containsKey(String url) {
        return mockResponses.containsKey(url);
    }

    public static String get(String url) {
        return mockResponses.get(url);
    }
}
