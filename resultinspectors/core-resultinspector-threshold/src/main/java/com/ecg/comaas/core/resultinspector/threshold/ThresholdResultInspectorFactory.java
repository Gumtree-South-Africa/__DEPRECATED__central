package com.ecg.comaas.core.resultinspector.threshold;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspector;
import com.ecg.replyts.core.api.pluginconfiguration.resultinspector.ResultInspectorFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.*;

@ComaasPlugin
@Profile({TENANT_GTUK, TENANT_GTAU, TENANT_MP, TENANT_EBAYK, TENANT_KJCA, TENANT_IT, TENANT_MX, TENANT_AR, TENANT_ZA, TENANT_SG, TENANT_MVCA, TENANT_BE})
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
