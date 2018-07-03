package com.ecg.comaas.core.filter.ebayservices;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IpAddressExtractorTest {

    private static final String IP_ADDR_HEADER = "X-Cust-Ip";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext contextMock;

    @Mock
    private Mail mailMock;

    @Before
    public void setUp() throws Exception {
        when(contextMock.getMail()).thenReturn(Optional.of(mailMock));
        when(contextMock.getConversation().getMessages().size()).thenReturn(1);
        when(mailMock.getUniqueHeader(IP_ADDR_HEADER)).thenReturn("10.0.0.1");
    }

    @Test
    public void whenNoMail_shouldReturnEmptyOptional() {
        when(contextMock.getMail()).thenReturn(Optional.empty());
        Optional<String> actual = IpAddressExtractor.retrieveIpAddress(contextMock);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenNotFirstMessage_shouldReturnEmptyOptional() {
        when(contextMock.getConversation().getMessages().size()).thenReturn(2);
        Optional<String> actual = IpAddressExtractor.retrieveIpAddress(contextMock);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenIpNotValid_shouldReturnEmptyOptional() {
        when(mailMock.getUniqueHeader(IP_ADDR_HEADER)).thenReturn("200.300.400.500");
        Optional<String> actual = IpAddressExtractor.retrieveIpAddress(contextMock);
        assertThat(actual).isNotPresent();
    }

    @Test
    public void whenIpIsValid_shouldReturnOptionalWithIp() {
        Optional<String> actual = IpAddressExtractor.retrieveIpAddress(contextMock);
        assertThat(actual).isPresent();
        assertThat(actual.get()).isEqualTo("10.0.0.1");
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
        when(contextMock.getMail()).thenReturn(Optional.of(mail));

        Optional<String> actual = IpAddressExtractor.retrieveIpAddress(contextMock);

        assertThat(actual).isPresent();
        assertThat(actual.get()).isEqualTo("41.67.128.24");
    }
}
