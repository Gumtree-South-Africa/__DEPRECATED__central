package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.processing.ModerationAction;
import com.ecg.replyts.core.api.processing.ModerationService;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse.IDHolder;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;
import com.ecg.replyts.core.api.webapi.model.MessageRtsState;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
class MessageSender {
    private final SearchService searchService;
    private final ApplicationContext applicationContext;
    private final int retentionTimeHours;
    private final MutableConversationRepository conversationRepository;

    private static final Logger LOG = LoggerFactory.getLogger(MessageSender.class);

    @Autowired
    MessageSender(
            SearchService searchService,
            ApplicationContext applicationContext,
            @Value("${replyts2.sendHeld.retentionTimeHours:12}") int retentionTimeHours,
            MutableConversationRepository conversationRepository) {
        this.searchService = searchService;
        this.applicationContext = applicationContext;
        this.retentionTimeHours = retentionTimeHours;
        this.conversationRepository = conversationRepository;
    }

    public void work() {

        // moderation service is in a feedback loop to the filter chain that needs the plugin system to work
        // this cronjob is a plugin --> we've got a circular dependency at startup here
        // I resolve this by fetching the moderation service on the cron job run individually.
        ModerationService moderationService = applicationContext.getBean(ModerationService.class);

        // all mails received before now - retention time and are still in held can be sent out.
        // the filter starts to operate after cs agent working hours + retention time, so no care must be taken about breaks.
        Date endOfRetentionTime = DateTime.now().minusHours(retentionTimeHours).toDate();
        // if a mail continous to fail then there might be something wrong with it. this is to ensure that it's not retried forever.
        Date oneDayBeforeEndOfRetentionTime = DateTime.now().minusHours(retentionTimeHours).minusHours(24).toDate();

        SearchMessagePayload smp = new SearchMessagePayload();
        smp.setMessageState(MessageRtsState.HELD);
        smp.setFromDate(oneDayBeforeEndOfRetentionTime);
        smp.setToDate(endOfRetentionTime);
        smp.setOffset(0);
        smp.setCount(20000);
        RtsSearchResponse searchResponse = searchService.search(smp);

        for (IDHolder idHolder : searchResponse.getResult()) {
            try {
                moderationService.changeMessageState(conversationRepository.getById(idHolder.getConversationId()), idHolder.getMessageId(), new ModerationAction(ModerationResultState.TIMED_OUT, Optional.<String>absent()));
            } catch (RuntimeException e) {
                LOG.warn("could not auto send message after retention time - skipping conv/msg " + idHolder.getConversationId() + "/" + idHolder.getMessageId(), e);
            }
        }

    }
}