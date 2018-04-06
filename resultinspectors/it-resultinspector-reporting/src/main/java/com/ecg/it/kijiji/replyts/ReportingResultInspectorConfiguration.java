package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(ReportingResultInspectorFactory.class)
public class ReportingResultInspectorConfiguration {
    @Bean
    public StatsDClient statsDClient(@Value("${statsd.wordfilter.report.hostname:statsd}") String hostname,
                                     @Value("${statsd.wordfilter.report.port:8125}") Integer port,
                                     @Value("${statsd.wordfilter.report.prefix:wordfilter}") String prefix) throws Exception {
        return new NonBlockingStatsDClient(prefix, hostname, port);
    }
}