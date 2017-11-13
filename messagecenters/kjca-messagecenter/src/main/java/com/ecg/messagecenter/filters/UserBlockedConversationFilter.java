package com.ecg.messagecenter.filters;

import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.Lists;

import java.util.List;

public class UserBlockedConversationFilter implements Filter {
    public static final String DESC_BUYER_BLOCKED_SELLER = "Buyer blocked seller";
    public static final String DESC_SELLER_BLOCKED_BUYER = "Seller blocked buyer";

    private final int score;
    private final ConversationBlockRepository conversationBlockRepository;

    public UserBlockedConversationFilter(ConversationBlockRepository conversationBlockRepository, int score) {
        this.score = score;
        this.conversationBlockRepository = conversationBlockRepository;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        ConversationBlock conversationBlock = conversationBlockRepository.byId(context.getConversation().getId());

        List<FilterFeedback> feedbacks = Lists.newArrayList();

        if (conversationBlock == null) {
            return feedbacks;
        }

        if (conversationBlock.getBuyerBlockedSellerAt().isPresent()) {
            feedbacks.add(
                    new FilterFeedback(
                            "Blocked on: " + conversationBlock.getBuyerBlockedSellerAt().get(),
                            DESC_BUYER_BLOCKED_SELLER,
                            score,
                            FilterResultState.DROPPED
                    )
            );
        }

        if (conversationBlock.getSellerBlockedBuyerAt().isPresent()) {
            feedbacks.add(
                    new FilterFeedback(
                            "Blocked on: " + conversationBlock.getSellerBlockedBuyerAt().get(),
                            DESC_SELLER_BLOCKED_BUYER,
                            score,
                            FilterResultState.DROPPED
                    )
            );
        }

        return feedbacks;
    }
}
