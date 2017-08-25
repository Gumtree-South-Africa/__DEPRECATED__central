package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReportingResultInspector implements ResultInspector {
    private static final Logger LOG = LoggerFactory.getLogger(ReportingResultInspector.class);

    private StatsDClient statsDClient;

    public ReportingResultInspector(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public void inspect(List<ProcessingFeedback> feedback) {
        if (statsDClient == null) {
            LOG.warn("StatsD client is not able to connect to daemon");

            return;
        }

        feedback.stream()
                .filter(f -> f.getDescription() != null)
                .filter(f -> f.getDescription().toLowerCase().contains("matched word"))
                .map(f -> f.getUiHint())
                .peek(uh -> LOG.trace("Sending feedback:{} ", uh))
                .forEach(uh -> statsDClient.incrementCounter(uh));
    }
}
