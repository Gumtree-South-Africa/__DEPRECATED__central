package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DiscardMailFromBlockedUserPreProcessorTest {

    @Mock
    private BlockUserRepository blockUserRepository;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private MutableMail mail;

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Mock
    private MutableConversation conversation;

    @Mock
    private Message message;

    @Mock
    private Termination termination;

    private DiscardMailFromBlockedUserPreProcessor preProcessor;

    private static final String SELLER_ID = "123";

    private static final String BUYER_ID = "234";

    @Before
    public void setUp(){
        preProcessor = new DiscardMailFromBlockedUserPreProcessor(blockUserRepository);
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getBuyerId()).thenReturn(BUYER_ID);
        when(conversation.getSellerId()).thenReturn(SELLER_ID);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getTermination()).thenReturn(termination);
        when(termination.getReason()).thenReturn("reason");
    }

    @Test
    public void discardsMailIfBlockedUsers(){
        when(blockUserRepository.areUsersBlocked(BUYER_ID, SELLER_ID)).thenReturn(true);

        preProcessor.preProcess(context);

        verify(context).terminateProcessing(eq(MessageState.DISCARDED), any(), anyString());
    }

    @Test
    public void doesNotDiscardMailIfNotBlockedUsers(){
        when(blockUserRepository.areUsersBlocked(BUYER_ID, SELLER_ID)).thenReturn(false);

        preProcessor.preProcess(context);

        verify(context, never()).terminateProcessing(eq(MessageState.DISCARDED), any(), anyString());
    }


}
