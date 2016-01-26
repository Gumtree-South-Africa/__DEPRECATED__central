package com.ecg.replyts.core.runtime.mailparser;

import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class HtmlRemover {

    private enum TagType {
        Remove, Inline, Block
    }

    private static final char NBSP_CHAR = (char) 160;
    private static final char TAB_CHAR = '\t';

    private static final String LINE_BREAK_TAG = "br";
    private static final String PARAGRAPH_TAG = "p";

    private static final String[] TAGS_TO_STRIP = new String[]{"script", "object",
            "embed", "canvas", "video", "style"};

    private static final String[] INLINE_TAGS = new String[]{"a", "abbr", "acronym", "b", "basefont", "bdo",
            "big", "br", "cite", "code", "dfn", "em", "font", "i", "img", "input", "kbd", "label", "q",
            "s", "samp", "select", "small", "span", "strike", "strong", "sub", "sup", "textarea", "tt",
            "u", "var"};

    private static final Pattern TOO_MANY_BLANK_LINES_PATTERN = Pattern.compile("( *\r?\n *){3,}");
    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("( *\r?\n *)+");
    private static final Pattern MULTI_OCCURANCE_PATTERN = Pattern.compile("[ \t" + NBSP_CHAR + "]+");

    private StringBuilder buff = new StringBuilder(50000);
    private boolean lastClosedIsBlock = false;

    private final DefaultHandler htmlRemoveHandler = new HtmlRemovingHandler();

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
        String output = buff.toString().trim();

        output = MULTI_OCCURANCE_PATTERN.matcher(output).replaceAll(" ");
        output = TOO_MANY_BLANK_LINES_PATTERN.matcher(output).replaceAll("\n\n");
        return output.trim();
    }

    private class HtmlRemovingHandler extends DefaultHandler {

        private StringBuilder elementBuffer = new StringBuilder();

        private boolean record = true;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String lastChar = elementBuffer.length() > 0 ? elementBuffer.substring(elementBuffer.length() - 1) : null;
            flushBuffer();
            switch (getType(qName)) {
                case Remove:
                    record = false;
                    break;
                case Block:
                    if (!lastClosedIsBlock) {
                        buff.append("\n");
                        lastClosedIsBlock = false;
                    }
                    break;
                case Inline:
                    // Line Break is *inline* however it adds a line break. so this tag needs to be handeled specially

                    if (qName.equalsIgnoreCase(LINE_BREAK_TAG)) {
                        buff.append("\n");
                    } else if (" ".equals(lastChar)) {
                        buff.append(" ");
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
                    buff.append("\n");
                    if (qName.equalsIgnoreCase(PARAGRAPH_TAG)) {
                        buff.append("\n");
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
                String output = bufferContents.replace(NBSP_CHAR, ' ').replace(TAB_CHAR, ' ');
                output = MULTI_OCCURANCE_PATTERN.matcher(output).replaceAll(" ");
                output = NEW_LINE_PATTERN.matcher(output).replaceAll(" ");

                // Check for the case, that a Space has been put between two inline elements
                if (!lastClosedIsBlock && !output.startsWith(" ")) {
                    output = output.trim();
                }

                buff.append(output);
                elementBuffer = new StringBuilder(5000);
            }
        }

        private Map<String, TagType> specialTags = new HashMap<String, TagType>();

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
