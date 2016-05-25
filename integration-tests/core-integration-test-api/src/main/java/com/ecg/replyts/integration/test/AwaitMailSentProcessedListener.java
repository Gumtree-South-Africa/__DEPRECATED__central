package com.ecg.replyts.integration.test;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Listener that gets informed whenever a mail has been processed by replyts and gives integration tests access to the
 * list of mails, ReplyTS has processed.
 */
public class AwaitMailSentProcessedListener implements MessageProcessedListener {

    private static final Logger LOG = LoggerFactory.getLogger(AwaitMailSentProcessedListener.class);

    @Autowired
    private MailRepository mailRepository;

    private static final ConcurrentHashMap<String, ProcessedMail> RECEIVED_MAILS = new ConcurrentHashMap<>();

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
    }

    private static final LinkedBlockingQueue<ProcessedMail> OUTCOME = new LinkedBlockingQueue<>();

    /**
     * waits for replyts to have finished the next mail.
     */
    public static ProcessedMail awaitMail() {
        OUTCOME.clear();
        try {
            ProcessedMail result = OUTCOME.poll(2, TimeUnit.SECONDS);
            if (result == null) {
                throw new RuntimeException("Timeout awaiting processed mail");
            }
            return result;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProcessedMail awaitMailIdentifiedBy(String uniqueIdentifier) {
        return awaitMailIdentifiedBy(uniqueIdentifier, 5);
    }

    /**
     * Blocks a reasonable time amount to see if a mail with a unique identifier value (see MailBuilder) has been received.
     */
    public static ProcessedMail awaitMailIdentifiedBy(String uniqueIdentifier, int deliveryTimeoutSeconds) {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(deliveryTimeoutSeconds);
        while (System.currentTimeMillis() < end) {
            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            ProcessedMail identifiedMail = RECEIVED_MAILS.get(uniqueIdentifier);
            if (identifiedMail != null) {
                LOG.info("Message with unique identifier '{}' downloaded at {}", uniqueIdentifier, DateTime.now());
                return identifiedMail;
            }

        }
        throw new RuntimeException("Timeout awaiting processed mail with unique identifier '" + uniqueIdentifier + "' at " + DateTime.now() + ". Mails in inbox: " + RECEIVED_MAILS.keySet());

    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        LOG.info("Processing message with id '{}'");
        Mail m = null;
        try {
            if (message.getState() == MessageState.SENT) {
                m = StructuredMail.parseMail(new ByteArrayInputStream(mailRepository.readOutboundMail(message.getId())));
            }
            OUTCOME.add(new ProcessedMail(message, m, conversation));
            String key = message.getHeaders().get(MailBuilder.UNIQUE_IDENTIFIER_HEADER);
            if (key != null) {
                RECEIVED_MAILS.put(key, new ProcessedMail(message, m, conversation));
                LOG.info("Message with unique identifier '{}' arrived at {}. Mails in Inbox: {}", key, DateTime.now(), RECEIVED_MAILS.keySet());
            }
        } catch (Exception e) {
            LOG.error("FAILURE AT DELIVERY", e);
            throw new RuntimeException(e);
        }

    }
}
