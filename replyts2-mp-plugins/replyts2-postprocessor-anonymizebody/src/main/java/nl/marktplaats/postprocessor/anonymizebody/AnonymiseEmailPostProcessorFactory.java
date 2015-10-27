package nl.marktplaats.postprocessor.anonymizebody;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.pluginconfiguration.BasePluginFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by reweber on 21/10/15
 */
public class AnonymiseEmailPostProcessorFactory implements BasePluginFactory<PostProcessor> {

    //TODO check if the key matches the key in the config
    private static final String ANONYMIZE_MAIL_PATTERNS_KEY = "anonymizeMailPatterns";

    private String[] platformDomains;

    public AnonymiseEmailPostProcessorFactory(String[] platformDomains) {
        this.platformDomains = platformDomains;
    }

    @Override
    public PostProcessor createPlugin(String instanceName, JsonNode configuration) {
        List<String> patterns = Lists.newArrayList(configuration.get(ANONYMIZE_MAIL_PATTERNS_KEY).elements()).stream()
                .map(JsonNode::asText).collect(Collectors.toList());
        AnonymiseEmailPostProcessorConfig anonymiseEmailPostProcessorConfig = new AnonymiseEmailPostProcessorConfig(patterns);
        return new AnonymizeEmailPostProcessor(platformDomains, anonymiseEmailPostProcessorConfig);
    }
}
