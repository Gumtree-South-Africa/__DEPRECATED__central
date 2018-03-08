package nl.marktplaats.postprocessor.urlgateway;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.app.postprocessorchain.ContentOverridingPostProcessor;
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

// TODO akobiakov: this thing should actually work with post message api messages as well.
public class UrlGatewayPostProcessor implements EmailPostProcessor, ContentOverridingPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(UrlGatewayPostProcessor.class);

    private static final PlainTextMailPartUrlGatewayRewriter PLAIN_TEXT_REWRITER = new PlainTextMailPartUrlGatewayRewriter();
    private static final HtmlMailPartUrlGatewayRewriter HTML_REWRITER = new HtmlMailPartUrlGatewayRewriter();

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
                        ? HTML_REWRITER
                        : PLAIN_TEXT_REWRITER;

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

    @Override
    public String overrideContent(String content) {
        return PLAIN_TEXT_REWRITER.rewriteUrls(content, gatewaySwitcher);
    }
}