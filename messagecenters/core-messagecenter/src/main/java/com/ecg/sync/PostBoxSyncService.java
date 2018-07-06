package com.ecg.sync;

import com.codahale.metrics.Counter;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PostBoxSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxSyncService.class);
    private static final Logger DIFF_LOG = LoggerFactory.getLogger("diffLogger");
    private static final int MAX_CONVERSATIONS_TO_RETRIEVE_PER_USER = 3;

    private final Counter diffFailureCounter = TimingReports.newCounter("webapi.diff.failed");

    private final ConversationRepository conversationRepository;
    private final UserIdentifierService userIdentifierService;
    private final Session session;
    private final SimplePostBoxRepository postBoxRepository;
    private final PreparedStatement conversationStatement;

    public PostBoxSyncService(
            ConversationRepository conversationRepository,
            UserIdentifierService userIdentifierService,
            Session session,
            SimplePostBoxRepository postBoxRepository
    ) {
        this.conversationRepository = conversationRepository;
        this.userIdentifierService = userIdentifierService;
        this.session = session;
        this.postBoxRepository = postBoxRepository;
        this.conversationStatement = this.session.prepare("SELECT conversation_id FROM mb_postbox WHERE postbox_id = ? AND conversation_id = ? LIMIT 1");
    }

    /**
     * Methods takes collection of conversation events and tries to to find 'userId' in it.
     * Best-case scenario is to use only one call to conversation repository but because tenants contain multiple conversation which
     * does not contain any conversation event, we repeat this action several times to be more likely that we find something.
     *
     * @param email   email of the client.
     * @param convIds list of conversation from which 'userId' is going to be retrieved.
     * @return 'userId' of the client having the provided email.
     */
    public String getUserId(String email, List<String> convIds) {
        if (convIds.isEmpty()) {
            throw new UserIdNotFoundException("No conversations to process", false);
        }

        boolean eventsFound = false;
        for (String conversationId: convIds) {
            MutableConversation conversation = conversationRepository.getById(conversationId);

            if (conversation == null) {
                LOG.debug("V2 Migration: Conversation Event not found, e-mail: {}, conversation: {}", email, conversationId);
                continue;
            }

            eventsFound = true;

            if (email.equalsIgnoreCase(conversation.getBuyerId())) {
                Optional<String> userId = userIdentifierService.getBuyerUserId(conversation);
                if (userId.isPresent()) {
                    return userId.get();
                }

                LOG.debug("V2 Migration: UserId was not found for email: {}, role: buyer, conversation: {}", email, conversationId);
            } else if (email.equalsIgnoreCase(conversation.getSellerId())) {
                Optional<String> userId = userIdentifierService.getSellerUserId(conversation);
                if (userId.isPresent()) {
                    return userId.get();
                }

                LOG.debug("V2 Migration: UserId was not found for email: {}, role: seller, conversation: {}", email, conversationId);
            }
        }

        throw new UserIdNotFoundException("Cannot find 'userId' for email: " + email + ", Events: " + eventsFound + ", Conversations: " + convIds);
    }

    /**
     * Returns conversation ID if conversation exists or {@link Collections#emptyList()}.
     *
     * @param email          email which postbox belongs to.
     * @param conversationId conversation id to check.
     * @return conversation ID in list if conversation exists.
     */
    public List<String> conversationExists(String email, String conversationId) {
        ResultSet resultSet = session.execute(conversationStatement.bind(email.toLowerCase(), conversationId));
        if (resultSet.iterator().hasNext()) {
            return Collections.singletonList(conversationId);
        }

        throw new UserIdNotFoundException("Conversation in Postbox not found. Email: " + email + ", Conversation-ID: " + conversationId);
    }

    /**
     * Returns {@link #MAX_CONVERSATIONS_TO_RETRIEVE_PER_USER} latest conversations.
     *
     * @param email email which postbox belongs to.
     * @return latest conversations.
     */
    @SuppressWarnings("unchecked")
    public List<String> getConversationId(String email) {
        com.ecg.messagecenter.core.persistence.simple.PostBox<AbstractConversationThread> postbox =
                (com.ecg.messagecenter.core.persistence.simple.PostBox<AbstractConversationThread>) postBoxRepository.byId(PostBoxId.fromEmail(email));

        return postbox.getConversationThreads().stream()
                .map(AbstractConversationThread::getConversationId)
                .limit(MAX_CONVERSATIONS_TO_RETRIEVE_PER_USER)
                .collect(Collectors.toList());
    }

    public <T> Function<Throwable, Optional<T>> handleOpt(Counter errorCounter, String errorMessage) {
        return ex -> {
            if (ex.getCause() instanceof UserIdNotFoundException) {
                UserIdNotFoundException userEx = (UserIdNotFoundException) ex.getCause();
                if (userEx.isLoggable()) {
                    LOG.warn("V2 Migration: " + ex.getMessage(), ex.getCause());
                }
                return Optional.empty();
            } else {
                errorCounter.inc();
                LOG.error(errorMessage, ex);
                throw new RuntimeException(ex);
            }
        };
    }

    public <T> Function<Throwable, T> handle(Counter errorCounter, String errorMessage) {
        return ex -> {
            errorCounter.inc();
            LOG.error(errorMessage, ex);
            throw new RuntimeException(ex);
        };
    }

    public Function<Throwable, Void> handleDiff(String errorMessage) {
        return ex -> {
            diffFailureCounter.inc();
            DIFF_LOG.error(errorMessage, ex);
            return null;
        };
    }
}
