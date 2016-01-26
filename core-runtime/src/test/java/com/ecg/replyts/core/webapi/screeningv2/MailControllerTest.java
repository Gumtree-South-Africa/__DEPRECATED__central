package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MailControllerTest {
    private byte[] inboundBytes;
    private byte[] outboundBytes;

    private HttpServletResponse response;
    private ServletOutputStream responseOutputStream;

    private MailController mailController;
    private MailRepository mailRepository;


    @Before
    public void setup() throws IOException {
        inboundBytes = new byte[]{1, 2};
        outboundBytes = new byte[]{3, 4};

        mailRepository = mock(MailRepository.class);
        when(mailRepository.readInboundMail("exists")).thenReturn(inboundBytes);
        when(mailRepository.readOutboundMail("exists")).thenReturn(outboundBytes);

        response = mock(HttpServletResponse.class);
        responseOutputStream = mock(ServletOutputStream.class);
        when(response.getOutputStream()).thenReturn(responseOutputStream);

        mailController = new MailController(mailRepository);
    }

    @Test
    public void getInboundGetsInbound() throws Exception {

        mailController.getRawMail(response, "exists", MailTypeRts.INBOUND);

        verify(mailRepository).readInboundMail(eq("exists"));
        verify(responseOutputStream).write(eq(inboundBytes));
    }

    @Test
    public void getOutboundGetsOutbound() throws Exception {
        mailController.getRawMail(response, "exists", MailTypeRts.OUTBOUND);

        verify(mailRepository).readOutboundMail(eq("exists"));
        verify(responseOutputStream).write(eq(outboundBytes));
    }


}
