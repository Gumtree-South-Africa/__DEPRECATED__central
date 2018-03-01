package com.ecg.replyts.core.runtime.mailparser;

import org.ccil.cowan.tagsoup.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlRemover {
    private static final Logger LOG = LoggerFactory.getLogger(HtmlRemover.class);

    public static volatile boolean IS_SPAN_FIX_ENABLED = false;
    private static Pattern SPAN_PATTERN = Pattern.compile("<span rowtxt=\"rowmessage\">(.*?)</span>", Pattern.DOTALL);

    private enum TagType {
        Remove, Inline, Block
    }

    private static final char NBSP_CHAR = (char) 160;
    private static final char TAB_CHAR = '\t';

    private static final String LINE_BREAK_TAG = "br";
    private static final String SPAN_TAG = "span";
    private static final String PARAGRAPH_TAG = "p";

    private static final String SPAN_FIX_ATTRIBUTE_NAME = "rowtxt";

    private static final String[] TAGS_TO_STRIP = new String[] {
      "script", "object",
      "embed", "canvas", "video", "style"};

    private static final String[] INLINE_TAGS = new String[] {
      "a", "abbr", "acronym", "b", "basefont", "bdo",
      "big", "br", "cite", "code", "dfn", "em", "font", "i", "img", "input", "kbd", "label", "q",
      "s", "samp", "select", "small", "span", "strike", "strong", "sub", "sup", "textarea", "tt",
      "u", "var" };

    private static final Pattern TOO_MANY_BLANK_LINES_PATTERN = Pattern.compile("( *\r?\n *){3,}");
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("( *\r?\n *)+");
    private static final Pattern MULTI_OCCURANCE_PATTERN = Pattern.compile("[ \t" + NBSP_CHAR + "]+");

    private final DefaultHandler htmlRemoveHandler = new HtmlRemovingHandler();

    private boolean lastClosedIsBlock = false;

    private boolean fixableSpanIsFound = false;

    private StringBuilder buffer = new StringBuilder(50000);


    public HtmlRemover(String s) throws HtmlCleanupException {
        this(new StringReader(s));
    }

    public HtmlRemover(Reader rdr) throws HtmlCleanupException {
        this(new InputSource(rdr));
    }

    public HtmlRemover(InputSource is) throws HtmlCleanupException {
        Parser reader = new Parser();

        try {
            reader.setFeature(Parser.namespacesFeature, false);
            reader.setFeature(Parser.namespacePrefixesFeature, false);

            reader.setContentHandler(htmlRemoveHandler);

            reader.parse(is);
        } catch (Exception ex) {
            throw new HtmlCleanupException(ex);
        }
    }

    public String getPlaintext() {
        String output = buffer.toString().trim();

        output = MULTI_OCCURANCE_PATTERN.matcher(output).replaceAll(" ");
        output = TOO_MANY_BLANK_LINES_PATTERN.matcher(output).replaceAll("\n\n");

        if (IS_SPAN_FIX_ENABLED) {
            Matcher matcher = SPAN_PATTERN.matcher(output);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }

        return output.trim();
    }

    private class HtmlRemovingHandler extends DefaultHandler {
        private StringBuilder elementBuffer = new StringBuilder();

        private boolean record = true;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String lastChar = elementBuffer.length() > 0 ? elementBuffer.substring(elementBuffer.length() - 1) : null;

            flushBuffer();

            fixableSpanIsFound = false;

            switch (getType(qName)) {
                case Remove:
                    record = false;

                    break;
                case Block:
                    if (!lastClosedIsBlock) {
                        buffer.append("\n");

                        lastClosedIsBlock = false;
                    }

                    break;
                case Inline:
                    // Line Break is *inline* however it adds a line break. so this tag needs to be handled specially

                    if (qName.equalsIgnoreCase(LINE_BREAK_TAG)) {
                        buffer.append("\n");
                    } else if (" ".equals(lastChar)) {
                        buffer.append(" ");
                    }

                    if (IS_SPAN_FIX_ENABLED
                        && qName.equalsIgnoreCase(SPAN_TAG)
                        && attributes != null
                        && attributes.getLength() > 0
                        && attributes.getQName(0).equalsIgnoreCase(SPAN_FIX_ATTRIBUTE_NAME)) {
                        LOG.debug("Found the span start tag; storing it in the plain text");

                        buffer
                          .append("<span ")
                          .append(attributes.getQName(0))
                          .append("=\"")
                          .append(attributes.getValue(0))
                          .append("\">");

                        fixableSpanIsFound = true;
                    }

                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            flushBuffer();

            lastClosedIsBlock = false;

            switch (getType(qName)) {
                case Remove:
                    record = true;

                    break;
                case Block:
                    lastClosedIsBlock = true;

                    buffer.append("\n");

                    if (qName.equalsIgnoreCase(PARAGRAPH_TAG)) {
                        buffer.append("\n");
                    }

                    break;
                case Inline:
                    if (IS_SPAN_FIX_ENABLED && qName.equalsIgnoreCase(SPAN_TAG) && fixableSpanIsFound) {
                        LOG.debug("Found the span end tag; storing it in the plain text");

                        buffer.append("</span>");
                    }

                    break;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (record) {
                elementBuffer.append(ch, start, length);
            }
        }

        private void flushBuffer() {
            if (elementBuffer.length() > 0) {
                final String bufferContents = elementBuffer.toString();

                String output = bufferContents
                  .replace(NBSP_CHAR, ' ')
                  .replace(TAB_CHAR, ' ');

                output = MULTI_OCCURANCE_PATTERN.matcher(output).replaceAll(" ");

                // Bolt uses the <span> tag for storing the message of the user and since it needs multilines
                // to be kept, we remove the newlines only if not bolt. Ref: BOLT-36519
                if (!IS_SPAN_FIX_ENABLED) {
                    output = NEW_LINE_PATTERN.matcher(output).replaceAll(" ");
                }

                // Check for the case where a space has been put between two inline elements
                if (!lastClosedIsBlock && !output.startsWith(" ")) {
                    output = output.trim();
                }

                buffer.append(output);

                elementBuffer = new StringBuilder(5000);
            }
        }

        private Map<String, TagType> specialTags = new HashMap<>();

        {
            for (String t : TAGS_TO_STRIP) {
                specialTags.put(t.toLowerCase(), TagType.Remove);
            }
            for (String t : INLINE_TAGS) {
                specialTags.put(t.toLowerCase(), TagType.Inline);
            }
        }

        private TagType getType(final String qName) {
            final TagType special = specialTags.get(qName.toLowerCase());

            return special == null ? TagType.Block : special;
        }
    }
}
