package com.ecg.de.mobile.replyts.rating.svc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.Test;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Maps;

import de.mobile.dealer.rating.invite.EmailInviteEntity;
import static de.mobile.dealer.rating.invite.EmailInviteTriggerType.BY_CONTACT_MESSAGE;

/**
 * Created by vbalaramiah on 4/28/15.
 */
public class EmailInviteAssemblerTest {

    private final EmailInviteAssembler assembler = new EmailInviteAssembler();

    @Test
    public void testToEmailInviteWithAllHeadersPresent() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("X-ADID", "123");
        headers.put("X-Cust-Customer_Id", "456");
        headers.put("X-Cust-Ip_Address_V4V6", "123.123.123.123");
        headers.put("X-Cust-Publisher", "SourceXY");
        headers.put("X-Cust-Buyer_Locale", "de");
        headers.put("X-Mobile-Vi", "test-mobile-vi-id-123");
        headers.put("X-Cust-Buyer_Device_Id", "test-device-id-123");
        headers.put("X-Cust-Buyer_Customer_Id", "test-customer-id-123");
        when(message.getHeaders()).thenReturn(headers);
        
        String conversationId = "56789-efgh";
        EmailInviteEntity invite = assembler.toEmailInvite(message, conversationId);
        
        assertThat(invite.getTriggerType(), is(BY_CONTACT_MESSAGE));
        assertThat(invite.getAdId(), is(123l));
        assertThat(invite.getDealerId(), is(456l));
        assertThat(invite.getIpAddress(), is("123.123.123.123"));
        assertThat(invite.getSource(), is("SourceXY"));
        assertThat(invite.getLocale(), is("de"));
        assertThat(invite.getMobileViId(), is("test-mobile-vi-id-123"));
        assertThat(invite.getBuyerDeviceId(), is("test-device-id-123"));
        assertThat(invite.getBuyerCustomerId(), is("test-customer-id-123"));
        assertThat(invite.getReplytsConversationId(), is("56789-efgh"));
    }

    @Test
    public void testToEmailInviteWithAllHeadersAbsent() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        when(message.getHeaders()).thenReturn(headers);
        
        String conversationId = "56789-efgh";
        EmailInviteEntity invite = assembler.toEmailInvite(message, conversationId);
        
        assertThat(invite.getTriggerType(), is(BY_CONTACT_MESSAGE));
        assertThat(invite.getAdId(), is(0l));
        assertThat(invite.getDealerId(), is(0l));
        assertThat(invite.getIpAddress(), is(nullValue()));
        assertThat(invite.getSource(), is(nullValue()));
        assertThat(invite.getLocale(), is(nullValue()));
        assertThat(invite.getMobileViId(), is(nullValue()));
        assertThat(invite.getBuyerDeviceId(), is(nullValue()));
        assertThat(invite.getBuyerCustomerId(), is(nullValue()));
        assertThat(invite.getReplytsConversationId(), is("56789-efgh"));
        assertThat(invite.getBuyerEmail(), is(nullValue()));
    }

    @Test
    public void testToEmailInviteWithReplyToHeader() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("Reply-To", "max@mustermann.de");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getBuyerEmail(), is("max@mustermann.de"));
    }

    @Test
    public void testToEmailInviteWithFromHeader() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("From", "max@mustermann.de");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getBuyerEmail(), is("max@mustermann.de"));
    }

    @Test
    public void testStrippingOfCOMAPrefix() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("X-ADID", "COMA123");
        headers.put("X-Cust-Customer_Id", "COMA456");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getAdId(), is(123l));
        assertThat(invite.getDealerId(), is(456l));
    }
    
    @Test
    public void testNonNumericIds() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("X-ADID", "abc123");
        headers.put("X-Cust-Customer_Id", "456abc");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getAdId(), is(0l));
        assertThat(invite.getDealerId(), is(0l));
    }
    
    @Test(expected=RuntimeException.class)
    public void testInvalidBuyerEmail() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("From", "assaf safsaf");
        when(message.getHeaders()).thenReturn(headers);
        
        assembler.toEmailInvite(message, null);
    }

    @Test
    public void testBuyerEmailFallbackForAngleBrackets() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("From", "Max Mustermann ( <max@mustermann.de>");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getBuyerEmail(), is("max@mustermann.de"));
    }

    @Test
    public void testBuyerEmailFallbackForAngleBrackets2() {
        Message message = mock(Message.class);

        Map<String, String> headers = Maps.newHashMap();
        headers.put("From", "foo <test111@foo.de> <test@mobile.de>");
        when(message.getHeaders()).thenReturn(headers);
        
        EmailInviteEntity invite = assembler.toEmailInvite(message, null);
        assertThat(invite.getBuyerEmail(), is("test@mobile.de"));
    }

}
