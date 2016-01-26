package com.ecg.replyts.core.api.model;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.google.common.base.Optional;

/**
 * @author mhuttar
 */
public interface MailCloakingService {
    Optional<CloakedReceiverContext> resolveUser(MailAddress mailAddress);

    MailAddress createdCloakedMailAddress(ConversationRole role, Conversation conversation);

    boolean isCloaked(MailAddress mailAddress);
}
