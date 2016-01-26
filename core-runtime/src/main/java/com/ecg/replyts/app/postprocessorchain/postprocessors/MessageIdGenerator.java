package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.runtime.mailparser.MessageIdHeaderEncryption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * User: gdibella
 * Date: 9/3/13
 */
@Component
class MessageIdGenerator {

    private final String[] platformDomains;
    private final MessageIdHeaderEncryption messageIdHeaderEncryption;

    @Autowired
    MessageIdGenerator(@Value("${mailcloaking.domains}") String[] platformDomains) {
        this(platformDomains, new MessageIdHeaderEncryption());
    }

    MessageIdGenerator(
            String[] platformDomains,
            MessageIdHeaderEncryption messageIdHeaderEncryption) {
        this.platformDomains = platformDomains.clone();
        this.messageIdHeaderEncryption = messageIdHeaderEncryption;
    }

    String encryptedMessageId(String messageId) {
        return "<" + messageIdHeaderEncryption.encrypt(messageId) + "@" + platformDomains[0] + ">";
    }
}
