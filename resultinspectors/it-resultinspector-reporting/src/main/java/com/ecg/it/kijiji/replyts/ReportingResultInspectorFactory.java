package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportingResultInspectorFactory implements ResultInspectorFactory {
    @Autowired
    private StatsDClient statsDClient;

    @Override
    public ResultInspector createPlugin(String instanceName, JsonNode configuration) {
        return new ReportingResultInspector(statsDClient);
    }
}