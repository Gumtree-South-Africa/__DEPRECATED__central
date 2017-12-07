package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.indexer.conversation.SearchIndexer;
import com.ecg.replyts.core.runtime.listener.MailPublisher;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@Import(ProcessingFinalizer.class)
@TestPropertySource(properties = {
        "persistence.strategy = cassandra",
        "ship.documents2kafka.enabled=true",
        "indexing.2kafka.enabled=ams1",
        "region=ams1"
})
public class ProcessingFinalizerIndexingToKafkaOnlyTest {
    @MockBean
    private MutableConversationRepository conversationRepository;

    @MockBean
    private MailRepository mailRepository;

    @MockBean
    private SearchIndexer searchIndexer;

    @MockBean
    private DefaultMutableConversation conv;

    @MockBean
    private Termination termination;

    @MockBean
    private ConversationEventListeners conversationEventListeners;

    @MockBean
    private MailPublisher mailProcessedListener;

    @MockBean
    private Document2KafkaSink document2KafkaSink;

    @Autowired
    private ProcessingFinalizer messagePersister;

    @Before
    public void setUp() throws Exception {
        when(conv.getMessages()).thenReturn(Collections.EMPTY_LIST);
        when(termination.getEndState()).thenReturn(MessageState.ORPHANED);
        when(termination.getIssuer()).thenReturn(Object.class);
        when(conv.getId()).thenReturn("a");
    }

    @Test
    public void updatingForCassandraEvenIfConversationSizeExceedsConstraint() {
        when(conv.getMessages()).thenReturn(Arrays.asList(new Message[ProcessingFinalizer.MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION + 1]));
        String msgid = "1";

        messagePersister.persistAndIndex(conv, msgid, "incoming".getBytes(), Optional.of("outgoing".getBytes()), termination);

        verify(conv).commit(conversationRepository, conversationEventListeners);

        verify(mailRepository).persistMail(anyString(), any(byte[].class), any(Optional.class));

        verify(searchIndexer, never()).updateSearchAsync(Arrays.<Conversation>asList(conv));

        verify(document2KafkaSink).pushToKafka(conv, msgid);
    }
}

