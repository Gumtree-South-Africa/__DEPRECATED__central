package com.ecg.messagecenter.kjca.persistence;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.core.persistence.AbstractConversationThread;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.listeners.UnreadCountCacher;
import com.ecg.replyts.core.runtime.TimingReports;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
public class UnreadCountCachePopulater {
    private static final Logger LOG = LoggerFactory.getLogger(UnreadCountCacher.class);
    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer();
    private static final Timer TIMER = TimingReports.newTimer("unread-count-cacher");
    private static final Counter EXCEPTION_COUNTER = TimingReports.newCounter("unread-count-cacher.exceptions");

    private SimplePostBoxRepository postBoxRepository;
    private JmsTemplate jmsTemplate;
    private String unreadCountCacheQueueName;

    @Autowired
    public UnreadCountCachePopulater(final SimplePostBoxRepository postBoxRepository,
                                     @Qualifier("messageCentreJmsTemplate") final JmsTemplate jmsTemplate,
                                     @Value("${unread.count.cache.queue}") final String unreadCountCacheQueueName) {
        this.postBoxRepository = postBoxRepository;
        this.jmsTemplate = jmsTemplate;
        this.unreadCountCacheQueueName = unreadCountCacheQueueName;
    }

    public void populateCache(String email) {
        final PostBox<AbstractConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        populateCache(postBox);
    }

    public void populateCache(PostBox<AbstractConversationThread> postBox) {
        try (Timer.Context unused = TIMER.time()) {
            int total = 0;
            int asPoster = 0;
            int asReplier = 0;

            for (AbstractConversationThread thread : postBox.getConversationThreads()) {
                if (!thread.isContainsUnreadMessages()) {
                    continue;
                }

                if (postBox.getEmail().equals(thread.getBuyerId().orElse(""))) {
                    asReplier++;
                } else {
                    asPoster++;
                }
                total++;
            }

            final UnreadConversationCount unreadConversationCount = new UnreadConversationCount(postBox.getEmail(), total, asPoster, asReplier);

            this.jmsTemplate.convertAndSend(this.unreadCountCacheQueueName, OBJECT_WRITER.writeValueAsString(unreadConversationCount));
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
