package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.integration.smtp.CapturingMailDeliveryService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Listener that gets informed whenever a mail has been processed by replyts and gives integration tests access to the
 * list of mails, ReplyTS has processed.
 */
public class AwaitMailSentProcessedListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(AwaitMailSentProcessedListener.class);

    @Autowired
    private CapturingMailDeliveryService deliveryService;

    private static final Map<String, ProcessedMail> RECEIVED_MAILS_MAP = new ConcurrentHashMap<>();

    private static final List<ProcessedMail> RECEIVED_MAILS = Collections.synchronizedList(newArrayList());

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


    /**
     * waits for replyts to have finished processing at least #count mails.
     */
    public static void awaitMails(int count, int deliveryTimeoutSeconds) throws InterruptedException {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(deliveryTimeoutSeconds);
        int size = 0;
        while (System.currentTimeMillis() < end) {
            TimeUnit.MILLISECONDS.sleep(50);
            size = RECEIVED_MAILS.size();
            if (size >= count) {
                LOG.info("Listener received {} mails", size);
                return;
            }
        }
        LOG.debug("Listener didn't receive enough mails: {}", size);
        throw new RuntimeException("Timeout awaiting at least 1 mail");
    }

    /**
     * Blocks a reasonable time amount to see if a mail with a unique identifier value (see MailBuilder) has been received.
     */
    public static ProcessedMail awaitMailIdentifiedBy(String uniqueIdentifier, int deliveryTimeoutSeconds) {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(deliveryTimeoutSeconds);
        while (System.currentTimeMillis() < end) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProcessedMail identifiedMail = RECEIVED_MAILS_MAP.get(uniqueIdentifier);
            if (identifiedMail != null) {
                LOG.info("Message with unique identifier '{}' downloaded at {}", uniqueIdentifier, DateTime.now());
                return identifiedMail;
            }

        }
        throw new RuntimeException("Timeout awaiting processed mail with unique identifier '" + uniqueIdentifier + "' at " + DateTime.now() + ". Mails in inbox: " + RECEIVED_MAILS_MAP.keySet());

    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        LOG.info("Processing message with id '{}'", message.getId());
        Mail m = null;
        try {
            if (message.getState() == MessageState.SENT) {
                m = deliveryService.getLastSentMail();
            }
            RECEIVED_MAILS.add(new ProcessedMail(message, m, conversation));
            String key = message.getHeaders().get(MailBuilder.UNIQUE_IDENTIFIER_HEADER);
            if (key != null) {
                RECEIVED_MAILS_MAP.put(key, new ProcessedMail(message, m, conversation));
                LOG.info("Message with unique identifier '{}' arrived at {}. Mails in Inbox: {}", key, DateTime.now(), RECEIVED_MAILS_MAP.keySet());
            }
        } catch (Exception e) {
            LOG.error("FAILURE AT DELIVERY", e);
            throw new RuntimeException(e);
        }
    }
}
