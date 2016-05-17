package com.ecg.messagecenter.persistence.riak;

import com.ecg.messagecenter.identifier.UserIdentifierService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RiakPostBoxServiceTest {

    private RiakPostBoxRepository riakPostBoxRepository = mock(RiakPostBoxRepository.class);
    private ConversationRepository conversationRepository = mock(ConversationRepository.class);
    private UserIdentifierService userIdentifierService = mock(UserIdentifierService.class);

    @Test
    public void testGetUnreadCounters() throws Exception {
        RiakPostBoxService service = new RiakPostBoxService(riakPostBoxRepository, conversationRepository, userIdentifierService, 250);

        PostBoxUnreadCounts expectedUnreadCounters = new PostBoxUnreadCounts(2, 3);
        when(riakPostBoxRepository.getUnreadCounts("p123")).thenReturn(expectedUnreadCounters);

        PostBoxUnreadCounts actualUnreadCounters = service.getUnreadCounts("p123");
        assertSame(expectedUnreadCounters, actualUnreadCounters);

//        [ comments taken from the original mp repository ]
//
//        RiakPostboxService service = new RiakPostboxService(postBoxRepository, conversationRepository, userIdentifierService, 250);
//
//        PostBox postBox = mock(PostBox.class);
//        when(postBox.getNumUnreadConversations()).thenReturn(5L);
//        when(postBox.getNewRepliesCounter()).thenReturn(2L);
//        when(postBoxRepository.getPostBox("p123")).thenReturn(postBox);
//
//        PostBoxUnreadCounters unreadCounters = service.getUnreadCounts("p123");
//        assertEquals(5, unreadCounters.getNumUnreadConversations());
//        assertEquals(2, unreadCounters.getNumUnreadMessages());
    }
}
