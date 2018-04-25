package com.ecg.replyts.integration.test.support;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

import static com.ecg.replyts.core.api.model.Tenants.TENANT;

public abstract class IntegrationTestUtils {
    /**
     * This sets a system level environment variable.
     * Obviously, this should never be done from a java process, which is why there are several hoops to jump through.
     * This should only be used for integration tests, and is currently only in use for setting the listening http port,
     * which can be removed when we move to Spring Boot, or Nomad
     *
     * @param key   the env var you want to set
     * @param value the value to set it to
     */
    @SuppressWarnings({"JavaReflectionMemberAccess", "unchecked"})
    public static void setEnv(String key, String value) {
        try {
            Map<String, String> env = System.getenv();
            Class<?> cl = env.getClass();
            Field field = cl.getDeclaredField("m");
            field.setAccessible(true);
            Map<String, String> writableEnv = (Map<String, String>) field.get(env);
            writableEnv.put(key, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set environment variable", e);
        }
    }

    public static Properties propertiesWithTenant(String tenant) {
        Properties properties = new Properties();
        properties.put(TENANT, tenant);
        return properties;
    }
}
