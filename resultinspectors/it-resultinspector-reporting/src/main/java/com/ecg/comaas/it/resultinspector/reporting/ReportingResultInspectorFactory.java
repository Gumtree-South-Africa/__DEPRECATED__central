package com.ecg.comaas.it.resultinspector.reporting;

import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.timgroup.statsd.StatsDClient;
import org.springframework.stereotype.Component;

@Component
public class ReportingResultInspectorFactory implements ResultInspectorFactory {

    public static final String IDENTIFIER = "com.ecg.it.kijiji.replyts.ReportingResultInspectorFactory";

    private StatsDClient statsDClient;

    @Override
    public ResultInspector createPlugin(String instanceName, JsonNode configuration) {
        return new ReportingResultInspector(statsDClient);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}