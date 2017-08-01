package com.ecg.kijijiit.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.timgroup.statsd.StatsDClient;

public class ReportingResultInspectorFactory implements ResultInspectorFactory {
    private StatsDClient statsDClient;

    public ReportingResultInspectorFactory(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    @Override
    public ResultInspector createPlugin(String instanceName, JsonNode configuration) {
        return new ReportingResultInspector(statsDClient);
    }
}