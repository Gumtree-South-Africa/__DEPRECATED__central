package com.ecg.messagebox.util.messages;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Primary
@Component
@ConditionalOnExpression("#{'${tenant}' == 'gtuk'}")
public class GtukMessageResponseFactory implements MessagesResponseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GtukMessageResponseFactory.class);

    private static final int THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY = 10000;

    private static final List<Pattern> REPLACE_PATTERNS = Lists.newArrayList(
            Pattern.compile("URL: http://www\\..*gumtree.*", Pattern.MULTILINE),

            Pattern.compile("Date: [0-9\\-]+", Pattern.MULTILINE),
            Pattern.compile("Date: ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), [0-9]+ ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{2}:[0-9]{2}:[0-9]{2} \\+[0-9]{4}", Pattern.MULTILINE),
            Pattern.compile("Time: [0-9:]+", Pattern.MULTILINE),
            Pattern.compile("On [0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4}, [0-9]{1,2}:[0-9]{2} (am|pm|AM|PM),.*", Pattern.DOTALL),
            Pattern.compile("On [0-9]{1,2}/[0-9]{1,2}/[0-9]{2,4} [0-9]{1,2}:[0-9]{2},.*", Pattern.DOTALL),
            Pattern.compile("On [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{1,2}:[0-9]{2} (am|pm|AM|PM|),.*", Pattern.DOTALL),
            Pattern.compile("On [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} [0-9]{1,2}:[0-9]{2},.*", Pattern.DOTALL),
            Pattern.compile("On [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4} at [0-9]{2}:[0-9]{2},.*", Pattern.DOTALL),
            Pattern.compile("On ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), [0-9]{1,2} ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{4},.*", Pattern.DOTALL),
            Pattern.compile("On ((Monday|Mon)|(Tuesday|Tue)|(Wednesday|Wed)|(Thursday|Thu)|(Friday|Fri)|(Saturday|Sat)|(Sunday|Sun)), ((January|Jan)|(February|Feb)|(March|Mar)|(April|Apr)|May|(June|Jun)|(Jul|July)|(August|Aug)|(September|Sep)|(October|Oct)|(November|Nov)|(December|Dec)) [0-9]{1,2}, [0-9]{4}.*", Pattern.DOTALL),
            Pattern.compile("\\[mailto:.*?", Pattern.DOTALL),
            Pattern.compile("<mailto:.*@.*>", Pattern.MULTILINE),
            Pattern.compile("Sent:.*?$", Pattern.MULTILINE),
            Pattern.compile("Posted:.*?$", Pattern.MULTILINE),
            Pattern.compile("Of:.*?$", Pattern.MULTILINE),
            Pattern.compile("From:.*?$", Pattern.DOTALL),
            Pattern.compile("Subject:.*?$", Pattern.MULTILINE),
            Pattern.compile("To:.*?$", Pattern.MULTILINE),
            Pattern.compile("---* ?Original.*?-.*$", Pattern.MULTILINE),
            Pattern.compile("____________________________* ?.*$", Pattern.MULTILINE),
            Pattern.compile("^>.*$", Pattern.MULTILINE),
            Pattern.compile("[Tt]elephone( [Nn]o)*:.*"),
            Pattern.compile("[Ee]mail [Aa]ddress:.*"),

            Pattern.compile("[Ss]ent [Ff]rom [Oo]utlook.*", Pattern.DOTALL),
            Pattern.compile("[Ss]ent [Bb]y [Oo]utlook [Ff]or [Aa]ndroid"),
            Pattern.compile("[Ss]ent [Ff]rom [Yy]ahoo Mail.*", Pattern.DOTALL),
            Pattern.compile("[Ss]ent [Ff]rom ([Ii][Pp]hone|[Ii][Pp]ad|Windows Mail|Samsung ([Mm]obile|[Tt]ablet)|HTC)"), // no Pattern.CASE_INSENSITIVE switch as it is expensive
            Pattern.compile("[Ss]ent [Ff]rom my ([Ii][Pp]hone|[Ii][Pp]ad|Windows Mail|Samsung ([Mm]obile|[Tt]ablet)|HTC)") // no Pattern.CASE_INSENSITIVE switch as it is expensive
    );

    @Override
    public String getCleanedMessage(Conversation conv, Message message) {
        String text = message.getPlainTextBody();

        if (StringUtils.isBlank(text)) {
            // nothing to do, empty message happened (most likely message contained attachment only)
            return text;
        }

        // as we iterate below
        String tmp = text;
        long start, end, duration;
        Matcher matcher;

        tmp = stripOutInitialReplyGumtreeTemplate(tmp);

        for (Pattern pattern : REPLACE_PATTERNS) {
            start = System.currentTimeMillis();

            matcher = pattern.matcher(tmp);
            tmp = matcher.replaceAll("").trim();
            end = System.currentTimeMillis();
            duration = end - start;

            if (duration > 10) {
                LOG.warn("Long running regex, Length: " + tmp.length() +
                        " Time: " + duration + " Pattern: " + pattern.toString());
            }
        }

        tmp = tmp.trim();
        if (tmp.length() == 0) {
            return "[A message cannot be displayed here, it has been sent to you via email instead.]";
        }

        if (tmp.length() > THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) {
            return tmp.substring(0,THRESHOLD_CHAR_SIZE_TO_MAX_TO_DISPLAY) + "...\n\n[This message is too long to be " +
                    "displayed here, the full message has been sent to you via email.]";
        }
        return tmp;
    }

    private static String stripOutInitialReplyGumtreeTemplate(String input) {
        Pattern prefix = Pattern.compile("(Dear[^\\n]*,[\\n\\s=]*You have received a reply to your ad.*[=\\s\\n-]*\\nFrom: [^\\n]*\\n[\\s\\n=]*)", Pattern.DOTALL);
        Matcher prefixMatcher = prefix.matcher(input);
        String inputAfterStrippingOutFirstpart = prefixMatcher.replaceAll("");

        if (inputAfterStrippingOutFirstpart.equals(input)) {
            return input;
        }

        Pattern belowText = Pattern.compile("=?(\\n[=\\s\\n-]*This message has been sent through Gumtree mail,.+)", Pattern.DOTALL);
        String res = belowText.matcher(inputAfterStrippingOutFirstpart).replaceAll("");

        Pattern belowMessageTextTemplate = Pattern.compile("=?(\\n[=\\s\\n-]*Need to respond[?] Answer this message by pressing 'Reply'.+)", Pattern.DOTALL);
        return belowMessageTextTemplate.matcher(res).replaceAll("");
    }
}
