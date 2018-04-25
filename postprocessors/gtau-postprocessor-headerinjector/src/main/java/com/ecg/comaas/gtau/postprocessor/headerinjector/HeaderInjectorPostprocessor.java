package com.ecg.comaas.gtau.postprocessor.headerinjector;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Component
public class HeaderInjectorPostprocessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(HeaderInjectorPostprocessor.class);

    private final HeaderInjectorPostprocessorConfig config;

    @Autowired
    public HeaderInjectorPostprocessor(
            @Value("${replyts.header-injector.headers}") String headers,
            @Value("${replyts.header-injector.order}") int order
    ) {
        config = new HeaderInjectorPostprocessorConfig(headers, order);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        LOG.trace("Going to inject: {}", config.getHeadersToInject());

        if (context.getMail().isPresent()) {
            Map<String, String> headers = context.getMail().get().getUniqueHeaders();
            LOG.trace("Got header names: {}", headers.keySet());
            for (String header : config.getHeadersToInject()) {
                String headerValue = headers.get(header);
                if (!StringUtils.isEmpty(headerValue)) {
                    LOG.trace("Injecting {}: {}", header, headerValue);
                    context.getOutgoingMail().addHeader(header.toUpperCase(), headerValue);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }
}
