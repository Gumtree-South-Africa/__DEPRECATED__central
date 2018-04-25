package com.ecg.comaas.gtau.listener.messageevent;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;
import static com.ecg.replyts.integration.test.MailBuilder.aNewMail;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageEventListenerIntegrationTest {

    private static final String EXCHANGE = "gt.topic.default";
    private static final String BINDING_QUEUE = "bind.to.exchange";
    private static final String BUYER_EMAIL = "buyer@example.com";
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String AD_ID = "12345";

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule(createProperties());

    private Properties createProperties() {
        Properties properties = propertiesWithTenant(TENANT_GTAU);
        properties.put("rabbitmq.host", "localhost");
        properties.put("rabbitmq.port", "5672");
        properties.put("rabbitmq.virtualHost", "/");
        properties.put("rabbitmq.username", "guest");
        properties.put("rabbitmq.password", "guest");
        properties.put("rabbitmq.connectionTimeout", "1000");
        return properties;
    }

    private Connection connection;
    private Channel channel;

    @Before
    public void setUp() throws RabbitMQTestException {
        connection = RabbitMQTestUtils.openConnection("localhost");
        channel = RabbitMQTestUtils.createChannelWithBinding(connection, EXCHANGE, BINDING_QUEUE);
    }

    @Test
    public void whenMessageProcessed_shouldPutEventToRabbitMq() throws RabbitMQTestException {
        testRule.deliver(
                aNewMail()
                        .from(BUYER_EMAIL)
                        .to(SELLER_EMAIL)
                        .adId(AD_ID)
                        .plainBody("A sample message")
        );
        testRule.waitForMail();

        String actual = RabbitMQTestUtils.consumeOne(channel, BINDING_QUEUE, 5L, TimeUnit.SECONDS);

        // checking if proper object was created and proto-buffed to RabbitMQ
        assertThat(actual).contains("MessageEvents$MessageCreatedEvent");
        assertThat(actual).contains(BUYER_EMAIL);
        assertThat(actual).contains(SELLER_EMAIL);
        assertThat(actual).contains(AD_ID);
    }

    @After
    public void tearDown() throws RabbitMQTestException {
        RabbitMQTestUtils.close(connection, channel);
    }
}
