package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Seller;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.processing.MessageProcessingContext.DELIVERY_CHANNEL_MAIL;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EmailOptOutPreProcessorTest {

    @Mock
    private EmailOptOutRepository emailOptOutRepo;
    @Mock
    private UserIdentifierService userIdService;
    @Mock
    private MessageProcessingContext context;
    @Mock
    private Mail mail;
    @Mock
    private MutableConversation conversation;
    @Mock
    private EmailOptOutPreProcessorFilter preProcessorFilter;

    private EmailOptOutPreProcessor preProcessor;

    @Before
    public void setUp() {
        preProcessor = new EmailOptOutPreProcessor(emailOptOutRepo, userIdService);
        preProcessor.setEmailOptOutPreProcessorFilter(preProcessorFilter);

        when(preProcessorFilter.filter(context)).thenReturn(false);
        when(context.getConversation()).thenReturn(conversation);
        when(context.getMessageDirection()).thenReturn(BUYER_TO_SELLER);
    }

    @Test
    public void skipProcessingEmail() {
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(userIdService.getUserIdentificationOfConversation(conversation, Seller)).thenReturn(of("123"));
        when(emailOptOutRepo.isEmailTurnedOn("123")).thenReturn(false);

        preProcessor.preProcess(context);

        verify(context).skipDeliveryChannel(DELIVERY_CHANNEL_MAIL);
    }

    @Test
    public void processEmail() {
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(userIdService.getUserIdentificationOfConversation(conversation, Seller)).thenReturn(of("123"));
        when(emailOptOutRepo.isEmailTurnedOn("123")).thenReturn(true);

        preProcessor.preProcess(context);

        verify(context, never()).skipDeliveryChannel(any());
    }

    @Test
    public void noUser() {
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(userIdService.getUserIdentificationOfConversation(conversation, Seller)).thenReturn(empty());

        preProcessor.preProcess(context);

        verify(context, never()).skipDeliveryChannel(any());
    }

    @Test
    public void processEmailIfFiltered() {
        when(context.getMail()).thenReturn(Optional.of(mail));
        when(preProcessorFilter.filter(context)).thenReturn(true);

        preProcessor.preProcess(context);

        verifyZeroInteractions(userIdService, context);
    }

    @Test
    public void processEmailIfNoFilter() {
        preProcessor.setEmailOptOutPreProcessorFilter(null);

        when(context.getMail()).thenReturn(Optional.of(mail));
        when(userIdService.getUserIdentificationOfConversation(conversation, Seller)).thenReturn(of("123"));
        when(emailOptOutRepo.isEmailTurnedOn("123")).thenReturn(true);

        preProcessor.preProcess(context);

        verify(context, never()).skipDeliveryChannel(any());
    }

    @Test
    public void chatShouldAlwaysOptOut() {
        when(context.getMail()).thenReturn(Optional.empty());
        when(userIdService.getUserIdentificationOfConversation(conversation, Seller)).thenReturn(of("123"));
        when(emailOptOutRepo.isEmailTurnedOn("123")).thenReturn(false);

        preProcessor.preProcess(context);

        verify(context).skipDeliveryChannel(DELIVERY_CHANNEL_MAIL);
        verifyZeroInteractions(emailOptOutRepo, userIdService);
    }
}