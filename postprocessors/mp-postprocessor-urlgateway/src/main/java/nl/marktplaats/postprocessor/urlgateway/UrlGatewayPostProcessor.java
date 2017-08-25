package nl.marktplaats.postprocessor.urlgateway;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.MediaTypeHelper;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.net.MediaType;
import nl.marktplaats.postprocessor.urlgateway.support.GatewaySwitcher;
import nl.marktplaats.postprocessor.urlgateway.support.HtmlMailPartUrlGatewayRewriter;
import nl.marktplaats.postprocessor.urlgateway.support.PlainTextMailPartUrlGatewayRewriter;
import nl.marktplaats.postprocessor.urlgateway.support.UrlGatewayRewriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class UrlGatewayPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(UrlGatewayPostProcessor.class);

    private GatewaySwitcher gatewaySwitcher;

    @Autowired
    public UrlGatewayPostProcessor(UrlGatewayPostProcessorConfig urlGatewayPostProcessorConfig) {
        gatewaySwitcher = new GatewaySwitcher(urlGatewayPostProcessorConfig);
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();

        MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

        LOG.trace("UrlGatewayPostProcessor for message #{}", message.getId());

        List<TypedContent<String>> typedContents = outboundMail.getTextParts(false);
        if (typedContents.isEmpty()) {
            LOG.warn("Message {} has no recognized text parts", message.getId());
        }

        // Find mutable parts and change the URLs
        for (TypedContent<String> typedContent : typedContents) {
            if (typedContent.isMutable()) {
                MediaType mediaType = typedContent.getMediaType();

                UrlGatewayRewriter urlGatewayRewriter = MediaTypeHelper.isHtmlCompatibleType(mediaType)
                        ? new HtmlMailPartUrlGatewayRewriter()
                        : new PlainTextMailPartUrlGatewayRewriter();

                String existingContent = typedContent.getContent();
                String newContent = urlGatewayRewriter.rewriteUrls(existingContent, gatewaySwitcher);

                typedContent.overrideContent(newContent);
            }
        }
    }

    @Override
    public int getOrder() {
        return 300;
    }
}