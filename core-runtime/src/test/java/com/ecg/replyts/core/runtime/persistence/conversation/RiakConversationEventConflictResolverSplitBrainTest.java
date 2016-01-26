package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakObject;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RiakConversationEventConflictResolverSplitBrainTest {
    // we had this situation in prod in at least two cases:
    // split brain, in one DC mail was sent
    // in other DC mail was dropped a bit later.
    // due to split brain this was legal but can't be resolved to a
    // valid state
    // this test should make the issue visible


    @Mock
    private IRiakObject obj1;

    @Mock
    private IRiakObject obj2;


    @Test
    public void executeMerge() {
        when(obj1.getValue()).thenReturn(get("VersionA"));
        when(obj2.getValue()).thenReturn(get("VersionB"));
        ConversationEvents eventsFromVersionA = new ConversationEventsConverter("foobucket", new ConversationJsonSerializer()).toDomain(obj1);
        ConversationEvents eventsFromVersionB = new ConversationEventsConverter("foobucket", new ConversationJsonSerializer()).toDomain(obj2);

        ConversationEvents events = new RiakConversationEventConflictResolver().resolve(Lists.newArrayList(eventsFromVersionA, eventsFromVersionB));

        ImmutableConversation.replay(events.getEvents());

        // last 2 events are both TO --> good

    }

    private byte[] get(String version) {
        try (InputStream is = getClass().getResourceAsStream("RiakConversationEventConflictResolverSplitBrainTest_" + version + ".json")) {
            return ByteStreams.toByteArray(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
