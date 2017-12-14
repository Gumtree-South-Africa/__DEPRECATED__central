package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse.IDHolder;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Optional;

@Component
public class MessageSender {
    private static final Logger LOG = LoggerFactory.getLogger(MessageSender.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SearchService searchService;

    @Autowired
    private MutableConversationRepository conversationRepository;

    @Autowired
    private SearchIndexer searchIndexer;

    @Value("${replyts2.sendHeld.retentionTimeHours:12}")
    private int retentionTimeHours;

    @Value("${replyts2.sendHeld.retentionTimeStartHours:24}") // 24 hours is the default to guard against stuck mails
    private int retentionTimeStartHours;
    
    @Value("${replyts2.sendHeld.processingMaximum:20000}")
    private int processingMaximum;
    
    public void work() {
        // moderation service is in a feedback loop to the filter chain that needs the plugin system to work
        // this cronjob is a plugin --> we've got a circular dependency at startup here
        // I resolve this by fetching the moderation service on the cron job run individually.
        ModerationService moderationService = applicationContext.getBean(ModerationService.class);

        Date startDate = DateTime.now().minusHours(retentionTimeHours).minusHours(retentionTimeStartHours).toDate();
        // All mails received before (now() - retentionTimeHours) which are still in held can be sent out.
        Date endDate = DateTime.now().minusHours(retentionTimeHours).toDate();

        SearchMessagePayload smp = new SearchMessagePayload();

        smp.setMessageState(MessageRtsState.HELD);
        smp.setFromDate(startDate);
        smp.setToDate(endDate);
        smp.setOffset(0);
        smp.setCount(processingMaximum);

        RtsSearchResponse searchResponse = searchService.search(smp);

        LOG.info("About to change state for {} documents " , searchResponse.getCount());

        for (IDHolder idHolder : searchResponse.getResult()) {
            MutableConversation conversation = conversationRepository.getById(idHolder.getConversationId());
            String messageId = idHolder.getMessageId();

            try {
                moderationService.changeMessageState(conversation, messageId, new ModerationAction(ModerationResultState.TIMED_OUT, Optional.empty()));
            } catch (RuntimeException e) {
                LOG.warn("could not auto send message after retention time - skipping conv/msg " + idHolder.getConversationId() + "/" + idHolder.getMessageId(), e);
            } catch (MessageNotFoundException e) {
                LOG.warn("Message with id {} was not found - conversation persistence and index likely out of sync - will reindex the conversation", messageId);

                searchIndexer.updateSearchAsync(ImmutableList.of(conversation));
            }
        }
    }
}
