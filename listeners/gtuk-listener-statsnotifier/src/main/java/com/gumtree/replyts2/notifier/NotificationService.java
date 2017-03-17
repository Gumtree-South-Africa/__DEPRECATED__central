package com.gumtree.replyts2.notifier;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.analytics.GoogleAnalyticsService;
import com.gumtree.analytics.event.GAEvent;
import com.gumtree.replyts2.googleanalytics.ReplyEmailBackendSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class NotificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private static final Timer REQUEST_STATS_TIMER = TimingReports.newTimer("stats-notifier.timer");
    private static final Timer REQUEST_GA_TIMER = TimingReports.newTimer("ga-notifier.timer");

    private StatsApiNotifier statsApiNotifier;
    private GoogleAnalyticsService googleAnalyticsService;

    public NotificationService(GoogleAnalyticsService googleAnalyticsService, StatsApiNotifier statsApiNotifier) {
        this.googleAnalyticsService = googleAnalyticsService;
        this.statsApiNotifier = statsApiNotifier;
    }

    public void notifyReplySuccesfullySent(String buyerId, String adId, Optional<String> clientId,
                                           Boolean sendGoogleAnalyticsEventEnabled) {
        statsApiNotifier.sendAsyncNotification(buyerId, adId, REQUEST_STATS_TIMER);
        LOGGER.debug("Notification sent to Stats");
        if (clientId.isPresent() && sendGoogleAnalyticsEventEnabled) {
            LOGGER.debug(String.format("Client Id to use for GA: %s", clientId));
            googleAnalyticsService.sendAsyncEvent(createReplySuccessEvent(clientId.get()), Optional.of(REQUEST_GA_TIMER));
            LOGGER.debug("Notification sent to GA");
        }

    }

    private GAEvent createReplySuccessEvent(String clientId) {
        return ReplyEmailBackendSuccess.create()
                .withClientId(clientId)
                .withEventCategory("ReplySuccess")
                .build();
    }
}
