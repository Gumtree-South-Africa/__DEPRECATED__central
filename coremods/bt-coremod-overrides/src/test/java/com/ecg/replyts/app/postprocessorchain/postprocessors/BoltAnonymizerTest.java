package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTenantMailCloakingService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BoltAnonymizerTest {
    @Mock
    private MultiTenantMailCloakingService mailCloakingService;

    @Mock
    private MessageProcessingContext context;

    @Mock
    private MutableMail outgoingMail;

    @Mock
    private Conversation conversation;

    private BoltAnonymizer anonymizer;

    @Before
    public void setUp() {
        anonymizer = new BoltAnonymizer(mailCloakingService);

        ReflectionTestUtils.setField(anonymizer, "defaultFromDisplay", "User via Gumtree");

        when(context.getConversation()).thenReturn(conversation);

        when(conversation.getUserIdFor(ConversationRole.Seller)).thenReturn("seller@domain.tld");
        when(conversation.getUserIdFor(ConversationRole.Buyer)).thenReturn("buyer@domain.tld");

        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("buyer.cloaked@ebay.com"));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress("seller.cloaked@ebay.com"));
    }

    @Test
    public void senderEmailIsCloaked() {
        when(context.getOutgoingMail()).thenReturn(outgoingMail);
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(context.getMail()).thenReturn(Optional.of(outgoingMail));
        when(outgoingMail.getCustomHeaders()).thenReturn(new HashMap<String, String>());

        anonymizer.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress("seller@domain.tld"));
        verify(outgoingMail).setFrom(new MailAddress("User via Gumtree <buyer.cloaked@ebay.com>"));
    }

    @Test
    public void receiverIsSetToUncloakedBuyerWhenMessageDirectionIsSellerToBuyer() {
        when(context.getOutgoingMail()).thenReturn(outgoingMail);
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        when(context.getMail()).thenReturn(Optional.of(outgoingMail));
        when(outgoingMail.getCustomHeaders()).thenReturn(new HashMap<String, String>());

        anonymizer.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress("buyer@domain.tld"));
        verify(outgoingMail).setFrom(new MailAddress("User via Gumtree <seller.cloaked@ebay.com>"));
    }

    @Test
    public void receiverIsSetToUncloakedSellerWhenMessageDirectionIsBuyerToSeller() {
        when(context.getOutgoingMail()).thenReturn(outgoingMail);
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        when(context.getMail()).thenReturn(Optional.of(outgoingMail));
        when(outgoingMail.getCustomHeaders()).thenReturn(new HashMap<String, String>());

        anonymizer.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress("seller@domain.tld"));
        verify(outgoingMail).setFrom(new MailAddress("User via Gumtree <buyer.cloaked@ebay.com>"));
    }
}