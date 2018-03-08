package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.CloakedReceiverContext;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTenantMailCloakingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExistingEmailConversationLoaderTest {
    @Mock
    private MultiTenantMailCloakingService mailCloakingService;

    @Mock
    private CloakedReceiverContext cloakedReceiverContext;

    @Mock
    private MutableConversation conversation;

    @Mock
    private MutableMail mail;

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    private ExistingEmailConversationLoader existingEmailConversationLoader;

    private MessageProcessingContext context;

    @Before
    public void setUp() {
        existingEmailConversationLoader = new ExistingEmailConversationLoader(mailCloakingService);
        when(mail.getDeliveredTo()).thenReturn("to@host.com");
        when(mail.getFrom()).thenReturn("from@host.com");
        context = spy(new MessageProcessingContext(mail, "1", processingTimeGuard));
    }

    @Test
    public void loadsConversation() {
        when(mailCloakingService.resolveUser(any(MailAddress.class))).thenReturn(Optional.of(cloakedReceiverContext));
        when(cloakedReceiverContext.getConversation()).thenReturn(conversation);
        when(cloakedReceiverContext.getRole()).thenReturn(ConversationRole.Buyer);

        doReturn(conversation).when(context).getConversation();
        doReturn(new MailAddress("from@host.com")).when(context).getSender();
        doReturn(new MailAddress("to@host.com")).when(context).getRecipient();
        doReturn(MessageDirection.BUYER_TO_SELLER).when(context).getMessageDirection();

        existingEmailConversationLoader.loadExistingConversation(context);

        assertEquals(conversation, context.mutableConversation());
    }

    @Test
    public void terminatesWhenConversationNotLoaded() {
        when(mailCloakingService.resolveUser(any(MailAddress.class))).thenReturn(Optional.empty());

        existingEmailConversationLoader.loadExistingConversation(context);

        verify(context).terminateProcessing(eq(MessageState.ORPHANED), any(), anyString());

        verify(context, never()).setConversation(any(MutableConversation.class));
    }
}
