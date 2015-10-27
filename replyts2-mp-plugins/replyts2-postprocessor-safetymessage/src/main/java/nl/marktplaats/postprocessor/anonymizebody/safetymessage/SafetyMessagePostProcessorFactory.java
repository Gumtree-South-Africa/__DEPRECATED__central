package nl.marktplaats.postprocessor.anonymizebody.safetymessage;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by reweber on 22/10/15
 */
public class SafetyMessagePostProcessorFactory implements BasePluginFactory<PostProcessor> {

    private static final String SELLER_SAFETY_TEXTS = "sellerSafetyTexts";
    private static final String BUYER_SAFETY_TEXTS = "buyerSafetyTexts";

    @Override
    public PostProcessor createPlugin(String instanceName, JsonNode configuration) {
        List<String> sellerSafetyTexts = Lists.newArrayList(configuration.get(SELLER_SAFETY_TEXTS).elements()).stream()
                .map(JsonNode::asText).collect(Collectors.toList());

        List<String> buyerSafetyTexts = Lists.newArrayList(configuration.get(BUYER_SAFETY_TEXTS).elements()).stream()
                .map(JsonNode::asText).collect(Collectors.toList());

        SafetyMessagePostProcessorConfig safetyMessagePostProcessorConfig =
                new SafetyMessagePostProcessorConfig(sellerSafetyTexts, buyerSafetyTexts);
        return new SafetyMessagePostProcessor(safetyMessagePostProcessorConfig);
    }
}
