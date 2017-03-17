package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakClient;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class RiakConversationRepositoryIntegrationTest extends ConversationRepositoryIntegrationTestBase<RiakConversationRepository> {
    @Override
    protected RiakConversationRepository createConversationRepository() {
        IRiakClient riakClient = new EmbeddedRiakClient();
        return new RiakConversationRepository(riakClient, true, false);
    }

    @Test
    public void findOldConversations() {
        DateTime threshold = new DateTime(2012, 2, 10, 9, 11, 44);
        givenABunchOfCommands();
        List<String> conversationIds = conversationRepository.listConversationsCreatedBetween(threshold.minusDays(1), threshold);
        assertThat(conversationIds.size(), is(1));
        assertThat(conversationIds.contains(conversationId1), is(true));
        assertThat(conversationIds.contains(conversationId2), is(false));
    }
}
