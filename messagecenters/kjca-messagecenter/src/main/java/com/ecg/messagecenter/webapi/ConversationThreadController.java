package com.ecg.messagecenter.webapi;

import ca.kijiji.replyts.TextAnonymizer;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.UnreadCountCachePopulater;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterBlockCommand;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxConversationCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationThreadController.class);

    private SimplePostBoxRepository postBoxRepository;
    private ConversationRepository conversationRepository;
    private ConversationBlockRepository conversationBlockRepository;
    private MailCloakingService mailCloakingService;
    private TextAnonymizer textAnonymizer;
    private UnreadCountCachePopulater unreadCountCachePopulater;

    @Autowired
    public ConversationThreadController(final SimplePostBoxRepository postBoxRepository,
                                        final ConversationRepository conversationRepository,
                                        final ConversationBlockRepository conversationBlockRepository,
                                        final MailCloakingService mailCloakingService,
                                        final TextAnonymizer textAnonymizer,
                                        final UnreadCountCachePopulater unreadCountCachePopulater) {
        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.conversationBlockRepository = conversationBlockRepository;
        this.mailCloakingService = mailCloakingService;
        this.textAnonymizer = textAnonymizer;
        this.unreadCountCachePopulater = unreadCountCachePopulater;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    /*
     * newCounterMode always FALSE
     */
    @GetMapping(value = "/postboxes/{email}/conversations/{conversationId}")
    ResponseObject<?> getPostBox(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response);
        }

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        boolean blockedByBuyer = false, blockedBySeller = false;
        if (conversationBlock != null) {
            blockedByBuyer = conversationBlock.getBuyerBlockedSellerAt().isPresent();
            blockedBySeller = conversationBlock.getSellerBlockedBuyerAt().isPresent();
        }

        return lookupConversation(postBox.getUnreadConversationsCapped().size(), email, conversationId, blockedByBuyer, blockedBySeller, response);
    }

    /*
     * newCounterMode always FALSE
     */
    @PutMapping("/postboxes/{email}/conversations/{conversationId}")
    ResponseObject<?> readPostBox(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            HttpServletResponse response) {

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return entityNotFound(response);
        }

        boolean needToMarkAsRead = conversationThreadRequested.get().isContainsUnreadMessages();
        if (needToMarkAsRead) {
            int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
            postBox.decrementNewReplies(unreadMessages);
            postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
        }

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        boolean blockedByBuyer = false, blockedBySeller = false;
        if (conversationBlock != null) {
            blockedByBuyer = conversationBlock.getBuyerBlockedSellerAt().isPresent();
            blockedBySeller = conversationBlock.getSellerBlockedBuyerAt().isPresent();
        }

        long numUnread;
        if (needToMarkAsRead) {
            final PostBox updatedPostbox = markConversationAsRead(email, conversationId, postBox);
            numUnread = updatedPostbox.getUnreadConversationsCapped().size();
            unreadCountCachePopulater.populateCache(updatedPostbox);
        } else {
            numUnread = postBox.getUnreadConversationsCapped().size();
        }
        return lookupConversation(numUnread, email, conversationId, blockedByBuyer, blockedBySeller, response);
    }

    private ResponseObject<?> lookupConversation(
            long numUnread,
            String email,
            String conversationId,
            boolean blockedByBuyer,
            boolean blockedBySeller,
            HttpServletResponse response
    ) {
        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id [{}] exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return entityNotFound(response);
        }

        final String sellerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation).getAddress();
        final String buyerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress();
        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(
                numUnread, email, conversation, buyerAnonymousEmail, sellerAnonymousEmail, blockedByBuyer, blockedBySeller,
                textAnonymizer);
        if (created.isPresent()) {
            return ResponseObject.of(created.get());
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound(response);
        }

    }

    private PostBox<ConversationThread> markConversationAsRead(String email, String conversationId, PostBox<ConversationThread> postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        boolean needsUpdate = false;
        ConversationThread updatedConversation = null;
        for (ConversationThread item : postBox.getConversationThreads()) {
            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                updatedConversation = new ConversationThread(
                        item.getAdId(),
                        item.getConversationId(),
                        item.getCreatedAt(),
                        now(UTC),
                        item.getReceivedAt(),
                        false, // mark as read
                        item.getPreviewLastMessage(),
                        item.getBuyerName(),
                        item.getSellerName(),
                        item.getBuyerId(),
                        item.getMessageDirection());

                threadsToUpdate.add(updatedConversation);
                needsUpdate = true;
            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox<ConversationThread> postBoxToUpdate = new PostBox<>(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
            postBoxRepository.markConversationAsRead(postBoxToUpdate, updatedConversation);
            return postBoxToUpdate;
        } else {
            return postBox;
        }
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }

    @DeleteMapping("/postboxes/{email}/conversations/{conversationId}")
    public ResponseEntity<Void> deleteSingleConversation(@PathVariable("email") final String email,
                                                         @PathVariable("conversationId") String conversationId,
                                                         HttpServletResponse response) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (conversationThreadRequested.isPresent()) {
            postBox.removeConversation(conversationId);
            postBoxRepository.deleteConversations(postBox, Collections.singletonList(conversationId));
        }

        unreadCountCachePopulater.populateCache(postBox);

        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
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
