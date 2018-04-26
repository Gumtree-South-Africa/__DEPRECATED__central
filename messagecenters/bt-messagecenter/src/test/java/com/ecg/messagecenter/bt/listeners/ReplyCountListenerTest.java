package com.ecg.messagecenter.bt.listeners;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReplyCountListenerTest {
    @Mock
    private Message message;

    @Mock
    private HashMap<String,String> mockHeaders;

    @Mock
    private ResponseEntity<String> response;

    private ReplyCountListener replyCountListener;

    @Before
    public void initialize() {
        replyCountListener = new ReplyCountListener(true,"http://localhost:8080/boltapi/v1/ads/%s/statistics/actions/inc-counts");
    }

    @Test
    public void shouldIncrementReplyCount() throws  Exception{
        mockMessage();
        RestTemplate mockRestTemplate = mockRestTemplate();

        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        replyCountListener.messageProcessed(null, message);

        verify(mockRestTemplate,times(1)).exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),isA(Class.class));
    }

    @Test
    public void shouldHandleIncrementReplyCountException() throws  Exception{
        mockMessage();
        RestTemplate mockRestTemplate = mockRestTemplate();

        doThrow(RuntimeException.class).when(mockRestTemplate).exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),isA(Class.class));
        replyCountListener.messageProcessed(null, message);

        verify(mockRestTemplate,times(1)).exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class),isA(Class.class));
    }

    private void mockMessage() {
        when(message.getState()).thenReturn(MessageState.SENT);
        when(message.getHeaders()).thenReturn(mockHeaders);
        when(mockHeaders.get("X-Cust-Reply-Adid")).thenReturn("1234567890");
        when(mockHeaders.get("X-Cust-Conversation_Id")).thenReturn(null);
        when(mockHeaders.get("X-Cust-Locale")).thenReturn("es_MX_VNS");
    }

    private RestTemplate mockRestTemplate() throws IOException, NoSuchFieldException, IllegalAccessException {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

        when(mockRestTemplate.exchange(any(URI.class), any(HttpMethod.class), any(HttpEntity.class), isA(Class.class))).thenReturn((ResponseEntity) response);

        ReflectionTestUtils.setField(replyCountListener, "restTemplate", mockRestTemplate);

        return mockRestTemplate;
    }
}