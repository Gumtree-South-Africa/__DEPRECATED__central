package nl.marktplaats.postprocessor.anonymizebody;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnonymizeEmailPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(AnonymizeEmailPostProcessor.class);
    private final String[] platformDomains;

    private List<Pattern> patterns;

    @Autowired
    public AnonymizeEmailPostProcessor(@Value("${mailcloaking.domains}") String[] platformDomains,
                                       AnonymizeEmailPostProcessorConfig anonymizeEmailPostProcessorConfig) {
        if (platformDomains.length != 1)
            throw new IllegalArgumentException("AnonymizeEmailPostProcessor currently does not accept multiple domains");

        this.platformDomains = platformDomains;

        List<String> stringPatterns = anonymizeEmailPostProcessorConfig.getPatterns();
        patterns = new ArrayList<>(stringPatterns.size());

        // The patterns are enhanced to do better whitespace detection. Any whitespace (including newlines)
        // and HTML whitespace is detected.
        for (String stringPattern : stringPatterns ) {
            String enhancedPattern = stringPattern.replace(" *", "(?:\\s|&nbsp;)*");
            patterns.add(Pattern.compile(enhancedPattern, Pattern.CASE_INSENSITIVE));
        }
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        Mail mail = context.getOutgoingMail();
        Message message = context.getMessage();
        Conversation conversation = context.getConversation();

        try {
            // get anonymized and unanonymized sender email addresses
            String unanonymizedSender = getConversationSenderEmail(message, conversation);
            String anonymizedSender = mail.getFrom();
            String platformDomain = "@" + platformDomains[0];

            LOG.trace("Replacing unanonymized '{} with '{}'.", unanonymizedSender, anonymizedSender);

            // loop through all text parts and replace all occurrences
            for (TypedContent<String> textPart : mail.getTextParts(false)) {
                if (textPart.isMutable()) {
                    doReplacement(textPart, unanonymizedSender, anonymizedSender, patterns, platformDomain);
                }
            }

            // Implementation NOTE: it would be better to to keep (replace 'platformDomain' with) the anonymous address
            // of the receiver.

        } catch (PersistenceException pe) {
            LOG.error("Could not retrieve conversation for message with id {}", message.getId(), pe);

        } catch (StackOverflowError e) {
            LOG.error("Could not process message with id {}", message.getId(), e);
            throw e;
        }
    }

    private String getConversationSenderEmail(Message message, Conversation conv) {
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        return conv.getUserId(fromRole);
    }

    private void doReplacement(TypedContent<String> textPart, String toReplace, String replacement, List<Pattern> patterns, String toKeep) {
        boolean isHtml = MediaTypeHelper.isHtmlCompatibleType(textPart.getMediaType());
        String originalContent = textPart.getContent();

        String newContent = originalContent;
        if (isHtml) {
            // Un-escape some characters that are used in the patterns and that have been seen escaped.
            newContent = newContent.
                    replace("&#58;", ":").replace("&#x3a;", ":").
                    replace("&#64;", "@").replace("&#x40;", "@");
        }

        boolean contentChanged = false;
        StringBuilder sb = new StringBuilder(newContent.length());
        for (String line : lines(newContent, isHtml)) {
            if (line.contains("@")) {
                for (Pattern pattern : patterns) {
                    try {
                        Matcher matcher = pattern.matcher(line);
                        while (matcher.find()) {
                            String element = matcher.group();
                            if (element.contains(toReplace)) {
                                // Replace the address
                                String replacedElement = element.replace(toReplace, replacement);
                                line = line.replace(element, replacedElement);
                                contentChanged = true;

                            } else if (element.contains(replacement)) {
                                // Replacement was already done, skip this line.

                            } else if (element.contains(toKeep)) {
                                // Already anonymized, skip this line.

                            } else {
                                // No replacement could be made,
                                // and the replacement was not already done
                                // ===> the found e-mail address is not from this conversation.
                                // Action: strip the entire match
                                line = line.replace(element, "");
                                contentChanged = true;
                            }
                        }
                    } catch (Throwable t) {
                        LOG.error("Failed to replace '{}', '{}'", pattern, line);
                    }
                }
            }
            sb.append(line);
        }

        if (!contentChanged) return;

        textPart.overrideContent(sb.toString());
    }

    private Iterable<String> lines(final String text, final boolean isHtml) {
        return new Iterable<String>() {
            private final Pattern newLinePattern = Pattern.compile("(\\r|\\n|<[Bb][Rr]\\s*/?>)");
            private final Matcher m = newLinePattern.matcher(text);

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    private int textIndex = 0;
                    private String nextLine;
                    private boolean advanceNextCall = true;

                    @Override
                    public boolean hasNext() {
                        if (advanceNextCall) findNextLine();
                        advanceNextCall = false;
                        return nextLine != null;
                    }

                    @Override
                    public String next() {
                        if (advanceNextCall) findNextLine();
                        advanceNextCall = true;
                        if (nextLine == null) throw new NoSuchElementException();
                        return nextLine;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                    private void findNextLine() {
                        if (isHtml) {
                            findHtmlNextLine();
                        } else {
                            findPlainTextNextLine();
                        }
                    }

                    private void findHtmlNextLine() {
                        if (textIndex == text.length()) {
                            nextLine = null;
                        } else {
                            // Some HTMLs do linebreaks on ±76 characters. The result is that even the most
                            // simple patters can not be detected as they are longer. Here we combine lines so
                            // that there is a higher chance we match those patterns.
                            //
                            // We noticed that HTML lines that end in tag (and therefore end on a '>'), usually
                            // forms the end of a HTML construct. Patterns are not likely to span such a tag.
                            //
                            // Furthermore, as we only care about patterns at the beginning of such a HTML construct,
                            // we only take the first 140 characters.
                            //
                            int nextTextIndex = textIndex;
                            do {
                                if (m.find(nextTextIndex)) {
                                    nextTextIndex = m.end();
                                } else {
                                    nextTextIndex = text.length();
                                }
                            } while (
                                    nextTextIndex < text.length() &&
                                            ((nextTextIndex - textIndex) < 140 || htmlConstructContinuesOnNextLine(nextTextIndex))
                                    );
                            nextLine = text.substring(textIndex, nextTextIndex);
                            textIndex = nextTextIndex;
                        }
                    }

                    private void findPlainTextNextLine() {
                        if (textIndex == text.length()) {
                            nextLine = null;
                        } else if (m.find(textIndex)) {
                            int nextTextIndex = m.end();

                            // Merge with next line if it contains a '@'
                            if (m.find(nextTextIndex)) {
                                String nextNextLine = text.substring(nextTextIndex, m.end());
                                if (nextNextLine.indexOf('@') != -1) nextTextIndex = m.end();
                            }

                            nextLine = text.substring(textIndex, nextTextIndex);
                            textIndex = nextTextIndex;
                        } else {
                            nextLine = text.substring(textIndex, text.length());
                            textIndex = text.length();
                        }
                    }

                    private boolean htmlConstructContinuesOnNextLine(int endOfLineIndex) {
                        String line = text.substring(0, endOfLineIndex);
                        return !(line.endsWith(">") || line.endsWith(">\r\n") || line.endsWith(">\n"));
                    }
                };
            }
        };
    }

    @Override
    public int getOrder() {
        // WARNING: order value needs to be higher then that of Anonymizer.getOrder().
        return 300;
    }
}
