package com.ecg.messagebox.util.messages;

import com.ecg.messagecenter.cleanup.gtau.TextCleaner;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnExpression("#{'${replyts.tenant}' == 'gtau'}")
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
