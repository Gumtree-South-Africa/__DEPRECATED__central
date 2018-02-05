package com.ecg.messagebox.resources;

import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagebox.service.ResponseDataService;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@ComponentScan("com.ecg.messagebox.resources")
@TestPropertySource(properties = "spring.cloud.consul.enabled = false")
@ContextConfiguration(classes = AbstractTest.Configuration.class)
public abstract class AbstractTest {

    static final String USER_ID = "USER_ID";
    static final String CONVERSATION_ID = "CONVERSATION_ID";

    public static class Configuration {

        @Bean
        PostBoxService postBoxService() {
            return mock(PostBoxService.class);
        }

        @Bean
        ResponseDataService responseDataService() {
            return mock(ResponseDataService.class);
        }
    }
}
