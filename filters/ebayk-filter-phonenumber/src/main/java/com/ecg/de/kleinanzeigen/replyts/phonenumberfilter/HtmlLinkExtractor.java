package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts the telephone number in an HTML links into a NumberStream.
 * <p/>
 * See: http://wiki.selfhtml.org/wiki/HTML/Textauszeichnung/a#klickbare_Telefonnummern
 * See: https://tools.ietf.org/html/rfc3966
 */
public class HtmlLinkExtractor {

    public NumberStream extractStream(TypedContent<String> textPart) {
        Handler handler = new Handler();
        try {
            Parser parser = new Parser();
            parser.setFeature(Parser.namespacesFeature, false);
            parser.setFeature(Parser.namespacePrefixesFeature, false);
            parser.setContentHandler(handler);
            parser.parse(new InputSource(new StringReader(textPart.getContent())));
        } catch (Exception ignore) {
            // ignore, empty number stream
        }
        return new NumberStream(handler.getNumbers());
    }

    private static class Handler extends DefaultHandler {

        private static final CharMatcher NON_NUMERIC = CharMatcher.inRange('0', '9').negate();
        private static final String HTML_A = "a";
        private static final String HREF = "href";
        private static final String TEL_PREFIX = "tel:";

        private final Set<String> numbers = new HashSet<>();

        public List<String> getNumbers() {
            return ImmutableList.copyOf(numbers);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (HTML_A.equalsIgnoreCase(qName)) {
                Optional.ofNullable(atts.getValue(HREF))
                        .map(String::trim)
                        .filter(hrefValue -> hrefValue.startsWith(TEL_PREFIX))
                        .map(NON_NUMERIC::removeFrom)
                        .ifPresent(numbers::add);
            }
        }
    }
}
