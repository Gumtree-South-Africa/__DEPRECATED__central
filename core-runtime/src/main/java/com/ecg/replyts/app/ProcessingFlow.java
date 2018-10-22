package com.ecg.replyts.app;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.replyts.app.filterchain.FilterChain;
import com.ecg.replyts.app.postprocessorchain.PostProcessorChain;
import com.ecg.replyts.app.preprocessorchain.PreProcessorManager;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.ConversationEventService;
import com.ecg.replyts.core.api.processing.MessageFixer;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.util.ConversationEventConverter;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.ecg.comaas.events.Conversation.Participant;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Represents the mail's flow through various processing stages. Has input methods for all stages (messages can be input
 * everywhere, and they will flow as far as they can get (flow stops at the end, or if it is terminated before)
 */
@Component
class ProcessingFlow {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessingFlow.class);

    private static final String CUST_HEADER_BUYER_NAME = "buyer-name";
    private static final String CUST_HEADER_SELLER_NAME = "seller-name";

    @Autowired
    private PreProcessorManager preProcessor;

    @Autowired(required = false)
    private List<MessageFixer> javaMailMessageFixers = emptyList();

    @Autowired
    private FilterChain filterChain;

    @Autowired
    private PostProcessorChain postProcessor;

    @Autowired
    private MailDeliveryService mailDeliveryService;

    @Autowired
    private ConversationEventService conversationEventService;

    @Autowired
    private UserIdentifierService userIdentifierService;

    @Autowired
    private ContentOverridingPostProcessorService contentOverridingPostProcessorService;

    @Value("${replyts.tenant.short}")
    private String shortTenant;

    private final Timer preProcessorTimer = TimingReports.newTimer("preProcessor");
    private final Timer filterChainTimer = TimingReports.newTimer("filterChain");
    private final Timer postProcessorTimer = TimingReports.newTimer("postProcessor");
    private final Timer sendingTimer = TimingReports.newTimer("sending");
    private final Timer mailFixersTimer = TimingReports.newTimer("send-fail-fixers");
    private final Counter emailDeliverySkippedTimer = TimingReports.newCounter("email-delivery-skipped");

    void inputForPreProcessor(MessageProcessingContext context) {
        try (Timer.Context ignore = preProcessorTimer.time()) {
            LOG.trace("PreProcessing Message {}", context.getMessageId());
            preProcessor.preProcess(context);
        }

        if (!context.isTerminated()) {
            inputForFilterChain(context);
        }
    }

    void inputForFilterChain(MessageProcessingContext context) {
        try (Timer.Context ignore = filterChainTimer.time()) {
            LOG.trace("Filtering Message {}", context.getMessageId());
            filterChain.filter(context);
        }

        if (!context.isTerminated()) {
            inputForPostProcessor(context);
        }
    }

    void inputForPostProcessor(MessageProcessingContext context) {
        try (Timer.Context ignore = postProcessorTimer.time()) {
            LOG.trace("PostProcessing Message {}", context.getMessageId());
            postProcessor.postProcess(context);
        }

        if (context.isTerminated()) {
            throw new IllegalStateException("PostProcessors may not Terminate messages");
        }

        inputForConversationEventsQueue(context);
    }

    void inputForConversationEventsQueue(MessageProcessingContext context) {
        if (!context.isTerminated()) {
            try {
                sendConversationEvents(context);
            } catch (InterruptedException e) {
                LOG.warn("Aborting mail processing flow because thread is interrupted.");
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                LOG.error("failed to submit the conversation into the messaging events queue", e);
                throw e;
            }
        }

        inputForSending(context);
    }

    void sendConversationEvents(MessageProcessingContext context) throws InterruptedException {
        Conversation conversation = context.getConversation();
        if (conversation == null || conversation.getMessages() == null) {
            LOG.warn("conversation.getMessages() == null");
            return;
        }

        if (conversation.getMessages().size() == 1) {
            conversationEventService.sendConversationCreatedEvent(shortTenant, conversation.getAdId(),
                    conversation.getId(), conversation.getCustomValues(), getParticipants(conversation), conversation.getCreatedAt());
        }

        Message message = Iterables.getLast(conversation.getMessages());
        String cleanedMessage = contentOverridingPostProcessorService.getCleanedMessage(conversation, message);
        Optional<String> senderIdOpt = getSenderUserId(conversation, message);

        conversationEventService.sendMessageAddedEvent(shortTenant, conversation.getId(), senderIdOpt, message.getId(), cleanedMessage, message.getHeaders(), context);
    }

    private Optional<String> getSenderUserId(Conversation conversation, Message message) {
        return message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER
                ? userIdentifierService.getBuyerUserId(conversation.getCustomValues())
                : userIdentifierService.getSellerUserId(conversation.getCustomValues());
    }

    public Set<Participant> getParticipants(Conversation conversation) {
        Participant buyer = ConversationEventConverter.createParticipant(
                getBuyerUserId(conversation.getCustomValues(), conversation.getBuyerId()),
                conversation.getCustomValues().get(CUST_HEADER_BUYER_NAME),
                conversation.getBuyerId(),
                Participant.Role.BUYER,
                conversation.getBuyerSecret());
        Participant seller = ConversationEventConverter.createParticipant(
                getSellerUserId(conversation.getCustomValues(), conversation.getSellerId()),
                conversation.getCustomValues().get(CUST_HEADER_SELLER_NAME),
                conversation.getSellerId(),
                Participant.Role.SELLER,
                conversation.getSellerSecret());
        return new HashSet<>(asList(buyer, seller));
    }

    private String getBuyerUserId(Map<String, String> customValues, String buyerId) {
        return userIdentifierService.getBuyerUserId(customValues).orElse(buyerId);
    }

    private String getSellerUserId(Map<String, String> customValues, String sellerId) {
        return userIdentifierService.getSellerUserId(customValues).orElse(sellerId);
    }

    void inputForSending(MessageProcessingContext context) {
        if (context.isSkipDeliveryChannel(MessageProcessingContext.DELIVERY_CHANNEL_MAIL)) {
            emailDeliverySkippedTimer.inc();
            LOG.debug("E-mail delivery switched off for Message {}", context.getMessageId());
            return;
        }

        try {
            doTimedSend(context);
        } catch (MailDeliveryException e) {
            // This can occur when MIME4J was able to parse the incoming email, but JavaMail couldn't parse it on the
            // way out. There's still a chance we could recover by fixing the outgoing email.
            tryMailSendingRecovery(e, context);
        }
    }

    private void tryMailSendingRecovery(MailDeliveryException e, MessageProcessingContext context) {
        LOG.info("Exception on delivery. Trying to recover and retry.");

        try (Timer.Context ignore = mailFixersTimer.time()) {
            context.getOutgoingMail().applyOutgoingMailFixes(javaMailMessageFixers, e);
        }

        try {
            doTimedSend(context);
            LOG.info("Successful recovery.");
        } catch (MailDeliveryException deliveryException) {
            Mail mail = context.getMail().get();
            String msg = String.format("Failed to process mail '%s' from '%s' to '%s'", context.getMessageId(), mail.getFrom(), mail.getDeliveredTo());
            throw new RuntimeException(msg, deliveryException);
        }
    }

    private void doTimedSend(MessageProcessingContext context) throws MailDeliveryException {
        try (Timer.Context ignore = sendingTimer.time()) {
            LOG.trace("Sending Message {}", context.getMessageId());
            mailDeliveryService.deliverMail(context.getOutgoingMail());
        }
    }
}
