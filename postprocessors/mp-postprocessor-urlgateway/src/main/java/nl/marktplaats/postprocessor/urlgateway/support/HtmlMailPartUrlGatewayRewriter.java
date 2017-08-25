package nl.marktplaats.postprocessor.urlgateway.support;

import net.htmlparser.jericho.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rewrites URLs to the gateway for HTML formatted mail parts.
 *
 * @author Erik van Oosten
 */
public class HtmlMailPartUrlGatewayRewriter implements UrlGatewayRewriter {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlMailPartUrlGatewayRewriter.class);

    /** {@inheritDoc} */
    @Override
    public String rewriteUrls(String content, GatewaySwitcher gatewaySwitcher) {
        LOG.trace("Inserting safety text in html part.");

        Source source = new Source(content);
        source.setLogger(null);
        OutputDocument target = new OutputDocument(source);

        source.fullSequentialParse();

        // Replace text in href attributes of anchors
        List<Element> links = source.getAllElements("a");
        for (Element anchor : links) {
            Attributes anchorAttributes = anchor.getAttributes();
            Map<String, String> attributeMap = asMap(anchorAttributes);

            String linkRawValue = attributeMap.get("href");
            if (linkRawValue != null && !linkRawValue.isEmpty()) {
                // Replace non empty link.
                String gatewayUrl;
                try {
                    gatewayUrl = gatewaySwitcher.rewrite(linkRawValue);

                } catch (IllegalArgumentException e) {
                    LOG.info("URL could not be parsed by java.net.URL: " + linkRawValue);
                    gatewayUrl = linkRawValue;
                }

                if (gatewayUrl != null) {
                    attributeMap.put("href", gatewayUrl);
                } else {
                    attributeMap.remove("href");
                }
                target.replace(anchorAttributes, attributeMap);
            }
        }

        // Replace links in text
        PlainTextMailPartUrlGatewayRewriter rewriter = new PlainTextMailPartUrlGatewayRewriter();
        List<Tag> allTags = source.getAllTags();
        if (allTags.isEmpty()) {
            // No tags, process all text
            processText(source, target, rewriter, gatewaySwitcher, 0, source.getEnd());

        } else {
            // Process up to first tag
            Tag firstTag = allTags.get(0);
            processText(source, target, rewriter, gatewaySwitcher, 0, firstTag.getBegin());

            boolean insideAnchor = false;
            for (Tag tag : allTags) {
                if ("a".equals(tag.getName())) {
                    insideAnchor = tag.getTagType() instanceof StartTagType;
                }

                if (!insideAnchor) {
                    Tag nextTag = tag.getNextTag();
                    int beforeNextTag = nextTag != null ? nextTag.getBegin() : source.getEnd();
                    processText(source, target, rewriter, gatewaySwitcher, tag.getEnd(), beforeNextTag);
                }
            }

            // Process after last tag
            Tag lastTag = allTags.get(allTags.size() - 1);
            processText(source, target, rewriter, gatewaySwitcher, lastTag.getEnd(), source.getEnd());
        }

        return target.toString();
    }

    private void processText(
            Source source,
            OutputDocument target,
            PlainTextMailPartUrlGatewayRewriter rewriter,
            GatewaySwitcher gatewaySwitcher,
            int begin, int end
    ) {
        String text = source.subSequence(begin, end).toString();
        String processed = rewriter.rewriteUrlsForHtml(text, gatewaySwitcher);
        target.replace(begin, end, processed);
    }

    private Map<String, String> asMap(Attributes anchorAttributes) {
        return anchorAttributes.populateMap(
                new LinkedHashMap<>(anchorAttributes.getCount() * 2, 1.0F), true);
    }

}
