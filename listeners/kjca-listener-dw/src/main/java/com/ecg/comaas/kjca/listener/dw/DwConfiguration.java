package com.ecg.comaas.kjca.listener.dw;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;

@ComaasPlugin
@Configuration
@Import(ActiveMQReporter.class)
@ComponentScan("com.ecg.comaas.kjca")
public class DwConfiguration {

    private static final String AMQ_CONN_ARGS =
            "wireFormat.maxInactivityDurationInitalDelay=5000" +
                    "&amp;wireFormat.maxInactivityDuration=10000" +
                    "&amp;wireFormat.stackTraceEnabled=true" +
                    "&amp;keepAlive=true&amp;soTimeout=3000" +
                    "&amp;soWriteTimeout=3000" +
                    "&amp;connectionTimeout=3000";

    @Bean(initMethod = "start", destroyMethod = "stop")
    public PooledConnectionFactory jmsConnectionFactory(
            @Value("${activemq.broker.protocol:tcp}") String amqProtocol,
            @Value("${activemq.broker.host:localhost}") String brokerHost,
            @Value("${activemq.standby.broker.host:localhost}") String standbyHost,
            @Value("${activemq.broker.port:61616}") int amqPort,
            @Value("${activemq.broker.maxConnections:100}") int maxConnections) {

        String amqPrimary = createAmqUrl(amqProtocol, brokerHost, amqPort);
        String amqStandby = createAmqUrl(amqProtocol, standbyHost, amqPort);

        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
        activeMQConnectionFactory.setBrokerURL(createAmqFailoverUrl(amqPrimary, amqStandby));

        PooledConnectionFactory connectionFactory = new PooledConnectionFactory();
        connectionFactory.setConnectionFactory(activeMQConnectionFactory);
        connectionFactory.setMaxConnections(maxConnections);
        return connectionFactory;
    }

    private static String createAmqUrl(String protocol, String host, int port) {
        return protocol + "://" + host + ":" + port + "?" + AMQ_CONN_ARGS;
    }

    private static String createAmqFailoverUrl(String primary, String standby) {
        return "failover://(" + primary + "," + standby + ")?randomize=false&amp;initialReconnectDelay=100&amp;maxReconnectDelay=2000&amp;timeout=1&amp;priorityBackup=true";
    }

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
