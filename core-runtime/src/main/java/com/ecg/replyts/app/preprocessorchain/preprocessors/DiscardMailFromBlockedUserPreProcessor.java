package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("discardMailFromBlockedUserPreProcessor")
@ConditionalOnProperty(name = "blockedusers.preprocessor.enabled", havingValue = "true", matchIfMissing = true)
public class DiscardMailFromBlockedUserPreProcessor implements PreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(DiscardMailFromBlockedUserPreProcessor.class);

    private final BlockUserRepository blockUserRepository;
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public DiscardMailFromBlockedUserPreProcessor(BlockUserRepository blockUserRepository, UserIdentifierService userIdentifierService){
        this.blockUserRepository = blockUserRepository;
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    public void preProcess(MessageProcessingContext context) {
        LOG.trace("Checking if users in conversation {} are blocked", context.getConversation().getId());

        Conversation conversation = context.getConversation();

        String buyer = userIdentifierService.getBuyerUserId(conversation)
                .orElse(conversation.getBuyerId());
        String seller = userIdentifierService.getSellerUserId(conversation)
                .orElse(conversation.getSellerId());

        if (blockUserRepository.areUsersBlocked(buyer, seller)) {
            context.terminateProcessing(MessageState.DISCARDED, this,
                    "Conversations between users " + buyer + " and " + seller + " are blocked");
            LOG.debug("Users in conversation {} are blocked reply: {}", context.getConversation().getId(), context.getTermination().getReason());
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}


