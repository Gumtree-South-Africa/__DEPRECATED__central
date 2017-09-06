package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.ecg.replyts.core.api.persistence.ConversationIndexKey;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

public abstract class ConversationResumer {
    private static final Logger LOG = LoggerFactory.getLogger(ConversationResumer.class);

    public abstract boolean resumeExistingConversation(ConversationRepository repository, MessageProcessingContext context);

    public ConversationIndexKey keyFromCreatedEvent(ConversationCreatedEvent event) {
        return new ConversationIndexKey(event.getBuyerId(), event.getSellerId(), event.getAdId());
    }

    public ConversationIndexKey keyFromConversation(Conversation conversation) {
        return new ConversationIndexKey(conversation.getBuyerId(), conversation.getSellerId(), conversation.getAdId());
    }

    protected boolean tryResume(ConversationRepository repository, MessageProcessingContext context, MessageDirection direction, ConversationIndexKey key) {
        Optional<Conversation> existingConversation = repository.findExistingConversationFor(key);

        if (existingConversation.isPresent()) {
            context.setConversation((MutableConversation) existingConversation.get());
            context.setMessageDirection(direction);
            addNewCustomValuesToConversation(context);

            MDCConstants.setContextFields(context);

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
                LOG.trace("starter mail had a new custom value - adding this to the conversation: {}={}", customHeadersInNewMail.getKey(), customHeadersInNewMail.getValue());
            }
        }
    }
}