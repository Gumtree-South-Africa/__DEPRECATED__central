package com.ecg.replyts.integration.test;

import com.ecg.replyts.app.MessageProcessingCoordinator;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.jayway.awaitility.core.ConditionTimeoutException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Hooks into different aspects of the system to intercept messages on different stages of their lifecycle.
 * <b>Note</b>: the expected use case is a single interceptor per a test case. The interceptor must not
 * hold any shared state.
 */
public class MailInterceptor implements BeanPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(MailInterceptor.class);

    public static class ProcessedMail {
        private final Mail outboundMail;
        private final Conversation c;
        private final Message m;


        public ProcessedMail(Message m, Mail outboundMail, Conversation c) {
            this.m = m;
            this.outboundMail = outboundMail;
            this.c = c;
        }

        public Conversation getConversation() {
            return c;
        }

        public Mail getOutboundMail() {
            return outboundMail;
        }

        public Message getMessage() {
            return m;
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
        }
    }


    private final AtomicInteger processedMessagesCount = new AtomicInteger();
    private final AtomicReference<Mail> mostRecentSentMail = new AtomicReference<>();

    // note that emails in this list may be not yet processed
    private final Map<String, ProcessedMail> interceptedMails = Collections.synchronizedMap(new LinkedHashMap<>());
    private final Map<String, ProcessedMail> uniqueKeyToProcessedEmail = Collections.synchronizedMap(new HashMap<>());

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MessageProcessingCoordinator) {
            return wrapMessageProcessingCoordinator((MessageProcessingCoordinator) bean);
        } else if (bean instanceof MailDeliveryService) {
            return wrapDeliveryServiceIfNecessary((MailDeliveryService) bean);
        } else {
            return bean;
        }
    }

    private MailDeliveryService wrapDeliveryServiceIfNecessary(MailDeliveryService realDeliveryService) {
        return m -> {
            realDeliveryService.deliverMail(m);
            LOG.info("A mail has been intercepted with messageId={}", m.getMessageId());
            mostRecentSentMail.set(m);
        };
    }

    @Bean
    public MessageProcessedListener interceptingListener() {
        return (conversation, message) -> {
            LOG.info("Intercepted conversationId={}, messageId={}, will notify as soon as it's processed", conversation.getId(),
                    message.getId());
            ProcessedMail previousMessage = interceptedMails.put(message.getId(), new ProcessedMail(message,
                    message.getState() == MessageState.SENT ? mostRecentSentMail.get() : null,
                    conversation));
            if (previousMessage != null) {
                LOG.warn("Two messages with the same messageId={} were intercepted, old={}, new={}", message.getId(),
                        previousMessage, message);
            }
        };
    }

    private MessageProcessingCoordinator wrapMessageProcessingCoordinator(MessageProcessingCoordinator realCoordinator) {
        return new MessageProcessingCoordinator() {
            @Override
            public Optional<String> accept(InputStream input) throws IOException, ParsingException {
                Optional<String> messageId = realCoordinator.accept(input);
                messageId.ifPresent(this::markMessageAsProcessed);
                return messageId;
            }

            @Override
            public String handleContext(Optional<byte[]> bytes, MessageProcessingContext context) {
                return realCoordinator.handleContext(bytes, context);
            }

            private void markMessageAsProcessed(String messageId) {
                int newCount = processedMessagesCount.incrementAndGet();
                LOG.info("Number of messages processed so far: {}", newCount);
                notifyMessageProcessed(messageId);
            }
        };
    }

    private void notifyMessageProcessed(String messageId) {
        ProcessedMail mail = interceptedMails.get(messageId);
        if (mail == null) {
            LOG.warn("No mail with messageId={} was found", messageId);
        } else {
            String uniqueMessageKey = mail.getMessage().getHeaders()
                    .get(MailBuilder.UNIQUE_IDENTIFIER_HEADER);
            LOG.info("Marking uniqueKey={} as processed", uniqueMessageKey);
            uniqueKeyToProcessedEmail.put(uniqueMessageKey, mail);
        }
    }

    /**
     * Blocks a reasonable time amount to see if a mail with a unique identifier value (see MailBuilder) has been received.
     */
    ProcessedMail awaitMailIdentifiedBy(String uniqueMessageKey, int deliveryTimeoutSeconds) {
        LOG.info("Await for uniqueMessageKey={} for at most {} seconds", uniqueMessageKey, deliveryTimeoutSeconds);
        try {
            return await().atMost(deliveryTimeoutSeconds, TimeUnit.SECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> uniqueKeyToProcessedEmail.get(uniqueMessageKey), notNullValue());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException("Timeout awaiting processed mail with unique identifier '" + uniqueMessageKey +
                    "' at " + DateTime.now() + ". Mails in inbox: " + interceptedMails);
        }
    }

    /**
     * Waits for replyts to have finished processing at least #count mails.
     */
    public void awaitMails(int count, int deliveryTimeoutSeconds) {
        try {
            await().atMost(deliveryTimeoutSeconds, TimeUnit.SECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> processedMessagesCount.get() >= count);
            LOG.info("Listener received {} mails", processedMessagesCount.get());
        } catch (ConditionTimeoutException e) {
            throw new RuntimeException("Timeout awaiting " + count + " processed mails ' at "
                    + DateTime.now() + ". Mails in inbox: " + interceptedMails);
        }
    }
}
