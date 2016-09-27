package com.ecg.messagecenter.webapi;

import ca.kijiji.replyts.TextAnonymizer;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.block.ConversationBlock;
import com.ecg.messagecenter.persistence.block.RiakConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.DefaultRiakSimplePostBoxRepository;
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
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

@Controller
class ConversationThreadController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationThreadController.class);

    private static final Timer API_POSTBOX_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-by-id");
    private static final Timer API_POSTBOX_DELETE_CONVERSATION_BY_ID = TimingReports.newTimer("webapi-postbox-delete-conversation-by-id");
    private static final Timer API_POSTBOX_BLOCK_CONVERSATION = TimingReports.newTimer("webapi-postbox-block-conversation");
    private static final Timer API_POSTBOX_UNBLOCK_CONVERSATION = TimingReports.newTimer("webapi-postbox-unblock-conversation");
    private static final Histogram API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION = TimingReports.newHistogram("webapi-postbox-num-messages-of-conversation");
    private static final Counter API_POSTBOX_EMPTY_CONVERSATION = TimingReports.newCounter("webapi-postbox-empty-conversation");

    @Autowired
    private DefaultRiakSimplePostBoxRepository postBoxRepository;
    @Autowired
    private ConversationRepository conversationRepository;
    @Autowired
    private RiakConversationBlockRepository conversationBlockRepository;
    @Autowired
    private MailCloakingService mailCloakingService;
    @Autowired
    private TextAnonymizer textAnonymizer;

    @Value("${replyts.maxConversationAgeDays}")
    private int maxAgeDays;

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<?> getPostBoxConversationByEmailAndConversationId(
            @PathVariable("email") String email,
            @PathVariable("conversationId") String conversationId,
            @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
            HttpServletRequest request,
            HttpServletResponse response) {

        try (Timer.Context ignored = API_POSTBOX_CONVERSATION_BY_ID.time()) {
            PostBox postBox = postBoxRepository.byId(email);

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (!conversationThreadRequested.isPresent()) {
                return entityNotFound(response);
            }

            boolean needToMarkAsRead = markAsRead(request) && conversationThreadRequested.get().isContainsUnreadMessages();
            if (needToMarkAsRead) {
                postBox.decrementNewReplies();
                postBoxRepository.write(postBox);
            }

            ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
            boolean blockedByBuyer = false, blockedBySeller = false;
            if (conversationBlock != null) {
                blockedByBuyer = conversationBlock.getBuyerBlockedSellerAt().isPresent();
                blockedBySeller = conversationBlock.getSellerBlockedBuyerAt().isPresent();
            }

            if (newCounterMode) {
                if (needToMarkAsRead) {
                    markConversationAsRead(email, conversationId, postBox);
                }
                return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId, blockedByBuyer, blockedBySeller, response);
            } else {
                long numUnread;
                if (needToMarkAsRead) {
                    numUnread = markConversationAsRead(email, conversationId, postBox);
                } else {
                    numUnread = postBox.getUnreadConversationsCapped().size();
                }
                return lookupConversation(numUnread, email, conversationId, blockedByBuyer, blockedBySeller, response);
            }
        }
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
            API_NUM_REQUESTED_NUM_MESSAGES_OF_CONVERSATION.update(created.get().getMessages().size());
            return ResponseObject.of(created.get());
        } else {
            API_POSTBOX_EMPTY_CONVERSATION.inc();
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return entityNotFound(response);
        }

    }

    private long markConversationAsRead(String email, String conversationId, PostBox<ConversationThread> postBox) {
        List<ConversationThread> threadsToUpdate = new ArrayList<>();

        boolean needsUpdate = false;
        for (ConversationThread item : postBox.getConversationThreads()) {

            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {

                threadsToUpdate.add(new ConversationThread(
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
                        item.getMessageDirection()
                ));

                needsUpdate = true;

            } else {
                threadsToUpdate.add(item);
            }
        }

        long numUnreadCounter;

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox postBoxToUpdate = new PostBox(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate, maxAgeDays);
            postBoxRepository.write(postBoxToUpdate);
            numUnreadCounter = postBoxToUpdate.getUnreadConversationsCapped().size();
        } else {
            numUnreadCounter = postBox.getUnreadConversationsCapped().size();
        }

        return numUnreadCounter;
    }

    private ResponseObject<?> entityNotFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return ResponseObject.of(RequestState.ENTITY_NOT_FOUND);
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }

    @RequestMapping(value = MessageCenterGetPostBoxConversationCommand.MAPPING,
            method = {RequestMethod.DELETE})
    public ResponseEntity<Void> deleteSingleConversation(@PathVariable("email") final String email,
                                                         @PathVariable("conversationId") String conversationId,
                                                         HttpServletResponse response) {
        try (Timer.Context ignored = API_POSTBOX_DELETE_CONVERSATION_BY_ID.time()) {
            PostBox postBox = postBoxRepository.byId(email);

            Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
            if (conversationThreadRequested.isPresent()) {
                postBox.removeConversation(conversationId);
                postBoxRepository.write(postBox, ImmutableList.of(conversationId));
            }

            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            LOGGER.warn("An unexpected exception occurred!", e);

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(
            value = MessageCenterBlockCommand.MAPPING,
            method = {RequestMethod.POST}
    )
    public ResponseEntity<Void> blockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId
    ) {
        try (Timer.Context ignored = API_POSTBOX_BLOCK_CONVERSATION.time()) {
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
    }

    @RequestMapping(
            value = MessageCenterBlockCommand.MAPPING,
            method = {RequestMethod.DELETE}
    )
    public ResponseEntity<Void> unblockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId
    ) {
        try (Timer.Context ignored = API_POSTBOX_UNBLOCK_CONVERSATION.time()) {
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
    }

    // Missing conversation or the email doesn't belong to either the seller or the buyer
    private boolean wrongConversationOrEmail(@PathVariable("email") String email, MutableConversation conversation) {
        return conversation == null ||
                (!conversation.getBuyerId().equalsIgnoreCase(email) && !conversation.getSellerId().equalsIgnoreCase(email));
    }
}
