package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import ca.kijiji.replyts.Activation;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VolumeFilterClusterTest {
    private static final Quota SINGLE_MESSAGE_QUOTA = new Quota(1, 1, TimeUnit.MINUTES, 100, 0, TimeUnit.MINUTES);
    private static final String FROM_MAIL = "from@example.com";

    private Activation activation;

    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Mock
    private Message message;

    @Mock
    private Conversation conversation;

    @Mock
    private SharedBrain sharedBrain;

    @Mock
    private EventStreamProcessor eventStreamProcessor;

    @Before
    public void setUp() throws Exception {
        JsonNode jsonNode = new ObjectMapper().readTree("{\"runFor\": {}}");
        activation = new Activation(jsonNode);

        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(conversation.getUserId(ConversationRole.Buyer)).thenReturn(FROM_MAIL);
    }

    @Test
    public void twoNodes_differentInstances_separateCalculation() throws Exception {
        VolumeFilter volumeFilter1 = new VolumeFilter("vf-test-diffInstances-1", sharedBrain, ImmutableList.of(SINGLE_MESSAGE_QUOTA), false, activation, eventStreamProcessor);
        VolumeFilter volumeFilter2 = new VolumeFilter("vf-test-diffInstances-2", sharedBrain, ImmutableList.of(SINGLE_MESSAGE_QUOTA), false, activation, eventStreamProcessor);

        assertThat(volumeFilter1.doFilter(messageProcessingContext), empty());
        assertThat(volumeFilter2.doFilter(messageProcessingContext), empty());

        verify(sharedBrain, times(2)).markSeenAsync(eq(FROM_MAIL));
        verify(sharedBrain, times(2)).getViolationRecordFromMemory(eq(FROM_MAIL));
    }

    @Test
    public void twoNodes_sameInstances_combinedCalculation() throws Exception {
        String testSpecificFilterName = "vf-test-sameInstance".concat(String.valueOf(System.currentTimeMillis()));
        VolumeFilter volumeFilter1 = new VolumeFilter(testSpecificFilterName, sharedBrain, ImmutableList.of(SINGLE_MESSAGE_QUOTA), false, activation, eventStreamProcessor);
        VolumeFilter volumeFilter2 = new VolumeFilter(testSpecificFilterName, sharedBrain, ImmutableList.of(SINGLE_MESSAGE_QUOTA), false, activation, eventStreamProcessor);

        assertThat(volumeFilter1.doFilter(messageProcessingContext), empty());
        when(sharedBrain.getViolationRecordFromMemory(eq(FROM_MAIL))).thenReturn(mock(QuotaViolationRecord.class));
        assertThat(volumeFilter2.doFilter(messageProcessingContext).size(), is(1));
    }
}