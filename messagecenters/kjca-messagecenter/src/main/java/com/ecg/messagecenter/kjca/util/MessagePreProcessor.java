package com.ecg.messagecenter.kjca.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagePreProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(MessagePreProcessor.class);

    enum LinebreakAdderRules {

        RULE_1(Pattern.compile("(@users.gumtree.com.au)([^\n\r>:])"),"$1\n$2"),
        RULE_2(Pattern.compile("[:]\t"),":\n"),
        RULE_3(Pattern.compile("[:]   "),":\n"),
        RULE_4(Pattern.compile("    "),"\n"),
        // RULE_5 for weird webmailer client which is prepending the Subject to the text
        RULE_5(Pattern.compile("Gumtree Australia([\\S])"),"$1");

        LinebreakAdderRules(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }

        private Pattern pattern;
        private String replacement;

        public Pattern getPattern() {
            return pattern;
        }

        public String getReplacement() {
            return replacement;
        }
    }
}
