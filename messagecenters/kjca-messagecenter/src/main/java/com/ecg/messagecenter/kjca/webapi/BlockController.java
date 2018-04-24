package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.kjca.persistence.block.ConversationBlock;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.joda.time.DateTimeZone.UTC;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class BlockController {

    private final ConversationRepository conversationRepository;
    private final ConversationBlockRepository conversationBlockRepository;

    @Autowired
    public BlockController(
            final ConversationRepository conversationRepository,
            final ConversationBlockRepository conversationBlockRepository) {

        this.conversationRepository = conversationRepository;
        this.conversationBlockRepository = conversationBlockRepository;
    }

    @PostMapping("/postboxes/{email}/conversations/{conversationId}/block")
    public ResponseEntity<Void> blockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (wrongConversationOrEmail(email, conversation)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        java.util.Optional<DateTime> now = java.util.Optional.of(DateTime.now(UTC));

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        java.util.Optional<DateTime> buyerBlockedSellerAt = java.util.Optional.empty();
        java.util.Optional<DateTime> sellerBlockerBuyerAt = java.util.Optional.empty();

        if (conversationBlock != null) {
            buyerBlockedSellerAt = conversation.getBuyerId().equalsIgnoreCase(email) ? now : conversationBlock.getBuyerBlockedSellerAt();
            sellerBlockerBuyerAt = conversation.getSellerId().equalsIgnoreCase(email) ? now : conversationBlock.getSellerBlockedBuyerAt();
        }

        conversationBlock = new ConversationBlock(
                conversationId,
                ConversationBlock.LATEST_VERSION,
                conversation.getBuyerId().equalsIgnoreCase(email) ? now : buyerBlockedSellerAt,
                conversation.getSellerId().equalsIgnoreCase(email) ? now : sellerBlockerBuyerAt
        );

        conversationBlockRepository.write(conversationBlock);

        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/postboxes/{email}/conversations/{conversationId}/block")
    public ResponseEntity<Void> unblockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (wrongConversationOrEmail(email, conversation)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        if (conversationBlock == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        boolean buyerUnblockedSeller = conversation.getBuyerId().equalsIgnoreCase(email);
        boolean sellerUnblockedBuyer = conversation.getSellerId().equalsIgnoreCase(email);
        conversationBlockRepository.write(new ConversationBlock(
                conversationId,
                ConversationBlock.LATEST_VERSION,
                buyerUnblockedSeller ? java.util.Optional.empty() : conversationBlock.getBuyerBlockedSellerAt(),
                sellerUnblockedBuyer ? java.util.Optional.empty() : conversationBlock.getSellerBlockedBuyerAt()
        ));

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // Missing conversation or the email doesn't belong to either the seller or the buyer
    private boolean wrongConversationOrEmail(@PathVariable("email") String email, MutableConversation conversation) {
        return conversation == null ||
                (!conversation.getBuyerId().equalsIgnoreCase(email) && !conversation.getSellerId().equalsIgnoreCase(email));
    }
}
