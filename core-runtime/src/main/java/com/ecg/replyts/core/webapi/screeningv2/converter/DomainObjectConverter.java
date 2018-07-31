package com.ecg.replyts.core.webapi.screeningv2.converter;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse.IDHolder;
import com.ecg.replyts.core.api.webapi.model.ConversationRts;
import com.ecg.replyts.core.api.webapi.model.ConversationRtsStatus;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.MessageRtsDirection;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.api.webapi.model.ProcessingFeedbackRts;
import com.ecg.replyts.core.api.webapi.model.imp.ConversationRtsRest;
import com.ecg.replyts.core.api.webapi.model.imp.MessageRtsRest;
import com.ecg.replyts.core.api.webapi.model.imp.ProcessingFeedbackRtsRest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DomainObjectConverter {
    private final ConversationRepository conversationRepository;
    private final MailCloakingService mailCloakingService;

    private static final Logger LOG = LoggerFactory.getLogger(DomainObjectConverter.class);

    @Autowired
    public DomainObjectConverter(ConversationRepository conversationRepository, MailCloakingService mailCloakingService) {
        this.conversationRepository = conversationRepository;
        this.mailCloakingService = mailCloakingService;
    }

    /**
     * Converts a conversation from the repository into an object to return via the API.
     *
     * @param conversation - object from repository
     * @return API conversation object with all its messages
     */
    public ConversationRts convertConversation(Conversation conversation) {
        return convertConversationInternal(conversation, true);
    }

    /**
     * Converts a list of <MessageId,ConversationId> pairs into a list of message API objects.
     *
     * @param messageResultIds - list of <MessageId,ConversationId>
     * @return API message objects, complete with their processing feedbacks and link back to parent conversation
     */
    public List<MessageRts> convertFromSearchResults(List<IDHolder> messageResultIds) {
        List<MessageRts> messageResults = new ArrayList<>();

        for (RtsSearchResponse.IDHolder messageResultId : messageResultIds) {
            try {
                Conversation conversation = conversationRepository.getById(messageResultId.getConversationId());

                if (conversation == null) {
                    LOG.warn("skipping search result in API response- conversation not found: conversation={}, message={}",
                            messageResultId.getConversationId(), messageResultId.getMessageId());
                    continue;
                }

                Message message = conversation.getMessageById(messageResultId.getMessageId());
                MessageRts messageRts = convertMessage(message, conversation, true);
                messageResults.add(messageRts);
            } catch (RuntimeException e) {
                LOG.warn("Could not convert conversation=" + messageResultId.getConversationId() + ", message=" + messageResultId.getMessageId() + " to api response", e);
            }
        }

        return messageResults;
    }

    private ConversationRts convertConversationInternal(Conversation conversation, boolean withMessages) {
        if (conversation == null) return null;

        ConversationRtsRest conversationRts = new ConversationRtsRest();

        conversationRts.setId(conversation.getId());
        conversationRts.setAdId(conversation.getAdId());
        conversationRts.setBuyer(conversation.getBuyerId());
        conversationRts.setBuyerAnonymousEmail(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation).getAddress());
        conversationRts.setSeller(conversation.getSellerId());
        conversationRts.setSellerAnonymousEmail(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation).getAddress());
        conversationRts.setCreationDate(conversation.getCreatedAt().toDate());
        conversationRts.setLastUpdateDate(conversation.getLastModifiedAt().toDate());
        conversationRts.setConversationHeaders(conversation.getCustomValues());
        conversationRts.setStatus(convertEnum(conversation.getState(), ConversationRtsStatus.values(), ConversationRtsStatus.OTHER));

        if (withMessages) {
            List<MessageRts> messages = new ArrayList<>();

            for (Message message : conversation.getMessages()) {
                messages.add(convertMessage(message, conversation, false));
            }

            conversationRts.setMessages(messages);
        }

        return conversationRts;
    }

    private MessageRts convertMessage(Message message, Conversation conversation, boolean withBasicConversationFields) {
        MessageRtsRest messageRts = new MessageRtsRest();

        messageRts.setAttachments(message.getAttachmentFilenames());
        messageRts.setId(message.getId());
        messageRts.setMailHeaders(message.getHeaders());
        messageRts.setText(message.getPlainTextBody());
        messageRts.setLastEditor(message.getLastEditor().orElse(null));

        if (withBasicConversationFields) {
            ConversationRts conversationRts = convertConversationInternal(conversation, false);
            messageRts.setConversation(conversationRts);
        }

        messageRts.setReceivedDate(message.getReceivedAt().toDate());

        messageRts.setState(convertEnum(message.getState(), MessageRtsState.values(), MessageRtsState.OTHER));
        messageRts.setFilterResultState(message.getFilterResultState());
        messageRts.setHumanResultState(message.getHumanResultState());

        switch (message.getMessageDirection()) {
            case BUYER_TO_SELLER:
                messageRts.setMessageDirection(MessageRtsDirection.BUYER_TO_SELLER);
                break;
            case SELLER_TO_BUYER:
                messageRts.setMessageDirection(MessageRtsDirection.SELLER_TO_BUYER);
                break;
            default:
                messageRts.setMessageDirection(MessageRtsDirection.UNKNOWN);
                break;
        }

        List<ProcessingFeedbackRts> processingFeedbackList = new ArrayList<>();

        for (ProcessingFeedback feedback : message.getProcessingFeedback()) {
            ProcessingFeedbackRtsRest processingFeedbackRts = new ProcessingFeedbackRtsRest();
            processingFeedbackRts.setDescription(feedback.getDescription());
            processingFeedbackRts.setEvaluation(feedback.isEvaluation());
            processingFeedbackRts.setFilterInstance(feedback.getFilterInstance());
            processingFeedbackRts.setFilterName(feedback.getFilterName());
            processingFeedbackRts.setScore(feedback.getScore());
            processingFeedbackRts.setState(feedback.getResultState());
            processingFeedbackRts.setUiHint(feedback.getUiHint());

            processingFeedbackList.add(processingFeedbackRts);
        }

        messageRts.setProcessingFeedback(processingFeedbackList);

        return messageRts;
    }

    private <T extends Enum<?>> T convertEnum(Enum<?> source, T[] targetValues, T defaultTarget) {
        if (source == null) {
            return defaultTarget;
        }
        String n = source.name();
        for (T t : targetValues) {
            if (t.name().equals(n)) {
                return t;
            }
        }
        return defaultTarget;
    }

}
