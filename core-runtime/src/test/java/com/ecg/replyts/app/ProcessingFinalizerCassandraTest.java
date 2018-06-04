package com.ecg.replyts.app;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.indexer.Document2KafkaSink;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.ecg.replyts.core.runtime.persistence.conversation.MutableConversationRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Import(ProcessingFinalizer.class)
public class ProcessingFinalizerCassandraTest {
    @MockBean
    private MutableConversationRepository conversationRepository;

    @MockBean
    private Document2KafkaSink document2KafkaSink;

    @MockBean
    private DefaultMutableConversation conv;

    @MockBean
    private Termination termination;

    @MockBean
    private ConversationEventListeners conversationEventListeners;

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
        String msgId = "1";
        when(conv.getMessages()).thenReturn(Arrays.asList(new Message[ProcessingFinalizer.MAXIMUM_NUMBER_OF_MESSAGES_ALLOWED_IN_CONVERSATION + 1]));

        messagePersister.persistAndIndex(conv, msgId, Optional.of("incoming".getBytes()), Optional.of("outgoing".getBytes()), termination);

        verify(conv).commit(conversationRepository, conversationEventListeners);

        verify(document2KafkaSink).pushToKafka(conv, msgId);
    }
}

