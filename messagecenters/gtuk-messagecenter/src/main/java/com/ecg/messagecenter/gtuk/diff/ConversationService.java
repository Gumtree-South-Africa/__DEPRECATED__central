package com.ecg.messagecenter.gtuk.diff;

import com.ecg.messagecenter.gtuk.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.gtuk.webapi.ConversationCustomValue;
import com.ecg.messagecenter.gtuk.webapi.DeletedCustomValue;
import com.ecg.messagecenter.gtuk.webapi.FlaggedCustomValue;
import com.ecg.messagecenter.gtuk.webapi.responses.PostBoxSingleConversationThreadResponse;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import com.gumtree.gumshield.api.client.GumshieldApi;
import com.gumtree.gumshield.api.domain.user_message_report.ApiFlaggedConversation;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Component
public class ConversationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConversationService.class);

    private static final Object SUCCESS = new Object();

    private final SimplePostBoxRepository postBoxRepository;
    private final MutableConversationRepository conversationRepository;
    private final SearchIndexer searchIndexer;
    private final DomainObjectConverter converter;
    private final GumshieldApi gumshieldApi;
    private final ConversationEventListeners conversationEventListeners;

    @Autowired
    public ConversationService(
            SimplePostBoxRepository postBoxRepository,
            MutableConversationRepository conversationRepository,
            SearchIndexer searchIndexer,
            MailCloakingService mailCloakingService,
            GumshieldApi gumshieldApi,
            ConversationEventListeners conversationEventListeners) {

        this.conversationRepository = conversationRepository;
        this.postBoxRepository = postBoxRepository;
        this.searchIndexer = searchIndexer;
        this.converter = new DomainObjectConverter(conversationRepository, mailCloakingService);
        this.gumshieldApi = gumshieldApi;
        this.conversationEventListeners = conversationEventListeners;
    }

    public Optional<PostBoxSingleConversationThreadResponse> getConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
        }

        return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId);
    }

    public Optional<PostBoxSingleConversationThreadResponse> readConversation(String email, String conversationId) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        Optional<ConversationThread> conversationThreadRequested = postBox.lookupConversation(conversationId);
        if (!conversationThreadRequested.isPresent()) {
            return Optional.empty();
        }

        if (conversationThreadRequested.get().isContainsUnreadMessages()) {
            int unreadMessages = postBoxRepository.unreadCountInConversation(PostBoxId.fromEmail(postBox.getEmail()), conversationId);
            postBox.decrementNewReplies(unreadMessages);
            postBoxRepository.markConversationAsRead(postBox, conversationThreadRequested.get());
        }

        return lookupConversation(postBox.getNewRepliesCounter().getValue(), email, conversationId);
    }

    public Optional<Object> reportConversation(String email, String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return Optional.empty();
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, ConversationCustomValue.AT_POSTFIX, now.toString()));
        markConversation(conversation, new FlaggedCustomValue(buyerOrSeller, ConversationCustomValue.DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        markConversationAsRead(conversationId, postBox);

        ConversationRts conversationRts = converter.convertConversation(conversation);

        ApiFlaggedConversation flaggedConversation = new ApiFlaggedConversation();
        flaggedConversation.setAdvertId(Long.valueOf(conversationRts.getAdId()));
        flaggedConversation.setConversationId(conversationRts.getId());
        flaggedConversation.setReportedByEmail(email);
        flaggedConversation.setReportedDate(DateTime.parse(getFlaggedTime(conversationRts, email)));
        flaggedConversation.setReportedForEmail(getOtherEmail(conversationRts, email));
        gumshieldApi.conversationApi().flagConversation(flaggedConversation);
        return Optional.of(SUCCESS);
    }

    public Optional<ConversationRts> deleteConversation(String email, String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        ConversationRole buyerOrSeller = emailBelongsToBuyerOrSeller(conversation, email);
        if (buyerOrSeller == null) {
            return Optional.empty();
        }

        DateTime now = DateTime.now();
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, ConversationCustomValue.AT_POSTFIX, now.toString()));
        markConversation(conversation, new DeletedCustomValue(buyerOrSeller, ConversationCustomValue.DATE_POSTFIX, now.toLocalDate().toString()));

        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);
        searchIndexer.updateSearchSync(Collections.singletonList(conversation));

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        markConversationAsRead(conversationId, postBox);

        return Optional.ofNullable(converter.convertConversation(conversation));
    }

    private Optional<PostBoxSingleConversationThreadResponse> lookupConversation(long numUnread, String email, String conversationId) {
        Conversation conversation = conversationRepository.getById(conversationId);

        // can only happen if both buckets diverge
        if (conversation == null) {
            LOGGER.warn("Inconsistency: Conversation id #{} exists in 'postbox' bucket but not inside 'conversations' bucket", conversationId);
            return Optional.empty();
        }

        Optional<PostBoxSingleConversationThreadResponse> created = PostBoxSingleConversationThreadResponse.create(numUnread, email, conversation);

        if (created.isPresent()) {
            return created;
        } else {
            LOGGER.info("Conversation id #{} is empty but was accessed from list-view, should normally not be reachable by UI", conversationId);
            return Optional.empty();
        }
    }

    private void markConversation(MutableConversation conversation, ConversationCustomValue customValue) {
        conversation.applyCommand(new AddCustomValueCommand(conversation.getId(), customValue.keyName(), customValue.value()));
    }

    private ConversationRole emailBelongsToBuyerOrSeller(Conversation conversation, String email) {
        if (conversation != null) {
            if (conversation.getBuyerId().equalsIgnoreCase(email)) {
                return ConversationRole.Buyer;
            } else if (conversation.getSellerId().equalsIgnoreCase(email)) {
                return ConversationRole.Seller;
            }
        }
        return null;
    }

    private void markConversationAsRead(String conversationId, PostBox<ConversationThread> postBox) {
        for (ConversationThread item : postBox.getConversationThreads()) {
            if (item.getConversationId().equals(conversationId) && item.isContainsUnreadMessages()) {
                postBoxRepository.markConversationAsRead(postBox, item);
                break;
            }
        }
    }

    private String getOtherEmail(ConversationRts conversation, String email) {
        return conversation.getBuyer().equalsIgnoreCase(email)
                ? conversation.getSeller()
                : conversation.getBuyer();
    }

    private String getFlaggedTime(ConversationRts conversation, String email) {
        Map<String, String> conversationHeaders = conversation.getConversationHeaders();
        return conversation.getBuyer().equalsIgnoreCase(email)
                ? conversationHeaders.get("flagged-buyer-at")
                : conversationHeaders.get("flagged-seller-at");
    }
}
