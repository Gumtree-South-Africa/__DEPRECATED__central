package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@Ignore("COMAAS-849")
public class VolumeFilterClusterIntegrationTest {
    private VolumeFilter volumeFilter1;
    private VolumeFilter volumeFilter2;

    private HazelcastInstance hazelcastInstance1;
    private HazelcastInstance hazelcastInstance2;
    private Quota singleMessageQuota;

    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Mock
    private Mail mail;

    @Mock
    private Message message;

    @Mock
    private Conversation conversation;
    
    @Mock
    private Map<String, String> customValues;

    @Mock
    private Map<String, String> customHeaders;
    
    @Before
    public void setUp() throws Exception {
        // localhost hazelcast config without multicast, but with an explicit cluster name to avoid conflicts in case of parallel execution
        Config config = new Config().setNetworkConfig(new NetworkConfig()
          .setJoin(new JoinConfig()
            .setTcpIpConfig(new TcpIpConfig().setEnabled(true).addMember("127.0.0.1"))
            .setMulticastConfig(new MulticastConfig().setEnabled(false))))
          .setGroupConfig(new GroupConfig("volume-filter-cluster-integration-test-" + new Random().nextInt(), "nopass"));

        hazelcastInstance1 = Hazelcast.newHazelcastInstance(config);
        hazelcastInstance2 = Hazelcast.newHazelcastInstance(config);

        singleMessageQuota = new Quota(1, 1, TimeUnit.MINUTES, 100, 0, TimeUnit.MINUTES);

        // JsonNode jsonNode = new ObjectMapper().readTree("{\"runFor\": {}}");

        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(conversation.getUserId(ConversationRole.Buyer)).thenReturn("from@example.com");
        when(conversation.getCustomValues()).thenReturn(customValues);
        when(customValues.get("conversation_id")).thenReturn("1234567890");
        
        when(messageProcessingContext.getMail()).thenReturn(mail);
        when(mail.getCustomHeaders()).thenReturn(customHeaders);
        when(customHeaders.get("conversation_id")).thenReturn("1234567890"); 
        when(customHeaders.get("categoryid")).thenReturn("8");
    }

    @After
    public void tearDown() throws Exception {
        hazelcastInstance1.shutdown();
        hazelcastInstance2.shutdown();
    }

    @Test
    @Ignore // Ignore this test for now; can't really instantiate two SharedBrains; should rewrite the tests
    public void twoNodes_differentInstances_separateCalculation() throws Exception {
        volumeFilter1 = new VolumeFilter("vf-test-diffInstances-1", mock(SharedBrain.class), ImmutableList.of(singleMessageQuota), false, Collections.<Integer>emptyList(), Arrays.asList(8));
        volumeFilter2 = new VolumeFilter("vf-test-diffInstances-2", mock(SharedBrain.class), ImmutableList.of(singleMessageQuota), false, Collections.<Integer>emptyList(), Arrays.asList(8));

        assertThat(volumeFilter1.filter(messageProcessingContext), empty());
        assertThat(volumeFilter2.filter(messageProcessingContext), empty());
    }

    @Test
    @Ignore // Ignore this test for now; can't really instantiate two SharedBrains; should rewrite the tests
    public void twoNodes_sameInstances_combinedCalculation() throws Exception {
        volumeFilter1 = new VolumeFilter("vf-test-sameInstance", mock(SharedBrain.class), ImmutableList.of(singleMessageQuota), false,Collections.<Integer>emptyList(), Arrays.asList(8));
        volumeFilter2 = new VolumeFilter("vf-test-sameInstance", mock(SharedBrain.class), ImmutableList.of(singleMessageQuota), false,Collections.<Integer>emptyList(), Arrays.asList(8));

        assertThat(volumeFilter1.filter(messageProcessingContext), empty());
        assertThat(volumeFilter2.filter(messageProcessingContext).size(), is(1));
    }
}