package com.ecg.replyts.app.preprocessorchain.preprocessors;


import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class ConversationResumer {

    private static final Logger LOG = LoggerFactory.getLogger(ConversationResumer.class);

    private final ConversationRepository conversationRepository;
    private final boolean enableResuming;

    @Autowired
    ConversationResumer(ConversationRepository conversationRepository, @Value("${replyts.allowConversationResume:true}")boolean enableResuming) {
        this.conversationRepository = conversationRepository;
        this.enableResuming = enableResuming;
    }

    public boolean resumeExistingConversation(MessageProcessingContext context) {
        if (!enableResuming) {
            return false;
        }

        ConversationStartInfo info = new ConversationStartInfo(context);
        return
                tryResume(context, info.asConversationIndexKeyBuyerToSeller(), MessageDirection.BUYER_TO_SELLER) ||
                tryResume(context, info.asConversationIndexKeySellerToBuyer(), MessageDirection.SELLER_TO_BUYER);
    }

    private boolean tryResume(MessageProcessingContext context, ConversationIndexKey key, MessageDirection direction) {
        Optional<Conversation> existingConversation = conversationRepository.findExistingConversationFor(key);
        if (existingConversation.isPresent()) {
            context.setConversation((MutableConversation) existingConversation.get());
            context.setMessageDirection(direction);

            addNewCustomValuesToConversation(context);

            LOG.debug("Attaching message to existing conversation '{}': buyerId, sellerId and adId match ({}).", existingConversation.get().getId(), direction);
            return true;
        }
        return false;
    }

    private void addNewCustomValuesToConversation(MessageProcessingContext context) {
        // this is a conversation starter mail - therefore it might bring it's own custom values.
        // I do not want to allow overriding of new values, but adding new custom values does make sense...

        Map<String, String> existingCustomValues = context.getConversation().getCustomValues();
        for (Map.Entry<String, String> customHeadersInNewMail : context.getMail().getCustomHeaders().entrySet()) {
            if (!existingCustomValues.containsKey(customHeadersInNewMail.getKey())) {
                context.addCommand(new AddCustomValueCommand(context.getConversation().getId(), customHeadersInNewMail.getKey(), customHeadersInNewMail.getValue()));
                LOG.debug("starter mail had a new custom value - adding this to the conversation: {}={}", customHeadersInNewMail.getKey(), customHeadersInNewMail.getValue());
            }
        }
    }
}
