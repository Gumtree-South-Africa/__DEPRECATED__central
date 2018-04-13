package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.comaas.core.filter.ebayservices.IpAddressExtractor;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * User: acharton
 * Date: 12/17/12
 */
@RunWith(MockitoJUnitRunner.class)
public class IpAddressExtractorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext mpc;
    @Mock
    private Mail mail;

    private IpAddressExtractor ipAddressExtractor = new IpAddressExtractor();

    @Before
    public void setUp() throws Exception {
        when(mpc.getMail()).thenReturn(Optional.of(mail));
        when(mail.getUniqueHeader(IpAddressExtractor.IP_ADDR_HEADER)).thenReturn("10.0.0.1");

        when(mpc.getConversation().getMessages().size()).thenReturn(1);
    }

    @Test
    public void enableForFirstContactMailWithIpAddressInHeader() throws Exception {
        assertTrue(ipAddressExtractor.retrieveIpAddress(mpc).isPresent());
    }

    @Test
    public void disabledForSecondMessage() throws Exception {
        when(mpc.getConversation().getMessages().size()).thenReturn(2);

        assertFalse(ipAddressExtractor.retrieveIpAddress(mpc).isPresent());
    }

    @Test
    public void disabledWhileNoIpHeader() throws Exception {
        when(mpc.getMail().get().getUniqueHeader(IpAddressExtractor.IP_ADDR_HEADER)).thenReturn(null);

        assertFalse(ipAddressExtractor.retrieveIpAddress(mpc).isPresent());
    }

    @Test
    public void disabledForIpv6Addrs() throws Exception {
        when(mpc.getMail().get().getUniqueHeader(IpAddressExtractor.IP_ADDR_HEADER)).thenReturn("::1");

        assertFalse(ipAddressExtractor.retrieveIpAddress(mpc).isPresent());
    }

    @Test
    public void extractsIpHeaderFromStructuredMail() throws Exception {
        byte[] mailBody = ("Delivered-To: interessent-18ovcqic1zgjt@mail.ebay-kleinanzeigen.de\n" +
                "From: foo@asdf.com\n" +
                "Subject: a replyt test\n" +
                "X-CUST-IP: 41.67.128.24\n" +
                "\n" +
                "hello\n" +
                "asfasdf").getBytes();
        Mail mail = StructuredMail.parseMail(new ByteArrayInputStream(mailBody));
        when(mpc.getMail()).thenReturn(Optional.of(mail));

        assertEquals("41.67.128.24", ipAddressExtractor.retrieveIpAddress(mpc).get());

    }
}
