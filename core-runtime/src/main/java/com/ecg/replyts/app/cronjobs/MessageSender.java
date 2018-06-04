package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse.IDHolder;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MessageSender {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSender.class);

    private final ApplicationContext applicationContext;
    private final Document2KafkaSink document2KafkaSink;

    private final ConversationRepository conversationRepository;
    private final SearchService searchService;

    private final int retentionTimeHours;
    private final int retentionTimeStartHours;
    private final int processingMaximum;

    @Autowired
    public MessageSender(ApplicationContext applicationContext,
                         SearchService searchService,
                         Document2KafkaSink document2KafkaSink,
                         ConversationRepository conversationRepository,
                         @Value("${cronjob.sendHeld.retentionTimeHours:12}") int retentionTimeHours,
                         // 24 hours is the default to guard against stuck mails
                         @Value("${cronjob.sendHeld.retentionTimeStartHours:24}") int retentionTimeStartHours,
                         @Value("${cronjob.sendHeld.processingMaximum:20000}") int processingMaximum) {
        this.applicationContext = applicationContext;
        this.conversationRepository = conversationRepository;
        this.document2KafkaSink = document2KafkaSink;
        this.retentionTimeHours = retentionTimeHours;
        this.searchService = searchService;
        this.retentionTimeStartHours = retentionTimeStartHours;
        this.processingMaximum = processingMaximum;
    }

    public void work() {
        // moderation service is in a feedback loop to the filter chain that needs the plugin system to work
        // this cronjob is a plugin --> we've got a circular dependency at startup here
        // I resolve this by fetching the moderation service on the cron job run individually.
        ModerationService moderationService = applicationContext.getBean(ModerationService.class);

        DateTime endOfRatentionTime = DateTime.now().minusHours(retentionTimeHours);
        SearchMessagePayload smp = new SearchMessagePayload();
        smp.setMessageState(MessageRtsState.HELD);
        smp.setFromDate(endOfRatentionTime.minusHours(retentionTimeStartHours).toDate());
        // All mails received before (now() - retentionTimeHours) which are still in held can be sent out.
        smp.setToDate(endOfRatentionTime.toDate());
        smp.setOffset(0);
        smp.setCount(processingMaximum);

        RtsSearchResponse searchResponse = searchService.search(smp);

        LOG.info("About to change state for {} documents ", searchResponse.getCount());

        for (IDHolder idHolder : searchResponse.getResult()) {
            MutableConversation conversation = conversationRepository.getById(idHolder.getConversationId());
            String messageId = idHolder.getMessageId();

            try {
                moderationService.changeMessageState(conversation, messageId, new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
            } catch (RuntimeException e) {
                LOG.warn("could not auto send message after retention time - skipping conv/msg " + idHolder.getConversationId() + "/" + idHolder.getMessageId(), e);
            } catch (MessageNotFoundException e) {
                LOG.warn("Message with id {} was not found - conversation persistence and index likely out of sync - will reindex the conversation", messageId);

                document2KafkaSink.pushToKafka(conversation);
            }
        }
    }
}
