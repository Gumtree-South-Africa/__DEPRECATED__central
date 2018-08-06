package com.ecg.messagebox.util.messages;

import com.ecg.messagecenter.core.cleanup.kjca.TextCleaner;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.processing.MessagesResponseFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_KJCA;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_MVCA;

@Primary
@Component
@Profile({TENANT_KJCA, TENANT_MVCA})
public class KjcaMessageResponseFactory implements MessagesResponseFactory {

    @Override
    public String getCleanedMessage(Conversation conv, Message message) {
        String plainTextBody = message.getPlainTextBody();
        if (org.apache.commons.lang3.StringUtils.isBlank(plainTextBody)) {
            // nothing to do, empty message happened (most likely message contained attachment only)
            return plainTextBody;
        }
        return TextCleaner.cleanupText(plainTextBody);
    }
}
