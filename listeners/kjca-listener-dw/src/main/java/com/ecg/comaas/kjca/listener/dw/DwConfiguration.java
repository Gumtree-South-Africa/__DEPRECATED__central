package com.ecg.comaas.kjca.listener.dw;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;

@ComaasPlugin
@Profile({TENANT_KJCA, TENANT_MVCA})
@Configuration
@ImportResource("/jms-configuration.xml")
public class DwConfiguration {

    @Bean
    public ActiveMQReporter activeMQReporter(
            JmsTemplate jmsTemplate,
            @Value("${mailcloaking.localized.buyer}") String buyerPrefix,
            @Value("${mailcloaking.localized.seller}") String sellerPrefix,
            // Do not ever change this, you will break replies to existing messages.
            @Value("${mailcloaking.seperator:.}") String mailCloakingSeparator) {

        return new ActiveMQReporter(jmsTemplate, buyerPrefix, sellerPrefix, mailCloakingSeparator);
    }
}
