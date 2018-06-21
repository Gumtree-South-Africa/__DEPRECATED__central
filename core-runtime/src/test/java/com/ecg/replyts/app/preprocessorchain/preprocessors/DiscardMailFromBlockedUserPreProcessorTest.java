package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.api.processing.Termination;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.Optional;

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
    private MutableConversation conversation;

    @Mock
    private UserIdentifierService userIdentifierService;

    @Mock
    private Termination termination;

    private DiscardMailFromBlockedUserPreProcessor preProcessor;

    private static final String SELLER_MAIL = "seller@mail.com";
    private static final String BUYER_MAIL = "buyer@mail.com";
    private static final String BUYER_ID = "1111";
    private static final String SELLER_ID = "2222";

    @Before
    public void setUp(){
        preProcessor = new DiscardMailFromBlockedUserPreProcessor(blockUserRepository, userIdentifierService);
        when(userIdentifierService.getBuyerUserId(conversation)).thenReturn(Optional.empty());
        when(userIdentifierService.getSellerUserId(conversation)).thenReturn(Optional.empty());
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getBuyerId()).thenReturn(BUYER_MAIL);
        when(conversation.getSellerId()).thenReturn(SELLER_MAIL);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getTermination()).thenReturn(termination);
        when(termination.getReason()).thenReturn("reason");
    }

    @Test
    public void discardsMailIfBlockedUsers(){
        when(blockUserRepository.areUsersBlocked(BUYER_MAIL, SELLER_MAIL)).thenReturn(true);
        preProcessor.preProcess(context);
        verify(context).terminateProcessing(eq(MessageState.DISCARDED), any(), anyString());
    }

    @Test
    public void doesNotDiscardMailIfNotBlockedUsers(){
        when(blockUserRepository.areUsersBlocked(BUYER_MAIL, SELLER_MAIL)).thenReturn(false);
        preProcessor.preProcess(context);
        verify(context, never()).terminateProcessing(eq(MessageState.DISCARDED), any(), anyString());
    }

    @Test
    public void selectBuyerUserId() {
        when(userIdentifierService.getBuyerUserId(conversation)).thenReturn(Optional.of(BUYER_ID));
        preProcessor.preProcess(context);
        verify(blockUserRepository).areUsersBlocked(BUYER_ID, SELLER_MAIL);
    }


    @Test
    public void selectSellerUserId() {
        when(userIdentifierService.getSellerUserId(conversation)).thenReturn(Optional.of(SELLER_ID));
        preProcessor.preProcess(context);
        verify(blockUserRepository).areUsersBlocked(BUYER_MAIL, SELLER_ID);
    }
}
