package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.ecg.comaas.gtuk.listener.statsnotifier.NotificationService;
import com.ecg.comaas.gtuk.listener.statsnotifier.Replyts2StatsNotifier;
import com.ecg.comaas.gtuk.listener.statsnotifier.utils.Fixtures;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class Replyts2StatsNotifierTest {

    private Replyts2StatsNotifier notifier;

    @Mock
    private ProcessingFeedback processingFeedback;

    @Mock
    private NotificationService notificationService;

    @Before
    public void setup() {
        notifier = new Replyts2StatsNotifier(notificationService, true);
    }

    @Test
    public void sendEventsToStatsApiAndGAIfItsFirstMessageOFCOnversationAndStatusIsSent() throws Exception {
        Message message = Fixtures.buildMessage(MessageDirection.BUYER_TO_SELLER, processingFeedback);
        Conversation conversation = Fixtures.buildConversation(ImmutableList.of(message));

        notifier.messageProcessed(conversation, message);

        verify(notificationService).notifyReplySuccesfullySent(conversation.getBuyerId(), conversation.getAdId(), Optional.of("abcde.fgh"), true);
    }

    @Test
    public void dontSendNextReplyToStats() throws Exception {
        Message buyerMessage = Fixtures.buildMessage(MessageDirection.BUYER_TO_SELLER, processingFeedback);
        Message sellerMessage = Fixtures.buildMessage(MessageDirection.SELLER_TO_BUYER, processingFeedback);
        Conversation conversation = Fixtures.buildConversation(ImmutableList.of(buyerMessage, sellerMessage));

        notifier.messageProcessed(conversation, sellerMessage);

        verifyZeroInteractions(notificationService);
    }
}