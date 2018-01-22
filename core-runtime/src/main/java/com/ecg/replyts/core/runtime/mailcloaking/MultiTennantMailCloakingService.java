package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MultiTennantMailCloakingService implements MailCloakingService {
    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private AnonymizedMailConverter anonymizedMailConverter;

    @Override
    public MailAddress createdCloakedMailAddress(ConversationRole role, Conversation conversation) {
        return anonymizedMailConverter.fromSecretToMail(conversation, role);
    }

    @Override
    public boolean isCloaked(MailAddress mailAddress) {
        return anonymizedMailConverter.isCloaked(mailAddress);
    }

    @Override
    public Optional<CloakedReceiverContext> resolveUser(MailAddress mailAddress) {
        if (anonymizedMailConverter.isCloaked(mailAddress)) {

            String secret = anonymizedMailConverter.fromMailToSecret(mailAddress);
            MutableConversation conv = conversationRepository.getBySecret(secret);
            if (conv == null) {
                return Optional.empty();
            }
            ConversationRole role = conv.getSecretFor(ConversationRole.Buyer).equalsIgnoreCase(secret) ? ConversationRole.Buyer : ConversationRole.Seller;
            return Optional.of(new CloakedReceiverContext(conv, role));
        }

        return Optional.empty();
    }
}
