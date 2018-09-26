package com.ecg.replyts.app.mailreceiver.kafka;

import com.ecg.comaas.protobuf.MessageOuterClass.Message;
import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.app.ProcessingContextFactory;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.processing.Attachment;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.ecg.replyts.core.runtime.persistence.kafka.KafkaTopicService;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import scala.Int;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.api.model.conversation.command.AddMessageCommandBuilder.anAddMessageCommand;

/**
 * docker-compose --project-name comaasdocker exec kafka bash
 * /opt/kafka/bin/kafka-topics.sh --list --zookeeper zookeeper:2181
 * /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic mp_messages_retry --from-beginning
 * /opt/kafka/bin/kafka-topics.sh --alter --zookeeper zookeeper:2181 --topic mp_messages --partitions 10
 * <p>
 * https://github.corp.ebay.com/ecg-comaas/kafka-message-producer
 * docker run --rm --net=host dock.es.ecg.tools/comaas/kafka-message-producer:0.0.1 --tenant mp --delay 100 10
 */

public class KafkaNewMessageProcessor extends KafkaMessageProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaNewMessageProcessor.class);
    private static final String X_MESSAGE_ID_HEADER = "X-Message-ID";

    private final int maxRetries;
    private final MessageProcessingCoordinator messageProcessingCoordinator;
    private final MutableConversationRepository conversationRepository;
    private final ProcessingContextFactory processingContextFactory;
    private final UserIdentifierService userIdentifierService;

    private final long messageProcessingTimeoutMs, messageTaskCancellationTimeoutMs;

    private ExecutorService executor = createMessageProcessingExecutor();

    KafkaNewMessageProcessor(MessageProcessingCoordinator messageProcessingCoordinator, QueueService queueService,
                             KafkaMessageConsumerFactory kafkaMessageConsumerFactory, int retryOnFailedMessagePeriodMinutes,
                             int maxRetries, String shortTenant,
                             MutableConversationRepository conversationRepository,
                             ProcessingContextFactory processingContextFactory, UserIdentifierService userIdentifierService,
                             long messageProcessingTimeoutMs, long messageTaskCancellationTimeoutMs) {
        super(queueService, kafkaMessageConsumerFactory, retryOnFailedMessagePeriodMinutes, shortTenant);
        this.maxRetries = maxRetries;
        this.messageProcessingCoordinator = messageProcessingCoordinator;
        this.conversationRepository = conversationRepository;
        this.processingContextFactory = processingContextFactory;
        this.userIdentifierService = userIdentifierService;
        this.messageProcessingTimeoutMs = messageProcessingTimeoutMs;
        this.messageTaskCancellationTimeoutMs = messageTaskCancellationTimeoutMs;

        LOG.info("messageProcessingTimeoutMs = {}, messageTaskCancellationTimeoutMs= {}, maxPollIntervalMs={}", messageProcessingTimeoutMs, messageTaskCancellationTimeoutMs, kafkaMessageConsumerFactory.getMaxPollIntervalMs());
        Assert.isTrue(messageProcessingTimeoutMs + messageTaskCancellationTimeoutMs < kafkaMessageConsumerFactory.getMaxPollIntervalMs(),
                "Sum of message processing timeout + message cancellation timeout should be less that kafka max poll timeout");
    }

    private ExecutorService createMessageProcessingExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    @Override
    protected void processMessage(Message message) throws InterruptedException {
        LOG.debug("Found a message in the incoming topic {}, tried so far: {} times", message.getCorrelationId(), message.getRetryCount());

        // note that we are not dealing with concurrency here -- not for the AtomicBoolean, and not for the
        // executor. It's only a way to run a separate thread, that might hang indefinitely -- and if it does,
        // we want to propagate an interrupt up the stack, to signal that some thread is stuck.

        // Because we're not sure if/how the interrupt flag can be read from outside the executor.
        AtomicBoolean interrupted = new AtomicBoolean();
        Future<?> task = executor.submit(MDCConstants.setTaskFields(() -> {
            try {
                try {
                    // chooseStrategyAndProcess seems not interruptible -- or it swallows interruptedException.
                    // But it must be doing IO somewhere down the line?
                    chooseStrategyAndProcess(message);
                } catch (ParsingException e) {
                    unparseableMessage(message);
                } catch (IOException e) {
                    retryOrAbandon(message, e);
                }
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                interrupted.set(true);
            }

        }, Thread.currentThread().getName() + "-tns"));

        try {
            task.get(messageProcessingTimeoutMs, TimeUnit.MILLISECONDS);

            if (interrupted.get()){
                Thread.currentThread().interrupt();
                throw new InterruptedException();
            }

            LOG.debug("Processed message with correlation id {} successfully", message.getCorrelationId());
        } catch (TimeoutException e) {
            LOG.info("Message processing time exceeded {} seconds. Trying to stop the thread", messageProcessingTimeoutMs);
            try {
                executor.shutdownNow();
                if (executorRespectsInterrupt()) {
                    // if the Below call is interrupted, the HangingThreadException is thrown, after which the
                    // kafka offset is committed. That's NOT good, because we wanted to retry the message.
                    retryOrAbandon(message, new RuntimeException("Message processing timed out"));
                } else {
                    abandonMessage(message, new RuntimeException("Shutting down comaas since we could not stop message processing in time"));
                    throw new HangingThreadException();
                }
            } catch (InterruptedException e1) {
                throw new HangingThreadException();
            } finally {
                //create new executor for next message
                executor = createMessageProcessingExecutor();
            }
        } catch (ExecutionException e) {
            retryOrAbandon(message, e);
        }
    }

    /**
     * @return true iff the executor shut down in timely manner
     */
    private boolean executorRespectsInterrupt() throws InterruptedException {
        return executor.awaitTermination(messageTaskCancellationTimeoutMs, TimeUnit.MILLISECONDS);
    }

    private void retryOrAbandon(Message message, Exception e) throws InterruptedException {
        if (message.getRetryCount() >= maxRetries) {
            abandonMessage(message, e);
        } else {
            delayRetryMessage(message);
        }
    }

    private void chooseStrategyAndProcess(Message kafkaMessage) throws IOException, ParsingException {
        ByteString rawEmail = kafkaMessage.getRawEmail();
        if (rawEmail != null && !rawEmail.isEmpty()) {
            // A presence of a raw email represents the deprecated way of ingesting emails
            // See https://github.corp.ebay.com/ecg-comaas/comaas-adr/blob/master/adr-004-kmail-transition-period.md
            messageProcessingCoordinator.accept(getMessageId(kafkaMessage), new ByteArrayInputStream(rawEmail.toByteArray()));
        } else {
            MutableConversation conversation = getConversation(kafkaMessage.getPayload().getConversationId());
            Collection<Attachment> attachments = kafkaMessage.getAttachmentsList().stream()
                    .map(a -> new Attachment(a.getFileName(), a.getBody().toByteArray()))
                    .collect(Collectors.toSet());
            MessageProcessingContext context = createContext(kafkaMessage.getPayload().getUserId(), kafkaMessage.getMessageId(), conversation, attachments);
            context.addCommand(
                    createAddMessageCommand(kafkaMessage.getPayload().getMessage(), conversation.getId(), context,
                            kafkaMessage.getMetadataMap()));

            messageProcessingCoordinator.handleContext(Optional.empty(), context);
        }
    }

    private String getMessageId(Message kafkaMessage) {
        // This is undocumented behaviour which is set to be removed in COMAAS-1226
        // At the moment we can't ignore X-Message-Id entirely and just write/read the messageId field of the
        // kafka payload, because the emails originating from MP (the tenant itself) rely on this header to
        // render the chat ui correctly. So we can only get rid of the edge case when MP is fully on the post message api.
        // (sorry).
        Map<String, String> caseInsensitiveMetaValues = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveMetaValues.putAll(kafkaMessage.getMetadataMap());
        String messageIdFromHeader = caseInsensitiveMetaValues.get(X_MESSAGE_ID_HEADER);
        return messageIdFromHeader != null ? messageIdFromHeader : kafkaMessage.getMessageId();
    }

    private MutableConversation getConversation(String conversationId) {
        MutableConversation conversation = conversationRepository.getById(conversationId);
        if (conversation == null) {
            throw new IllegalStateException(String.format("Conversation not found for ID: %s", conversationId));
        }
        return conversation;
    }

    private MessageProcessingContext createContext(String userId, String messageId, MutableConversation conversation,
                                                   Collection<Attachment> attachments) {
        MessageProcessingContext context = processingContextFactory.newContext(messageId, attachments);
        context.setConversation(conversation);
        String sellerId = userIdentifierService.getSellerUserId(conversation)
                .orElseThrow(() -> new IllegalArgumentException("Failed to infer a seller id"));
        String buyerId = userIdentifierService.getBuyerUserId(conversation)
                .orElseThrow(() -> new IllegalArgumentException("Failed to infer a buyer id"));
        if (userId.equals(sellerId)) {
            context.setMessageDirection(MessageDirection.SELLER_TO_BUYER);
        } else if (userId.equals(buyerId)) {
            context.setMessageDirection(MessageDirection.BUYER_TO_SELLER);
        } else {
            throw new IllegalArgumentException("User is not a participant");
        }
        return context;
    }

    private AddMessageCommand createAddMessageCommand(String message, String conversationId,
                                                      MessageProcessingContext context, Map<String, String> metadata) {
        return anAddMessageCommand(conversationId, context.getMessageId())
                .withMessageDirection(context.getMessageDirection())
                .withSenderMessageIdHeader(context.getMessageId())
                //.withInResponseToMessageId() TODO akobiakov: find out what this is used for
                .withTextParts(Collections.singletonList(message))
                .withAttachmentFilenames(context.getAttachments().stream().map(Attachment::getName).collect(Collectors.toList()))
                .withHeaders(metadata)
                .build();
    }

    @Override
    protected String getTopicName() {
        return KafkaTopicService.getTopicIncoming(shortTenant);
    }
}
