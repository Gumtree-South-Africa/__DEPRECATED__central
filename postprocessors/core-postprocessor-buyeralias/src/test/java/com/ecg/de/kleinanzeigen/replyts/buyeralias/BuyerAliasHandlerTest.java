package com.ecg.de.kleinanzeigen.replyts.buyeralias;

import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * User: acharton
 * Date: 11/12/13
 */
public class BuyerAliasHandlerTest {

    private MessageProcessingContext msgContext;
    private Map<String, String> headers;
    private String aliasFormatPattern;

    @Before
    public void setUp() throws Exception {
        headers = new HashMap<String, String>();
        msgContext = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        aliasFormatPattern = "%s";

        when(msgContext.getConversation().getCustomValues()).thenReturn(headers);
    }

    @Test
    public void headerExists() {
        headers.put("buyerName", "Buyer Name");

        assertTrue(new BuyerAliasHandler(msgContext, "buyerName",  "sellerName", aliasFormatPattern).containsBuyerName());
    }

    @Test
    public void setAliasIfExists() {
        headers.put("buyerName", "Buyer Name");
        when(msgContext.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        new BuyerAliasHandler(msgContext, "buyerName",  "sellerName", aliasFormatPattern).handle();

        verify(msgContext.getOutgoingMail()).setFrom(any(MailAddress.class));
    }

    @Test
    public void headerNotExists() {
        assertFalse(new BuyerAliasHandler(msgContext, "buyerName",  "sellerName", aliasFormatPattern).containsBuyerName());
    }

    @Test
    public void suppressException() {
        when(msgContext.getOutgoingMail()).thenThrow(new RuntimeException());

        try {
            new BuyerAliasHandler(msgContext, "buyerName",  "sellerName", aliasFormatPattern).handle();
        } catch (Exception e) {
            fail();
        }

    }
}
