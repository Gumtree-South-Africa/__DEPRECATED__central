package com.ecg.messagecenter.kjca.sync;

import com.ecg.messagecenter.kjca.persistence.block.ConversationBlock;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static org.joda.time.DateTimeZone.UTC;

@Component
public class BlockService {

    private final ConversationRepository conversationRepository;
    private final ConversationBlockRepository conversationBlockRepository;

    @Autowired
    public BlockService(
            ConversationRepository conversationRepository,
            ConversationBlockRepository conversationBlockRepository) {

        this.conversationRepository = conversationRepository;
        this.conversationBlockRepository = conversationBlockRepository;
    }

    public boolean blockConversation(String email, String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (wrongConversationOrEmail(email, conversation)) {
            return false;
        }

        Optional<DateTime> now = Optional.of(DateTime.now(UTC));

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        Optional<DateTime> buyerBlockedSellerAt = Optional.empty();
        Optional<DateTime> sellerBlockerBuyerAt = Optional.empty();

        if (conversationBlock != null) {
            buyerBlockedSellerAt = conversation.getBuyerId().equalsIgnoreCase(email) ? now : conversationBlock.getBuyerBlockedSellerAt();
            sellerBlockerBuyerAt = conversation.getSellerId().equalsIgnoreCase(email) ? now : conversationBlock.getSellerBlockedBuyerAt();
        }

        conversationBlock = new ConversationBlock(
                conversationId,
                ConversationBlock.LATEST_VERSION,
                conversation.getBuyerId().equalsIgnoreCase(email) ? now : buyerBlockedSellerAt,
                conversation.getSellerId().equalsIgnoreCase(email) ? now : sellerBlockerBuyerAt);

        conversationBlockRepository.write(conversationBlock);
        return true;
    }

    public boolean unblockConversation(String email, String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (wrongConversationOrEmail(email, conversation)) {
            return false;
        }

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        if (conversationBlock == null) {
            return false;
        }

        boolean buyerUnblockedSeller = conversation.getBuyerId().equalsIgnoreCase(email);
        boolean sellerUnblockedBuyer = conversation.getSellerId().equalsIgnoreCase(email);

        conversationBlockRepository.write(new ConversationBlock(
                conversationId,
                ConversationBlock.LATEST_VERSION,
                buyerUnblockedSeller ? Optional.empty() : conversationBlock.getBuyerBlockedSellerAt(),
                sellerUnblockedBuyer ? Optional.empty() : conversationBlock.getSellerBlockedBuyerAt()));

        return true;
    }

    // Missing conversation or the email doesn't belong to either the seller or the buyer
    private boolean wrongConversationOrEmail(String email, MutableConversation conversation) {
        return conversation == null ||
                (!conversation.getBuyerId().equalsIgnoreCase(email) && !conversation.getSellerId().equalsIgnoreCase(email));
    }
}
