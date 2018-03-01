package com.ebay.ecg.replyts.robot.service;

import com.ebay.ecg.replyts.robot.api.requests.payload.GetConversationsResponsePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ecg.replyts.app.ConversationEventListeners;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RobotService {

    private static final Logger LOG = LoggerFactory.getLogger(RobotService.class);

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired
    private ModerationService moderationService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ConversationEventListeners conversationEventListeners;

    @Autowired(required = false)
    private MailPublisher mailPublisher;

    @Autowired(required = false)
    private HeldMailRepository heldMailRepository;

    private static SearchMessagePayload allMessagesOfAdByRole(String email, String adId, SearchMessagePayload.ConcernedUserRole role) {
        SearchMessagePayload smp = new SearchMessagePayload();
        smp.setMessageState(MessageRtsState.SENT);
        smp.setAdId(adId);
        smp.setUserEmail(email);
        smp.setUserRole(role);
        return smp;
    }

    public GetConversationsResponsePayload getConversationsForAd(String email, String adId) {
        Set<String> buyerUniqueConvIds = getUniqueConversationIdsForAd(email, adId, SearchMessagePayload.ConcernedUserRole.RECEIVER);
        Set<String> sellerUniqueConvIds = getUniqueConversationIdsForAd(email, adId, SearchMessagePayload.ConcernedUserRole.SENDER);
        buyerUniqueConvIds.removeAll(sellerUniqueConvIds);

        return new GetConversationsResponsePayload(adId, buyerUniqueConvIds);
    }

    private Set<String> getUniqueConversationIdsForAd(String email, String adId, SearchMessagePayload.ConcernedUserRole role) {
        return searchService.search(allMessagesOfAdByRole(email, adId, role)).getResult().stream()
                .map(RtsSearchResponse.IDHolder::getConversationId)
                .collect(Collectors.toSet());
    }

    public void addMessageToConversation(String conversationId, MessagePayload payload) {
        LOG.debug("Gumbot Message received for ConversationID: {}", conversationId);

        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            LOG.warn("Conversation with id {} was not found", conversationId);
            return;
        }

        if (ConversationState.ACTIVE != conversation.getState()) {
            LOG.warn("Conversation with id {} has state {}, only in {} would be processed",
                    conversationId, conversation.getState(), ConversationState.ACTIVE);
            return;
        }

        updateConversation(conversation, payload);
    }

    private void updateConversation(MutableConversation conversation, MessagePayload payload) {
        final String messageId = Guids.next();
        LOG.debug("Begin adding Message ID: {} to conversation {}", messageId, conversation.getId());

        conversation.applyCommand(ContentUtils.buildAddMessage(conversation, payload, messageId));
        ((DefaultMutableConversation) conversation).commit(conversationRepository, conversationEventListeners);

        writeIfInitialized(messageId, conversation, payload);

        LOG.debug("Done persisting message {}.", messageId);

        try {
            moderationService.changeMessageState(conversation, messageId, new ModerationAction(ModerationResultState.GOOD, Optional.empty()));
            LOG.debug("Done changing message state of {} to GOOD", messageId);
        } catch (MessageNotFoundException e) {
            LOG.warn("Message state with id  {} was not changed as message was not found", messageId, e);
        }
    }

    private void writeIfInitialized(String messageId, MutableConversation conversation, MessagePayload payload) {
        if (mailPublisher != null || heldMailRepository != null) {
            Optional<Mail> mailOptional = ContentUtils.buildMail(conversation, payload);
            if (mailPublisher != null && mailOptional.isPresent()) {
                mailPublisher.publishMail(messageId, Mails.writeToBuffer(mailOptional.get()), Optional.empty());
            }
            if (heldMailRepository != null && mailOptional.isPresent()) {
                heldMailRepository.write(messageId, Mails.writeToBuffer(mailOptional.get()));
            }
        }
    }
}
