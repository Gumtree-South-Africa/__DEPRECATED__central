package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service.HystrixCommandConfigurationProvider;
import com.netflix.hystrix.HystrixCommand;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ResponsivenessKafkaSinkIntegrationTest.TestConfig.class)
@TestPropertySource(properties = {
        "user-behaviour.responsiveness.enabled=true",
        "user-behaviour.responsiveness.sink=queue",
        "kafka.core.servers=localhost:9092",
})
public class ResponsivenessKafkaSinkIntegrationTest {

    private static final long DEFAULT_READ_TIMEOUT = 10_000;

    @Autowired
    private ResponsivenessKafkaSink sink;

    @Autowired
    private TestResponsivenessKafkaConsumer consumer;

    @Test
    public void whenRecordStored_shouldBeConsumed() {
        ResponsivenessRecord expected = defaultRecord();
        sink.storeRecord("ignored", expected);

        ConsumerRecords<String, ResponsivenessRecord> actualRecords = consumer.getConsumer().poll(DEFAULT_READ_TIMEOUT);

        assertThat(actualRecords.count()).isEqualTo(1);
        assertThat(actualRecords.iterator().next().value()).isEqualTo(expected);
    }

    @Test
    public void whenMultipleRecordStored_consumerRecordsShouldHaveCorrectSize() {
        sink.storeRecord("ignored", defaultRecord());
        sink.storeRecord("ignored", defaultRecord());

        ConsumerRecords<String, ResponsivenessRecord> actualRecords = consumer.getConsumer().poll(DEFAULT_READ_TIMEOUT);

        assertThat(actualRecords.count()).isEqualTo(2);
    }

    private static ResponsivenessRecord defaultRecord() {
        return new ResponsivenessRecord(1, 1L, "c1", "m1", 10, Instant.now());
    }

    @Configuration
    @ComponentScan(basePackages = "ca.kijiji.replyts.user_behaviour.responsiveness")
    static class TestConfig {

        @Bean
        @Qualifier("userBehaviourHystrixConfig")
        public HystrixCommand.Setter userBehaviourHystrixConfig() {
            return HystrixCommandConfigurationProvider.provideUserBehaviourConfig(true);
        }
    }
}
