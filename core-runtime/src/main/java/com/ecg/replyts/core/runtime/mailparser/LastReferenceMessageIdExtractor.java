package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class LastReferenceMessageIdExtractor {
    private static final Pattern REFERENCES_HEADER_PATTERN = Pattern.compile("(?:<([a-z0-9]+)@[^>]+> ?)+");
    private static final Logger LOG = LoggerFactory.getLogger(LastReferenceMessageIdExtractor.class);

    public Optional<String> get(Mail m) {
        String referencesHeader = m.getUniqueHeader(Mail.IN_REPLY_TO_HEADER);
        if (referencesHeader == null) {
            // Fall back to REFERENCES header.
            referencesHeader = m.getUniqueHeader(Mail.REFERENCES_HEADER);
        }
        if (referencesHeader != null) {
            try {
                Matcher matcher = REFERENCES_HEADER_PATTERN.matcher(referencesHeader);
                if (matcher.matches()) {
                    // According to http://cr.yp.to/immhf/thread.html, the last reference is the
                    // the 'parent', the email we're responding to.
                    String encryptedMessageId = matcher.group(matcher.groupCount());
                    String decryptedMessageId = new MessageIdHeaderEncryption().decrypt(encryptedMessageId);
                    return Optional.of(decryptedMessageId);
                }
            } catch (Exception ex) {
                LOG.info("Could not decrypt References/In-Reply-To header " + referencesHeader +
                        " for mail " + m.getMessageId());
            }
        }
        return Optional.empty();
    }
}
