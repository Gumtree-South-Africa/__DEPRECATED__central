package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.codahale.metrics.Timer;
import com.ecg.comaas.gtuk.listener.statsnotifier.NotificationService;
import com.ecg.comaas.gtuk.listener.statsnotifier.StatsApiNotifier;
import com.ecg.comaas.gtuk.listener.statsnotifier.event.GAEvent;
import com.ecg.comaas.gtuk.listener.statsnotifier.event.ReplyEmailBackendSuccess;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

    private NotificationService notificationService;

    @Mock
    private StatsApiNotifier statsApiNotifier;

    @Mock
    private GoogleAnalyticsService googleAnalyticsService;

    @Before
    public void setUp() throws Exception {
        notificationService = new NotificationService(googleAnalyticsService, statsApiNotifier);
    }

    @Test
    public void testNotifyReplySuccesfullySent() throws Exception {
        notificationService.notifyReplySuccesfullySent("buyerId", "advertId", Optional.of("123456.7890"), true);

        verify(statsApiNotifier).sendAsyncNotification( Mockito.eq("buyerId"), Mockito.eq("advertId"), Mockito.any(Timer.class));
        verify(googleAnalyticsService).sendAsyncEvent(Mockito.eq(createGAEvent("123456.7890")),Mockito.any(Optional.class));
    }
    @Test
    public void testNotifyReplySuccesfullySentOnlyCallsStatsIfClientIdMissing() throws Exception {
        notificationService.notifyReplySuccesfullySent("buyerId", "advertId", Optional.empty(), true);

        verify(statsApiNotifier).sendAsyncNotification( Mockito.eq("buyerId"), Mockito.eq("advertId"), Mockito.any(Timer.class));
        verifyZeroInteractions(googleAnalyticsService);
    }

    private GAEvent createGAEvent(String clientId) {
        return ReplyEmailBackendSuccess.create()
                .withClientId(clientId)
                .withEventCategory("ReplySuccess")
                .build();
    }
}