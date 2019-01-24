package com.ecg.messagecenter.kjca.sync;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.Session;
import com.ecg.messagebox.model.ConversationThread;
import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.CassandraPostBoxService;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimpleMessageCenterRepository;
import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponseBuilder;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import com.ecg.sync.CustomExecutorsFactory;
import com.ecg.sync.PostBoxSyncService;
import com.ecg.sync.UserIdNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;

@Component
@ConditionalOnProperty(name = "webapi.sync.ca.enabled", havingValue = "true")
public class WebApiSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(WebApiSyncService.class);

    private final Counter oldModelFailureCounter = TimingReports.newCounter("webapi.diff.oldModel.failed");
    private final Counter newModelFailureCounter = TimingReports.newCounter("webapi.diff.newModel.failed");

    private final BlockService blockService;
    private final BlockUserRepository blockUserRepository;
    private final ConversationService conversationService;
    private final ConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final SimpleMessageCenterRepository postBoxRepository;
    private final CassandraPostBoxService postBoxService;
    private final PostBoxSyncService postBoxSyncService;
    private final ExecutorService oldExecutor;
    private final ExecutorService newExecutor;
    private final PostBoxResponseBuilder responseBuilder;

    // the old model is restricted to a hard limit of 500 total messages per conversation
    // (see ProcessingFinalizer#MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION),
    // so keeping it until we migrate to the new data model
    private final int messagesLimit;

    @Autowired
    public WebApiSyncService(
            BlockService blockService,
            BlockUserRepository blockUserRepository,
            ConversationBlockRepository conversationBlockRepository,
            ConversationService conversationService,
            CassandraPostBoxService postBoxService,
            SimpleMessageCenterRepository postBoxRepository,
            ConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            CustomExecutorsFactory customExecutorsFactory,
            @Qualifier("cassandraSessionForMb") Session session,
            @Value("${messagebox.messagesLimit:500}") int messagesLimit) {

        this.blockService = blockService;
        this.blockUserRepository = blockUserRepository;
        this.conversationService = conversationService;
        this.postBoxService = postBoxService;
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.postBoxRepository = postBoxRepository;
        this.postBoxSyncService = new PostBoxSyncService(conversationRepository, userIdentifierService, session, postBoxRepository);
        this.responseBuilder = new PostBoxResponseBuilder(conversationBlockRepository, 30);
        this.messagesLimit = messagesLimit;
        this.oldExecutor = customExecutorsFactory.webApiExecutorService("old-webapi-executor");
        this.newExecutor = customExecutorsFactory.webApiExecutorService("new-webapi-executor");
    }

    public com.ecg.messagecenter.core.persistence.simple.PostBox getPostBox(String email, Integer size, Integer page) {

        /*
         * First two calls are dedicated to find real UserID using the email:
         * - find first conversation using V1
         * - find created event to this conversation and look into custom headers to resolve real UserID belonging to this email
         * - if user does not have any conversation in V1 or UserID is not found in conversation event then UserIdNotFound is thrown
         * and Warning is logged
         */
        CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.getConversationId(email), newExecutor)
                .thenApply(convIds -> postBoxSyncService.getUserId(email, convIds))
                .thenApply(userId -> Optional.of(postBoxService.getConversations(userId, Visibility.ACTIVE, page * size, size)))
                .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New GetPostBox Failed - email: " + email));

        CompletableFuture<com.ecg.messagecenter.core.persistence.simple.PostBox> oldModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxRepository.byId(PostBoxId.fromEmail(email)), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old GetPostBox Failed - email: " + email));

        oldModelFuture.thenCombine(newModelFuture, (oldbox, newbox) -> {
            Diffing.diffPostBox(newbox, responseBuilder.createRefreshedPostBox(oldbox), email);
            return oldbox;
        });

        return oldModelFuture.join();
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        CompletableFuture<Optional<ConversationThread>> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.conversationExists(email, conversationId), newExecutor)
                .thenApply(conversationIds -> postBoxSyncService.getUserId(email, conversationIds))
                .thenApply(userId -> {
                    try {
                        return postBoxService.markConversationAsRead(userId, conversationId, null, messagesLimit);
                    } catch (InterruptedException e) {
                        throw new CancellationException(e.getMessage());
                    }
                })
                .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New ReadConversation Failed - email: " + email));

        CompletableFuture<Optional<PostBoxSingleConversationThreadResponse>> oldModelFuture = CompletableFuture
                .supplyAsync(() -> conversationService.readConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old ReadConversation Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
        return oldModelFuture.join();
    }

    public void deleteConversation(String email, String conversationId) {
        CompletableFuture<Optional<PostBox>> newModelFuture = CompletableFuture
                .supplyAsync(() -> postBoxSyncService.getUserId(email, Collections.singletonList(conversationId)), newExecutor)
                .thenApply(userId -> {
                    try {
                        return Optional.of(postBoxService.archiveConversations(userId, Collections.singletonList(conversationId), 0, messagesLimit));
                    } catch (InterruptedException e) {
                        throw new CancellationException(e.getMessage());
                    }
                })
                .exceptionally(postBoxSyncService.handleOpt(newModelFailureCounter, "New DeleteConversation Failed - email: " + email));

        CompletableFuture<Void> oldModelFuture = CompletableFuture
                .supplyAsync(() -> conversationService.deleteConversation(email, conversationId), oldExecutor)
                .exceptionally(postBoxSyncService.handle(oldModelFailureCounter, "Old DeleteConversation Failed - email: " + email));

        CompletableFuture.allOf(newModelFuture, oldModelFuture).join();
    }

    public boolean blockConversation(String email, String conversationId) {
        try {
            processWithIdentities((blocker, blockie) -> blockUserRepository.blockUser(blocker, blockie), conversationId, email);
        } catch (UserIdNotFoundException ex) {
            LOG.warn("V2 Migration: " + ex.getMessage(), ex.getCause());
            return false;
        } catch (Exception ex) {
            LOG.error("V2 Migration: " + ex.getMessage(), ex);
            return false;
        } finally {
            blockService.blockConversation(email, conversationId);
        }

        return true;
    }

    public boolean unblockConversation(String email, String conversationId) {
        try {
            processWithIdentities((blocker, blockie) -> blockUserRepository.unblockUser(blocker, blockie), conversationId, email);
        } catch (UserIdNotFoundException ex) {
            LOG.warn("V2 Migration: " + ex.getMessage(), ex.getCause());
            return false;
        } catch (Exception ex) {
            LOG.error("V2 Migration: " + ex.getMessage(), ex);
            return false;
        } finally {
            blockService.unblockConversation(email, conversationId);
        }

        return true;
    }

    private void processWithIdentities(BiConsumer<String, String> consumer, String conversationId, String email) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            throw new UserIdNotFoundException("Conversation Event not found, e-mail: " + email + ", conversation: " + conversationId);
        }

        // Store user-id first (if it founds) otherwise use email
        // it cannot be changed because there is no other way how to
        // inject user-id into conversation with a new message

        String buyer = userIdentifierService.getBuyerUserId(conversation)
                .orElse(conversation.getBuyerId());

        String seller = userIdentifierService.getSellerUserId(conversation)
                .orElse(conversation.getSellerId());

        if (conversation.getBuyerId().equalsIgnoreCase(email)) {
            consumer.accept(buyer, seller);
        } else {
            consumer.accept(seller, buyer);
        }
    }
}
