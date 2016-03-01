package com.ecg.de.ebayk.messagecenter.util;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushNotificationTextShortener {

    private static final int THREASHOLED_CHAR_SIZE_TO_MAX_TO_DISPLAY = 10000;

    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationTextShortener.class);

    // only patterns for push notifications (they are more optimistic to produce shorter texts. the real shortening is in the MessagePreProcessor.
    static final List<Pattern> REPLACE_PATTERNS = ImmutableList.of(
            Pattern.compile(" [\\S]+ über eBay Kleinanzeigen .*", Pattern.DOTALL),
            Pattern.compile("eBay Kleinanzeigen.*?Anfrage zu Ihrer.*?Ein Interessent.*?Anzeigennummer:.*?[0-9]+.*?", Pattern.DOTALL),
            Pattern.compile("^.*?Nachricht von:.*$", Pattern.MULTILINE),
            Pattern.compile("Artikel bereits verkauft.*?Anzeige löschen Anzeige deaktivieren", Pattern.DOTALL),
            Pattern.compile("Beantworten Sie diese Nachricht einfach .*?E-Mail-Programms", Pattern.DOTALL),
            Pattern.compile("\\s*Schützen Sie sich vor Betrug:.*", Pattern.DOTALL),
            Pattern.compile("\n\n\\s*\\."),
            Pattern.compile(">.*?\n*"),
            Pattern.compile("interessent-.*", Pattern.DOTALL),
            Pattern.compile("anbieter-.*", Pattern.DOTALL),
            Pattern.compile("^.*?----.*Urspr.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("^.*?----.*Original.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("^.*?----.*Reply.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("Sent:.*?$", Pattern.MULTILINE),
            Pattern.compile("Gesendet:.*?$", Pattern.MULTILINE),
            Pattern.compile("Von:.*?$", Pattern.MULTILINE),
            Pattern.compile("Subject:.*?$", Pattern.MULTILINE),
            Pattern.compile("Betreff:.*?$", Pattern.MULTILINE),
            Pattern.compile("Date:.*?$", Pattern.MULTILINE),
            Pattern.compile("Datum:.*?$", Pattern.MULTILINE),
            Pattern.compile("Am:.*?$", Pattern.MULTILINE),
            Pattern.compile("To:.*?$", Pattern.MULTILINE),
            Pattern.compile("^\\s*Am .*,.*?$", Pattern.MULTILINE),
            Pattern.compile("^\\s*On .*?$", Pattern.MULTILINE),
            Pattern.compile("From:.*?$", Pattern.MULTILINE),
            Pattern.compile("Angebot:\\s+[0-9\\.,]+\\s+(EUR|\\\\u20ac|€)\\s+Angebot annehmen\\s+Gegenangebot")
    );


    public static String shortenText(String text) {
        if (StringUtils.isBlank(text)) {
            // nothing to do, empty message happened (most likely message contained attachement only)
            return text;
        }

        // as we iterate below
        String tmp = text;


        for (Pattern pattern : REPLACE_PATTERNS) {

            long start = System.currentTimeMillis();

            Matcher matcher = pattern.matcher(tmp);
            tmp = matcher.replaceAll("").trim();

            long end = System.currentTimeMillis();
            long duration = end - start;

            if (duration > 800) {
                LOG.debug("Long running regex, Length: " + tmp.length() + " Time: " + duration + " Pattern: " + pattern.toString());
            }
        }

        return tmp.trim();
    }

}
