package com.ecg.messagecenter;

import com.ecg.messagecenter.persistence.JsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.PostBoxToJsonConverter;
import com.ecg.messagecenter.persistence.simple.AbstractJsonToPostBoxConverter;
import com.ecg.messagecenter.persistence.simple.AbstractPostBoxToJsonConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by jaludden on 30/05/2017.
 */
@Configuration
@ComponentScan("com.ecg.de.ebayk.messagecenter")
public class MessageCenterConfiguration {

    @Bean
    public AbstractPostBoxToJsonConverter getToJson() {
        return new PostBoxToJsonConverter();
    }

    @Bean
    public AbstractJsonToPostBoxConverter getToPostBox() {
        return new JsonToPostBoxConverter();
    }

}
