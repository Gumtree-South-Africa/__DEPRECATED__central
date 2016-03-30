package com.ecg.replyts.core.runtime;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class EnvironmentSupport {
    public static Set<String> propertyNames(AbstractEnvironment environment) {
        Set<String> propertyNames = new HashSet<>();

        for (PropertySource source : environment.getPropertySources())
            if (source instanceof EnumerablePropertySource)
                propertyNames.addAll(Arrays.asList(((EnumerablePropertySource) source).getPropertyNames()));

        return propertyNames;
    }
}
