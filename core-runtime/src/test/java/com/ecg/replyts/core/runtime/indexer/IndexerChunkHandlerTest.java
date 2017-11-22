package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IndexerChunkHandlerTest {
    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private SearchIndexer indexer;

    @InjectMocks
    private IndexerChunkHandler indexerChunkHandler;

    @Before
    public void setup() {
        ReflectionTestUtils.setField(indexerChunkHandler, "maxChunkSize", 3);
    }

    @Test
    public void indexChunk() throws Exception {
        List<String> conversationIds = Lists.newArrayList("foo1", "foo2", "foo3");

        List<Conversation> conversations = Lists.newArrayList();

        for(String conversationId : conversationIds) {
            MutableConversation mutableConversation = mock(MutableConversation.class);

            conversations.add(mutableConversation);

            when(conversationRepository.getById(conversationId)).thenReturn(mutableConversation);
        }

        indexerChunkHandler.indexChunk(conversationIds);

        verify(indexer).updateSearchSync(conversations);
    }

    @Test
    public void indexChunkWithMissingConvId() throws Exception {
        List<String> conversationIds = Lists.newArrayList("foo1");

        List<Conversation> conversations = Lists.newArrayList();

        indexerChunkHandler.indexChunk(conversationIds);

        verify(indexer).updateSearchSync(conversations);
    }

    @Test
    public void indexChunkHighThanMax() throws Exception {
        List<String> conversationIds = Lists.newArrayList("foo1", "foo2", "foo3", "foo4", "foo5", "foo6", "foo7");

        List<Conversation> conversations = Lists.newArrayList();

        for(String conversationId : conversationIds) {
            MutableConversation mutableConversation = mock(MutableConversation.class);

            conversations.add(mutableConversation);

            when(conversationRepository.getById(conversationId)).thenReturn(mutableConversation);
        }

        indexerChunkHandler.indexChunk(conversationIds);

        verify(indexer).updateSearchSync(conversations.subList(0, 3));

        verify(indexer).updateSearchSync(conversations.subList(3, 6));

        verify(indexer).updateSearchSync(conversations.subList(6, 7));
    }
}