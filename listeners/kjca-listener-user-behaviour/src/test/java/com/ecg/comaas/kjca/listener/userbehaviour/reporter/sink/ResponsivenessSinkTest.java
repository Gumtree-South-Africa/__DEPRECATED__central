package com.ecg.comaas.kjca.listener.userbehaviour.reporter.sink;

import com.ecg.comaas.kjca.listener.userbehaviour.TestBaseSpringContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponsivenessSinkTest extends TestBaseSpringContext {

    private static final String FS_VALUE = "fs";
    private static final String QUEUE_VALUE = "queue";

    @Before
    public void setUp() {
        super.setUp();
        setProperty(USER_RESPONSIVENESS_ENABLED_PROP, Boolean.TRUE.toString());
        setProperty("kafka.core.servers", "localhost:9092");
    }

    @Test
    public void whenPropertyKeyIncorrect_shouldLoadFsSinkBean() {
        setProperty("incorrectKey", "incorrectValue");
        initContext();

        Set<String> actualBeans = context.getBeansOfType(ResponsivenessSink.class).keySet();

        assertThat(actualBeans).hasSize(1);
        assertThat(extractFirstClass(actualBeans)).isEqualTo(ResponsivenessFilesystemSink.class);
    }

    @Test(expected = Exception.class)
    public void whenPropertyValueIncorrect_shouldNotStartBeanFactory() {
        setSinkProperty("incorrectValue");
        initContext();

        Set<String> actualBeans = context.getBeansOfType(ResponsivenessSink.class).keySet();

        assertThat(actualBeans).isEmpty();
    }

    @Test
    public void whenFsSpecified_shouldLoadFsSinkBean() {
        setSinkProperty(FS_VALUE);
        initContext();

        Set<String> actualBeans = context.getBeansOfType(ResponsivenessSink.class).keySet();

        assertThat(actualBeans).hasSize(1);
        assertThat(extractFirstClass(actualBeans)).isEqualTo(ResponsivenessFilesystemSink.class);
    }

    @Test
    public void whenQueueSpecified_shouldLoadQueueSinkBean() {
        setSinkProperty(QUEUE_VALUE);
        initContext();

        Set<String> actualBeans = context.getBeansOfType(ResponsivenessSink.class).keySet();

        assertThat(actualBeans).hasSize(1);
        assertThat(extractFirstClass(actualBeans)).isEqualTo(ResponsivenessKafkaSink.class);
    }

    private Class<?> extractFirstClass(Set<String> beanNames) {
        if (beanNames == null || beanNames.isEmpty()) {
            return null;
        }
        String firstBean = beanNames.iterator().next();
        return context.getBean(firstBean).getClass();
    }

    private void setSinkProperty(String value) {
        setProperty("user-behaviour.responsiveness.sink", value);
    }
}
