package com.ecg.de.mobile.replyts.uniqueid;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: beckart
 */
public class MailUniqueIdHandlerTest {

    public static final String X_CUST_SELLER_TYPE = "seller_type";

    public static final String X_CUST_BUYER_UNIQUE_ID = "X-MOBILEDE-BUYER-ID";

    private MessageProcessingContext msgContext;

    private UniqueIdGenerator uniqueIdGenerator = new UniqueIdGenerator("foobar");

    private Set<String> ignoredMailAddresses = ImmutableSet.of("foo@foo.com");

    @Before
    public void setUp() throws Exception {
        msgContext = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void addUniqueIdMessagesSentToDealerSellers() {

        when(msgContext.getConversation().getCustomValues().get(X_CUST_SELLER_TYPE)).thenReturn("DEALER");

        String emailAddress = "buyer@mobile.de";
        when(msgContext.getConversation().getUserId(ConversationRole.Buyer)).thenReturn(emailAddress);
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(msgContext.getOutgoingMail()).addHeader(eq(X_CUST_BUYER_UNIQUE_ID), argument.capture());

        assertEquals(uniqueIdGenerator.generateUniqueBuyerId(emailAddress), argument.getValue());
    }

    @Test
    public void addUniqueIdMessagesSentToDealerSellersOnIgnoreList() {

        when(msgContext.getConversation().getCustomValues().get(X_CUST_SELLER_TYPE)).thenReturn("DEALER");

        String emailAddress = "foo@foo.com";
        when(msgContext.getConversation().getUserId(ConversationRole.Buyer)).thenReturn(emailAddress);
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(msgContext.getOutgoingMail(), never()).addHeader(eq(X_CUST_BUYER_UNIQUE_ID), argument.capture());

    }

    @Test
    public void addUniqueIdMessagesSentToDealerBuyers() {

        when(msgContext.getConversation().getCustomValues().get(X_CUST_SELLER_TYPE)).thenReturn("DEALER");

        String emailAddress = "buyer@mobile.de";
        when(msgContext.getConversation().getUserId(ConversationRole.Buyer)).thenReturn(emailAddress);
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);

        new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(msgContext.getOutgoingMail(), never()).addHeader(eq(X_CUST_BUYER_UNIQUE_ID), argument.capture());

    }

    @Test
    public void addUniqueIdMessagesSentToUnknownSellers() {

        when(msgContext.getConversation().getCustomValues().get(X_CUST_SELLER_TYPE)).thenReturn(null);

        String emailAddress = "buyer@mobile.de";
        when(msgContext.getConversation().getUserId(ConversationRole.Buyer)).thenReturn(emailAddress);
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(msgContext.getOutgoingMail(), never()).addHeader(eq(X_CUST_BUYER_UNIQUE_ID), argument.capture());
    }


    @Test
    public void addUniqueIdMessagesSentToFsboSellers() {

        when(msgContext.getConversation().getCustomValues().get(X_CUST_SELLER_TYPE)).thenReturn("FSBO");

        String emailAddress = "buyer@mobile.de";
        when(msgContext.getConversation().getUserId(ConversationRole.Buyer)).thenReturn(emailAddress);
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);

        new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        verify(msgContext.getOutgoingMail(), never()).addHeader(eq(X_CUST_BUYER_UNIQUE_ID), argument.capture());

    }




    @Test
    public void suppressException() {
        when(msgContext.getOutgoingMail()).thenThrow(new RuntimeException());

        try {
            new MailUniqueIdHandler(msgContext, uniqueIdGenerator, ignoredMailAddresses).handle();
        } catch (Exception e) {
            fail();
        }

    }
}
