package com.ecg.messagebox.util.messages;

import com.ecg.messagecenter.core.cleanup.gtau.TextCleaner;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessagesResponseFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@Primary
@Component
@Profile(TENANT_GTAU)
public class GtauMessageResponseFactory implements MessagesResponseFactory {

    @Override
    public String getCleanedMessage(Conversation conv, Message message) {
        String plainTextBody = message.getPlainTextBody();
        if (StringUtils.isBlank(plainTextBody)) {
            // nothing to do, empty message happened (most likely message contained attachment only)
            return plainTextBody;
        }
        return TextCleaner.cleanupText(plainTextBody);
    }
}
