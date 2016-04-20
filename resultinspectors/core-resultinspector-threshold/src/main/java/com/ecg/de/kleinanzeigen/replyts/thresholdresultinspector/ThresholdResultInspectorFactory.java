package com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector;

import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;

public class ThresholdResultInspectorFactory implements ResultInspectorFactory{


    @Override
    public ResultInspector createPlugin(String s, JsonNode jsonNode) {
        String held = jsonNode.get("held").asText();
        String blocked = jsonNode.get("blocked").asText();
        long heldThreshold = Long.valueOf(held);
        long blockedThreshold = Long.valueOf(blocked);

        return new ThresholdResultInspector(s, heldThreshold, blockedThreshold);
    }
}
