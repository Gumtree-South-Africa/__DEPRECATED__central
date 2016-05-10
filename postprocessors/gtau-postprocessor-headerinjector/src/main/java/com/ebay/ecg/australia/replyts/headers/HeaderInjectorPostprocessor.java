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
    private static final Logger LOGGER = LoggerFactory.getLogger(HeaderInjectorPostprocessor.class);

    private HeaderInjectorPostprocessorConfig config;

    @Autowired
    public HeaderInjectorPostprocessor(@Value("${replyts.header-injector.headers}") String headers,
                                       @Value("${replyts.header-injector.order}") int order) {
        config = new HeaderInjectorPostprocessorConfig(headers, order);
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        LOGGER.info("Going to inject: " + config.getHeadersToInject());
        try {
            //TODO not sure if this must be custom headers or all headers
            Map<String,String> headers = context.getMail().getUniqueHeaders();
            LOGGER.info("Got headers: " + headers.keySet());
            for (String header : config.getHeadersToInject()) {
                // TODO headers are shown in CamelCase
                String headerValue = headers.get(header);
                if (headerValue != null) {
                    try {
                        LOGGER.info("Injecting " + header + ": " + headerValue);
                        context.getOutgoingMail().addHeader(header.toUpperCase(), headerValue);
                    } catch (Exception e) {
                        LOGGER.error("Couldn't add header to message (" + header + ": " + headerValue + ")", e);
                    }
                }
            }
        } catch (PersistenceException e) {
            LOGGER.error("Couldn't read conversation headers. Ignoring message", e);
        }
    }

    @Override
    public int getOrder() {
        return config.getOrder();
    }
}
