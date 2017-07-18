package com.ecg.kijijiit.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.ecg.replyts.core.runtime.ComaasPlugin;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by fmaffioletti on 10/28/15.
 */
@ComaasPlugin
@Configuration
public class ReportingResultInspectorConfiguration {

    private static final Logger LOG =
                    LoggerFactory.getLogger(ReportingResultInspectorConfiguration.class);

    private StatsDClient statsDClient;

    public ReportingResultInspectorConfiguration(@Value("${statsd.wordfilter.report.hostname:statsd}") String hostname,
                                                 @Value("${statsd.wordfilter.report.port:8125}") Integer port,
                                                 @Value("${statsd.wordfilter.report.prefix:wordfilter}") String prefix) {
        try {
            statsDClient = new NonBlockingStatsDClient(prefix, hostname, port);
        } catch (Exception e) {
            LOG.error("Failed to create the statsd client", e);
        }
    }

    @Bean
    public ResultInspectorFactory resultInspectorFactory() {
        return new ReportingResultInspectorFactory(statsDClient);
    }
}
