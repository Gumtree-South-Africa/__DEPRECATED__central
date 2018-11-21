package com.ecg.messagecenter.kjca.sync;

import com.ecg.comaas.kjca.coremod.shared.TextAnonymizer;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.persistence.ConversationThread;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlock;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeZone.UTC;

@Component
public class ConversationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationService.class);

    private final SimplePostBoxRepository postBoxRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationBlockRepository conversationBlockRepository;
    private final MailCloakingService mailCloakingService;
    private final TextAnonymizer textAnonymizer;

    @Autowired
    public ConversationService(
            final SimplePostBoxRepository postBoxRepository,
            final ConversationRepository conversationRepository,
            final ConversationBlockRepository conversationBlockRepository,
            final MailCloakingService mailCloakingService,
            final TextAnonymizer textAnonymizer) {

        this.postBoxRepository = postBoxRepository;
        this.conversationRepository = conversationRepository;
        this.conversationBlockRepository = conversationBlockRepository;
        this.mailCloakingService = mailCloakingService;
        this.textAnonymizer = textAnonymizer;
    }

    public Optional<PostBoxSingleConversationThreadResponse> getConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
        }

        ConversationBlock conversationBlock = conversationBlockRepository.byId(conversationId);
        boolean blockedByBuyer = false, blockedBySeller = false;
        if (conversationBlock != null) {
            blockedByBuyer = conversationBlock.getBuyerBlockedSellerAt().isPresent();
            blockedBySeller = conversationBlock.getSellerBlockedBuyerAt().isPresent();
        }

        return lookupConversation(postBox.getUnreadConversationsCapped().size(), email, conversationId, blockedByBuyer, blockedBySeller);
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
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
        } else {
            numUnread = postBox.getUnreadConversationsCapped().size();
        }
        return lookupConversation(numUnread, email, conversationId, blockedByBuyer, blockedBySeller);
    }

    public Void deleteConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (conversationThreadRequested.isPresent()) {
            postBox.removeConversation(conversationId);
            postBoxRepository.deleteConversations(postBox, Collections.singletonList(conversationId));
        }

        return null;
    }

    private Optional<PostBoxSingleConversationThreadResponse> lookupConversation(
            long numUnread,
            String email,
            String conversationId,
            boolean blockedByBuyer,
            boolean blockedBySeller) {

        Conversation conversation = conversationRepository.getById(conversationId);
        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id [{}] exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return Optional.empty();
        }

        final String sellerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation).getAddress();
        final String buyerAnonymousEmail = mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress();
        return PostBoxSingleConversationThreadResponse.create(
                numUnread, email, conversation, buyerAnonymousEmail, sellerAnonymousEmail, blockedByBuyer, blockedBySeller,
                textAnonymizer);
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

        //optimization to not cause too many write actions (potential for conflicts)
        if (needsUpdate) {
            PostBox<ConversationThread> postBoxToUpdate = new PostBox<>(email, Optional.of(postBox.getNewRepliesCounter().getValue()), threadsToUpdate);
            postBoxRepository.markConversationAsRead(postBoxToUpdate, updatedConversation);
            return postBoxToUpdate;
        } else {
            return postBox;
        }
    }
}
