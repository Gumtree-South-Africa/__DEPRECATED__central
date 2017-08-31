package ca.kijiji.replyts.user_behaviour.responsiveness;

import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.HystrixCommandConfigurationProvider;
import com.netflix.hystrix.HystrixCommand;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class TestBaseSpringContext {

    private static final String CURRENT_PACKAGE = "ca.kijiji.replyts.user_behaviour.responsiveness";
    protected static final String USER_RESPONSIVENESS_ENABLED_PROP = "user-behaviour.responsiveness.enabled";

    protected ConfigurableEnvironment environment;
    protected ConfigurableApplicationContext context;

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

    protected void setProperty(String key, String value) {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(environment, key + "=" + value);
    }

    protected void initContext() {
        context = new SpringApplicationBuilder(TestConfiguration.class)
                .environment(environment)
                .web(false)
                .bannerMode(Banner.Mode.OFF)
                .run();
    }

    protected List<String> getResponsivenessBeansInScope() {
        return Arrays.stream(context.getBeanDefinitionNames())
                .map(beanName -> context.getBean(beanName).getClass().getName())
                .filter(beanClassName -> beanClassName.startsWith(CURRENT_PACKAGE))
                .collect(Collectors.toList());
    }

    @Configuration
    @ComponentScan(
            basePackages = CURRENT_PACKAGE,
            excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = CURRENT_PACKAGE + ".*Test.*")
    )
    static class TestConfiguration {

        @Bean
        @Qualifier("userBehaviourHystrixConfig")
        public HystrixCommand.Setter userBehaviourHystrixConfig() {
            return HystrixCommandConfigurationProvider.provideUserBehaviourConfig(true);
        }
    }
}
