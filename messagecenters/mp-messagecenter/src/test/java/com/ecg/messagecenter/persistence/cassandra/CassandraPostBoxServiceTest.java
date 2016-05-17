package com.ecg.messagecenter.persistence.cassandra;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CassandraPostBoxServiceTest {

    private DefaultCassandraPostBoxRepository postBoxRepository = mock(DefaultCassandraPostBoxRepository.class);
    private ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private UserIdentifierService userIdentifierService = mock(UserIdentifierService.class);

    @Test
    public void testGetUnreadCounters() throws Exception {
        CassandraPostBoxService service = new CassandraPostBoxService(postBoxRepository, conversationRepository, userIdentifierService, 250);

        PostBoxUnreadCounts postBoxUnreadCounts = new PostBoxUnreadCounts(2, 3);
        when(postBoxRepository.getUnreadCounts("p123")).thenReturn(postBoxUnreadCounts);

        PostBoxUnreadCounts actualUnreadCounters = service.getUnreadCounts("p123");
        assertSame(postBoxUnreadCounts, actualUnreadCounters);
    }
}