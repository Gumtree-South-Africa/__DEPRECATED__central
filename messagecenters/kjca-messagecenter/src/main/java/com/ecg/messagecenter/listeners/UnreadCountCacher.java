package com.ecg.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.messagecenter.persistence.simple.AbstractSimplePostBoxInitializer;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.replyts.core.runtime.TimingReports;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * Preemptively populates a cache in Box of the user's unread conversation counts.
 */
@Component
public class UnreadCountCacher implements AbstractSimplePostBoxInitializer.PostBoxWriteCallback {
    private static final Logger LOG = LoggerFactory.getLogger(UnreadCountCacher.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Timer TIMER = TimingReports.newTimer("unread-count-cacher");
    private static final Counter EXCEPTION_COUNTER = TimingReports.newCounter("unread-count-cacher.exceptions");

    private final JmsTemplate jmsTemplate;
    private final SimplePostBoxRepository postBoxRepository;
    private final String unreadCountCacheQueueName;

    @Autowired
    public UnreadCountCacher(final SimplePostBoxRepository postBoxRepository,
                             @Qualifier("messageCentreJmsTemplate") final JmsTemplate jmsTemplate,
                             @Value("${unread.count.cache.queue}") final String unreadCountCacheQueueName) {
        this.jmsTemplate = jmsTemplate;
        this.postBoxRepository = postBoxRepository;
        this.unreadCountCacheQueueName = unreadCountCacheQueueName;
    }

    @Override
    public void success(String email, Long unreadCount, boolean markedAsUnread) {
        try (Timer.Context unused = TIMER.time()) {
            final PostBox<AbstractConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            int total = 0;
            int asPoster = 0;
            int asReplier = 0;

            for (AbstractConversationThread thread : postBox.getConversationThreads()) {
                if (!thread.isContainsUnreadMessages()) {
                    continue;
                }

                if (email.equals(thread.getBuyerId().orElse(""))) {
                    asReplier++;
                } else {
                    asPoster++;
                }
                total++;
            }

            final UnreadConversationCount unreadConversationCount = new UnreadConversationCount(email, total, asPoster, asReplier);

            this.jmsTemplate.convertAndSend(this.unreadCountCacheQueueName, MAPPER.writer().writeValueAsString(unreadConversationCount));
        } catch (JmsException | JsonProcessingException e) {
            EXCEPTION_COUNTER.inc();
            LOG.error("Encountered exception while sending unread counts to external cache.", e);
        }
    }

    @JsonIgnoreProperties
    private static class UnreadConversationCount {
        @JsonProperty
        final String email;

        @JsonProperty
        final int total;

        @JsonProperty
        final int asPoster;

        @JsonProperty
        final int asReplier;

        UnreadConversationCount(String email, int total, int asPoster, int asReplier) {
            this.email = email;
            this.total = total;
            this.asPoster = asPoster;
            this.asReplier = asReplier;
        }
    }
}
