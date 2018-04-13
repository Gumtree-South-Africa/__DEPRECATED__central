package com.ecg.comaas.kjca.listener.userbehaviour;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class UserResponsivenessDITest extends TestBaseSpringContext {

    @Test
    public void whenResponsivenessDisabled_shouldLoadOnlyConfiguration() {
        setProperty(USER_RESPONSIVENESS_ENABLED_PROP, Boolean.FALSE.toString());
        initContext();

        List<String> actualBeans = getResponsivenessBeansInScope();

        assertThat(actualBeans).hasSize(1);
        // should have only configuration bean which would actually be in Spring bean factory
        assertThat(actualBeans.get(0)).startsWith(TestBaseSpringContext.TestConfiguration.class.getName());
    }

    @Test
    public void whenResponsivenessEnabled_shouldLoadBeans() {
        setProperty("kafka.core.servers", "localhost:9092");
        setProperty(USER_RESPONSIVENESS_ENABLED_PROP, Boolean.TRUE.toString());
        initContext();

        List<String> actualBeans = getResponsivenessBeansInScope();

        assertThat(actualBeans).isNotEmpty();
        assertThat(actualBeans).contains(UserResponsivenessListener.class.getName());
    }
}
