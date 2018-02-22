package com.ebay.ecg.australia.replyts.headers;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

/**
 * @author mdarapour
 */
public class HeaderInjectorPostprocessor implements PostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(HeaderInjectorPostprocessor.class);

    private HeaderInjectorPostprocessorConfig config;

    @Autowired
    public HeaderInjectorPostprocessor(@Value("${replyts.header-injector.headers}") String headers,
                                       @Value("${replyts.header-injector.order}") int order) {
        config = new HeaderInjectorPostprocessorConfig(headers, order);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        LOG.trace("Going to inject: {}", config.getHeadersToInject());
        try {
            Map<String,String> headers = context.getMail().getUniqueHeaders();
            LOG.trace("Got headers: {}", headers.keySet());
            for (String header : config.getHeadersToInject()) {
                String headerValue = headers.get(header);
                if (headerValue != null) {
                    try {
                        LOG.trace("Injecting {}: {}", header, headerValue);
                        context.getOutgoingMail().addHeader(header.toUpperCase(), headerValue);
                    } catch (Exception e) {
                        LOG.error("Couldn't add header to message ({}: {})", header, headerValue, e);
                    }
                }
            }
        } catch (PersistenceException e) {
            LOG.error("Couldn't read conversation headers. Ignoring message", e);
        }
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }
}
