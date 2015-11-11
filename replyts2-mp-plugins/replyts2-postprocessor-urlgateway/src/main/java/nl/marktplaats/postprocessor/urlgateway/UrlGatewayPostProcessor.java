package nl.marktplaats.postprocessor.urlgateway;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
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
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class UrlGatewayPostProcessor implements PostProcessor {

    public static final String FROM = "From";
    private static final Logger LOG = LoggerFactory.getLogger(UrlGatewayPostProcessor.class);
    private final String[] platformDomains;

    private GatewaySwitcher gatewaySwitcher;


    @Autowired
    public UrlGatewayPostProcessor(@Value("${mailcloaking.domains}") String[] platformDomains,
                                   UrlGatewayPostProcessorConfig urlGatewayPostProcessorConfig) {

        this.platformDomains = platformDomains;


        gatewaySwitcher = new GatewaySwitcher(urlGatewayPostProcessorConfig);


    }


    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public void postProcess(MessageProcessingContext messageProcessingContext) {
        Message message = messageProcessingContext.getMessage();
        MessageDirection messageDirection = message.getMessageDirection();

        MutableMail outboundMail = messageProcessingContext.getOutgoingMail();

        LOG.debug("UrlGatewayPostProcessor for message #" + message.getId());

        List<TypedContent<String>> typedContents = outboundMail.getTextParts(false);
        if (typedContents.isEmpty()) {
            LOG.warn("Message {} has no recognized text parts", message.getId());
        }

        // Find mutable parts and change the URLs
        for (TypedContent<String> typedContent : typedContents) {
            if (typedContent.isMutable()) {
                MediaType mediaType = outboundMail.getMainContentType();

                UrlGatewayRewriter urlGatewayRewriter = MediaTypeHelper.isHtmlCompatibleType(mediaType)
                        ? new HtmlMailPartUrlGatewayRewriter()
                        : new PlainTextMailPartUrlGatewayRewriter();

                String existingContent = typedContent.getContent();
                String newContent = urlGatewayRewriter.rewriteUrls(existingContent, gatewaySwitcher);

                // LOG.debug("New text: " + newContent);
                typedContent.overrideContent(newContent);
            }
        }
    }
}