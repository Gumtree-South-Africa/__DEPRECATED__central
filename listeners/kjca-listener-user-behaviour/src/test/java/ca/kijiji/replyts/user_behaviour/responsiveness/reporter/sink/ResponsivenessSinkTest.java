package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponsivenessSinkTest {

    private static final String SINK_PROP_KEY = "user-behaviour.responsiveness.sink";
    private static final String FS_VALUE = "fs";
    private static final String QUEUE_VALUE = "queue";

    private ConfigurableEnvironment environment;
    private ConfigurableApplicationContext context;

    @Before
    public void setUp() {
        environment = new StandardEnvironment();
        setProperty("spring.cloud.bootstrap.enabled", "false");
    }

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void whenPropertyKeyIncorrect_shouldLoadFsSinkBean() {
        setProperty("incorrectKey", "incorrectValue");
        setFsRequiredProperties();
        initContext();

        Map<String, ResponsivenessSink> actualBeans = context.getBeansOfType(ResponsivenessSink.class);

        assertThat(actualBeans).hasSize(1);
        assertThat(actualBeans).containsOnlyKeys(ResponsivenessFilesystemSink.class.getName());
    }

    @Test
    public void whenPropertyValueIncorrect_shouldNotLoadAnyBean() {
        setSinkProperty("incorrectValue");
        initContext();

        Map<String, ResponsivenessSink> actualBeans = context.getBeansOfType(ResponsivenessSink.class);

        assertThat(actualBeans).isEmpty();
    }

    @Test
    public void whenFsSpecified_shouldLoadFsSinkBean() {
        setSinkProperty(FS_VALUE);
        setFsRequiredProperties();
        initContext();

        Map<String, ResponsivenessSink> actualBeans = context.getBeansOfType(ResponsivenessSink.class);

        assertThat(actualBeans).hasSize(1);
        assertThat(actualBeans).containsOnlyKeys(ResponsivenessFilesystemSink.class.getName());
    }

    @Test
    public void whenQueueSpecified_shouldLoadQueueSinkBean() {
        setSinkProperty(QUEUE_VALUE);
        initContext();

        Map<String, ResponsivenessSink> actualBeans = context.getBeansOfType(ResponsivenessSink.class);

        assertThat(actualBeans).hasSize(1);
        assertThat(actualBeans).containsOnlyKeys(ResponsivenessKafkaSink.class.getName());
    }

    private void setFsRequiredProperties() {
        setProperty("user-behaviour.responsiveness.fs-export.dir", "/var/tmp/responsiveness");
        setProperty("user-behaviour.responsiveness.fs-export.everyNEvents", "0");
        setProperty("user-behaviour.responsiveness.fs-export.maxBufferedEvents", "0");
        setProperty("user-behaviour.responsiveness.fs-export.fileNamePrefixDuringWrite", "fakePrefix");
        setProperty("user-behaviour.responsiveness.fs-export.fileNamePrefixAfterFlush", "fakePrefix");
    }

    private void setSinkProperty(String value) {
        setProperty(SINK_PROP_KEY, value);
    }

    private void setProperty(String key, String value) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, key + "=" + value);
    }

    private void initContext() {
        context = new SpringApplicationBuilder(TestConfiguration.class)
                .environment(environment)
                .web(false)
                .bannerMode(Banner.Mode.OFF)
                .run();
    }

    @Configuration
    @Import({ResponsivenessKafkaSink.class, ResponsivenessFilesystemSink.class})
    static class TestConfiguration {

        @Bean
        public ResponsivenessKafkaProducer kafkaProducer() {
            // TODO: refactor???
            return new ResponsivenessKafkaProducer(/*"localhost:8080"*/);
        }
    }
}
