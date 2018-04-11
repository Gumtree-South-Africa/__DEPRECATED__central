package com.ecg.comaas.gtau.listener.messageevent;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class RabbitMQTestUtils {

    private static final ConnectionFactory connectionFactory = new ConnectionFactory();

    private RabbitMQTestUtils() {
    }

    public static Connection openConnection(String host) throws RabbitMQTestException {
        try {
            connectionFactory.setHost(host);
            return connectionFactory.newConnection();
        } catch (IOException e) {
            throw new RabbitMQTestException("Connection to RabbitMQ " + host + " could not be established", e);
        }
    }

    public static Channel createChannelWithBinding(Connection connection, String exchange, String bindQueue) throws RabbitMQTestException {
        try {
            Channel channel = connection.createChannel();
            channel.queueDeclare(bindQueue, false, false, true, null);
            channel.queueBind(bindQueue, exchange, "");
            return channel;
        } catch (IOException e) {
            throw new RabbitMQTestException("Failed to create channel", e);
        }
    }

    public static String consumeOne(Channel channel, String queue, long timeout, TimeUnit timeoutUnit) throws RabbitMQTestException {
        final CountDownLatch countDown = new CountDownLatch(1);
        final AtomicReference<byte[]> result = new AtomicReference<>();
        try {
            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    result.set(body);
                    countDown.countDown();
                }
            };
            channel.basicConsume(queue, true, consumer);
            countDown.await(timeout, timeoutUnit);
            if (result.get() != null) {
                return new String(result.get(), Charset.forName("UTF-8"));
            }
            return null;
        } catch (Exception e) {
            throw new RabbitMQTestException("Consumption from queue " + queue + " failed", e);
        }
    }

    public static void close(Connection connection, Channel channel) throws RabbitMQTestException {
        try {
            if (channel.isOpen()) {
                channel.close();
            }
            if (connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            throw new RabbitMQTestException("Could not properly close context", e);
        }
    }
}
