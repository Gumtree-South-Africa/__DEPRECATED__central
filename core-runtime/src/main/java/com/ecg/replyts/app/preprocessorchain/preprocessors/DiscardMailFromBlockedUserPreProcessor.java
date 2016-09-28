package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component("discardMailFromBlockedUserPreProcessor")
@ConditionalOnExpression("#{'${replyts.tenant}' == 'mp'}")
public class DiscardMailFromBlockedUserPreProcessor implements PreProcessor{
    private static final Logger LOG = LoggerFactory.getLogger(DiscardMailFromBlockedUserPreProcessor.class);

    private BlockUserRepository blockUserRepository;

    @Autowired
    public DiscardMailFromBlockedUserPreProcessor(BlockUserRepository blockUserRepository){
        this.blockUserRepository = blockUserRepository;
    }

    @Override
    public void preProcess(MessageProcessingContext context) {
        LOG.debug("Checking if users in conversation {} are blocked", context.getConversation().getId());

        Conversation conversation = context.getConversation();
        String buyerId = conversation.getBuyerId();
        String sellerId = conversation.getSellerId();

        if (blockUserRepository.areUsersBlocked(buyerId, sellerId)) {
            context.terminateProcessing(MessageState.DISCARDED, this,
                    "Conversations between users " + buyerId + " and " + sellerId + " are blocked");
            LOG.debug("Users in conversation {} are blocked reply: {}", context.getConversation().getId(), context.getTermination().getReason());
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}


