package com.ecg.messagecenter.util;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mdarapour@ebay.com
 */
public class MessageTextHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageTextHandler.class);

    private static final int    THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY = 10000;

    static final Pattern XML_PATTERN = Pattern.compile("<(\\S+?)(.*?)>(.*?)</\\1>");

    static final List<Pattern> REPLACE_PATTERNS = Lists.newArrayList(
        Pattern.compile("Gumtree member since [0-9]+", Pattern.MULTILINE),
        Pattern.compile("Offered to pay with", Pattern.MULTILINE),
        Pattern.compile("Agreed on a price\\? When you meet in person, ask your buyer to pay with the PayPal app\\.", Pattern.MULTILINE),
        Pattern.compile("Already sold it\\?", Pattern.MULTILINE),
        Pattern.compile("Stop getting emails about this ad\\.", Pattern.MULTILINE),
        Pattern.compile("Manage this ad", Pattern.MULTILINE),
        Pattern.compile("Regards, The Gumtree Team", Pattern.MULTILINE),
        Pattern.compile("Please report any suspicious email\\. Never send or wire money without meeting in person.*?eBay International AG\\.", Pattern.DOTALL),
        Pattern.compile("You have received a lead from Gumtree Australia regarding a.*", Pattern.MULTILINE),
        Pattern.compile("URL: http://www\\..*gumtree.*", Pattern.MULTILINE),
        Pattern.compile("Stock#: [a-zA-Z0-9/]+", Pattern.MULTILINE),
        Pattern.compile("Year: [0-9]+", Pattern.MULTILINE),
        Pattern.compile("Make: .*", Pattern.MULTILINE),
        Pattern.compile("Model: .*", Pattern.MULTILINE),
        Pattern.compile("Price: [0-9.]+", Pattern.MULTILINE),
        Pattern.compile("Email: .*", Pattern.MULTILINE),
        Pattern.compile("Date: [0-9\\-]+", Pattern.MULTILINE),
        Pattern.compile("Date: ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), [0-9]+ ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|July|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} \\+[0-9]{4}", Pattern.MULTILINE),
        Pattern.compile("Time: [0-9:]+", Pattern.MULTILINE),
        Pattern.compile("[0-9]{4}(-|/)[0-9]{2}(-|/)([0-9]{2}|[0-9]{4}) [0-9]{2}:[0-9]{2} [GMT\\+[0-9]{2}:[0-9]{2}]*.*via Gumtree <(Buyer|Seller)\\..*@users\\.gumtree\\.com\\.au wrote:.*", Pattern.DOTALL),
        Pattern.compile("On [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|July|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{2}:[0-9]{2}.*via Gumtree <[\n]*(Buyer|Seller)\\..*@users\\.gumtree\\.com\\.au> wrote:.*", Pattern.DOTALL),
        Pattern.compile("On ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|July|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{1,2}, [0-9]{4} at [0-9]{2}:[0-9]{2}.*via Gumtree <[\n]*(Buyer|Seller)\\..*@users\\.gumtree\\.com\\.au> wrote:.*", Pattern.DOTALL),
        Pattern.compile("On ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|July|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4},.*via Gumtree <[\n]*(Buyer|Seller)\\..*@users\\.gumtree\\.com\\.au> wrote:.*", Pattern.DOTALL),
        Pattern.compile("On [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|July|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{2}:[0-9]{2}.*via Gumtree <.*(Buyer|Seller)\\..*@users\\.gumtree\\.com\\.au> wrote:.*", Pattern.DOTALL),
        Pattern.compile("On\\s[0-9]{2}/[0-9]{2}/[0-9]{2}\\s[0-9]{2}:[0-9]{2}.*via Gumtree wrote:.*", Pattern.DOTALL),
        Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}\\s[0-9]{2}:[0-9]{2}.*via Gumtree <.*", Pattern.DOTALL),
        Pattern.compile("Gumtree Australia", Pattern.MULTILINE),
        Pattern.compile("<https://.*\\..*gumtree.*com.*au.*", Pattern.MULTILINE),
        Pattern.compile("\\[mailto:.*?", Pattern.DOTALL),
        Pattern.compile("<mailto:.*@.*>", Pattern.MULTILINE),
        Pattern.compile("Sent:.*?$", Pattern.MULTILINE),
        Pattern.compile("Posted:.*?$", Pattern.MULTILINE),
        Pattern.compile("Of:.*?$", Pattern.MULTILINE),
        Pattern.compile("From:.*?$", Pattern.DOTALL),
        Pattern.compile("Subject:.*?$", Pattern.MULTILINE),
        Pattern.compile("To:.*?$", Pattern.MULTILINE),
        Pattern.compile("^\\s*On .*?$", Pattern.MULTILINE),
        Pattern.compile("---* ?Original.*?-.*$", Pattern.MULTILINE),
        Pattern.compile("---* ?Reply.*?-.*$", Pattern.MULTILINE),
        Pattern.compile("^>.*$", Pattern.MULTILINE),
        Pattern.compile("[_]+", Pattern.MULTILINE),
        Pattern.compile("[-]{3,}", Pattern.MULTILINE),
        Pattern.compile("^\n+", Pattern.MULTILINE),
        Pattern.compile("[Ss]ent [Ff]rom ([Ii][Pp]hone|[Ii][Pp]ad|Windows Mail|Samsung ([Mm]obile|[Tt]ablet)|HTC)"), // no Pattern.CASE_INSENSITIVE switch as it is expensive
        Pattern.compile("[Ss]ent [Ff]rom my ([Ii][Pp]hone|[Ii][Pp]ad|Windows Mail|Samsung ([Mm]obile|[Tt]ablet)|HTC)") // no Pattern.CASE_INSENSITIVE switch as it is expensive
    );

    public static boolean isXml(String text) {
        return Strings.isNullOrEmpty(text) ? false : XML_PATTERN.matcher(text).find();
    }

    public static String remove(String text) {
        if (StringUtils.isBlank(text)) {
            // nothing to do, empty message happened (most likely message contained attachment only)
            return text;
        }

        // as we iterate below
        String tmp = text;
        long start, end, duration;
        Matcher matcher;

        tmp = removePatternsWithSenderName(tmp);

        for (Pattern pattern : REPLACE_PATTERNS) {

            start = System.currentTimeMillis();

            matcher = pattern.matcher(tmp);
            tmp = matcher.replaceAll("").trim();

            end = System.currentTimeMillis();
            duration = end - start;

            if (duration > 10) {
                LOG.warn("Long running regex, Length: " + tmp.length() + " Time: " + duration + " Pattern: " + pattern.toString());
            }
        }

        tmp = tmp.trim();

        if (tmp.length() == 0) {
            return "Gumtree Team: Problem in displaying the message. " +
                    "Sorry for inconvenient, please read the message in your email program.";
        }
        if(tmp.length() > THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) {
            return tmp.substring(0,THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) + "...\n\nGumtree Team: Problem in displaying the message." +
                    " Sorry for inconvenient, please read the message in your email program.";
        }

        return tmp;
    }

    private static String removePatternsWithSenderName(String text) {
        Matcher matcher = Pattern.compile("^(.*?) replied to your ad:", Pattern.MULTILINE).matcher(text);
        if(matcher.find()) {
            String tmp = text;
            String sender = matcher.group(1);
            tmp = Pattern.compile(String.format("^%s replied to your ad:.*?Respond to[\\w\\s]+by replying directly to this email\\s+", sender), Pattern.DOTALL).matcher(tmp).replaceAll("");
            tmp = Pattern.compile(String.format("^%s\\n[\\s]*(Gumtree member since [0-9]+|Offered to pay with|Agreed on a price\\? When you meet in person, ask your buyer to pay with the PayPal app\\.|Already sold it\\?)",sender), Pattern.MULTILINE).matcher(tmp).replaceAll("");
            return tmp;
        }
        return text;
    }
}
