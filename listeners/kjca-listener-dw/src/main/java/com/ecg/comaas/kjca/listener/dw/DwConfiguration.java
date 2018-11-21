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
@ImportResource("/jmx-configuration.xml")
public class DwConfiguration {

//    private static final String AMQ_CONN_ARGS =
//            "wireFormat.maxInactivityDurationInitalDelay=5000" +
//                    "&wireFormat.maxInactivityDuration=10000" +
//                    "&wireFormat.stackTraceEnabled=true" +
//                    "&keepAlive=true" +
//                    "&soTimeout=3000" +
//                    "&soWriteTimeout=3000" +
//                    "&connectionTimeout=3000";

//    @Bean(initMethod = "start", destroyMethod = "stop")
//    public PooledConnectionFactory jmsConnectionFactory(
//            @Value("${activemq.broker.protocol:tcp}") String amqProtocol,
//            @Value("${activemq.broker.host:localhost}") String brokerHost,
//            @Value("${activemq.standby.broker.host:localhost}") String standbyHost,
//            @Value("${activemq.broker.port:61616}") int amqPort,
//            @Value("${activemq.broker.maxConnections:100}") int maxConnections) {
//
//        String amqPrimary = createAmqUrl(amqProtocol, brokerHost, amqPort);
//        String amqStandby = createAmqUrl(amqProtocol, standbyHost, amqPort);
//
//        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
//        activeMQConnectionFactory.setBrokerURL(createAmqFailoverUrl(amqPrimary, amqStandby));
//
//        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
//        connectionFactory.setConnectionFactory(activeMQConnectionFactory);
//        connectionFactory.setMaxConnections(maxConnections);
//        return connectionFactory;
//    }

    @Bean
    public ActiveMQReporter activeMQReporter(
            @Qualifier("dwJmsTemplate") JmsTemplate jmsTemplate,
            @Value("${mailcloaking.localized.buyer}") String buyerPrefix,
            @Value("${mailcloaking.localized.seller}") String sellerPrefix,
            // Do not ever change this, you will break replies to existing messages.
            @Value("${mailcloaking.seperator:.}") String mailCloakingSeparator) {

        return new ActiveMQReporter(jmsTemplate, buyerPrefix, sellerPrefix, mailCloakingSeparator);
    }

//    private static String createAmqUrl(String protocol, String host, int port) {
//        return protocol + "://" + host + ":" + port + "?" + AMQ_CONN_ARGS;
//    }
//
//    private static String createAmqFailoverUrl(String primary, String standby) {
//        return "failover://(" + primary + "," + standby + ")?randomize=false&initialReconnectDelay=100&maxReconnectDelay=2000&timeout=1&priorityBackup=true";
//    }

    @Bean
    public JmsTransactionManager jmsTransactionManager(PooledConnectionFactory connectionFactory) {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(connectionFactory);
        return new JmsTransactionManager();
    }

    @Bean
    public JmsTemplate dwJmsTemplate(PooledConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }
}
