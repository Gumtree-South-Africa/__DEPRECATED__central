package com.ecg.comaas.ebayk.listener.dailyreport;

import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.HadoopEventKafkaEmitter;
import com.ecg.comaas.ebayk.listener.dailyreport.hadoop.KafkaHadoopLogConfig;
import com.ecg.comaas.ebayk.listener.dailyreport.messagestate.MessageStateListener;
import com.ecg.comaas.ebayk.listener.dailyreport.ruleperformance.FilterRulePerformanceListener;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.UUID;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_EBAYK;

@ComaasPlugin
@Profile(TENANT_EBAYK)
@Configuration
@Import(KafkaHadoopLogConfig.class)
public class DailyReportConfiguration {

    @Bean
    public HadoopEventKafkaEmitter hadoopEventKafkaEmitter(@Qualifier("kafkaUUIDProducer") AsyncProducer<UUID, String> producer) {
        return new HadoopEventKafkaEmitter(producer);
    }

    @Bean
    public MessageStateListener messageStateListener(HadoopEventKafkaEmitter emitter) {
        return new MessageStateListener(emitter);
    }

    @Bean
    public FilterRulePerformanceListener filterRulePerformanceListener(HadoopEventKafkaEmitter emitter) {
        return new FilterRulePerformanceListener(emitter);
    }
}
