package com.ecg.comaas.core.resultinspector.threshold;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

@ComaasPlugin
@Component
public class ThresholdResultInspectorFactory implements ResultInspectorFactory {

    public static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.thresholdresultinspector.ThresholdResultInspectorFactory";

    @Override
    public ResultInspector createPlugin(String s, JsonNode jsonNode) {
        String held = jsonNode.get("held").asText();
        String blocked = jsonNode.get("blocked").asText();
        long heldThreshold = Long.valueOf(held);
        long blockedThreshold = Long.valueOf(blocked);

        return new ThresholdResultInspector(s, heldThreshold, blockedThreshold);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
