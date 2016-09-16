package com.ecg.messagecenter.webapi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TopLevelExceptionHandlerTest {

    @Mock
    private HttpServletResponse response;

    private final StringWriter writer = new StringWriter();

    @Test
    public void printException() throws IOException {

        when(response.isCommitted()).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(writer));

        TopLevelExceptionHandler handler = new TopLevelExceptionHandler(new Exception("exception-message"), response);

        handler.handle();

        String message = writer.toString();
        assertThat(message).contains("exception-message");

        verify(response).setStatus(500);
    }

    @Test
    public void noWriteIfResponseCommitted() throws IOException {

        when(response.isCommitted()).thenReturn(true);

        TopLevelExceptionHandler handler = new TopLevelExceptionHandler(new Exception("exception-message"), response);

        handler.handle();

        verify(response, never()).setStatus(500);
    }
}
