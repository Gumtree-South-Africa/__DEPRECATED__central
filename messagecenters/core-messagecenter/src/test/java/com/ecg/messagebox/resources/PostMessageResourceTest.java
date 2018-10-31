package com.ecg.messagebox.resources;

import com.ecg.messagebox.resources.requests.PostMessageRequest;
import com.ecg.replyts.core.runtime.persistence.kafka.QueueService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.HashMap;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostMessageResource.class)
public class PostMessageResourceTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private QueueService queueService;

    private InterruptedException interrupt = new InterruptedException("Simulating shutdown with interruptException in " + this.getClass().getName());
    private RuntimeException runtimeException = new RuntimeException("Some unexpected exception thrown intentionally in " + this.getClass().getName());

    private String someMessage = defaultMessage("some message");

    @Test
    public void interruptedPostReturns500() throws Exception {
        breakQueueServiceWithException(interrupt);

        mvc.perform(
                createProperPostMessageRequest()
        )
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void interruptedPostAttachmentReturns500() throws Exception {
        breakQueueServiceWithException(interrupt);

        mvc.perform(
                createProperPostMessageWithAttachmentRequest()
        )
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void runtimeExceptionReturns500() throws Exception {
        /**
         * We want to make sure a failed call to the queueService actually leads to a failed HTTP response.
         * This was previously not the case.
         * We are still not testing this for all kinds of RuntimeExceptions, as we kind-of analyzed (implementation specific
         * assumptions!) this is currently not an issue. I guess this is the best we can do.
         * Arguably this is already exaggerating, as we can not do with with every dependency in our system???
         */
        breakQueueServiceWithException(runtimeException);

        mvc.perform(
                createProperPostMessageWithAttachmentRequest()
        )
                .andExpect(status().isInternalServerError());
    }

    private RequestBuilder createProperPostMessageRequest() {
        return post("/users/2/conversations/conv1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(someMessage);
    }

    private RequestBuilder createProperPostMessageWithAttachmentRequest() {
        return fileUpload("/users/2/conversations/conv1/attachment")
                .file(new MockMultipartFile("message", "message", "application/json", someMessage.getBytes()))
                .file(new MockMultipartFile("attachment", "attachment", "value", new byte[1000]));
    }

    private static String defaultMessage(String content) {
        try {
            PostMessageRequest request = new PostMessageRequest();
            request.message = content;
            request.metadata = new HashMap<>();
            return new ObjectMapper().writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // make the queueService mock fail
    private void breakQueueServiceWithException(Exception e) throws Exception {
        Mockito.
                doThrow(e)
                .when(queueService)
                .publishSynchronously(anyString(), (Message) anyObject());

        Mockito.
                doThrow(e)
                .when(queueService)
                .publishSynchronously(anyString(), anyString(), anyObject());

        Mockito.
                doThrow(e)
                .when(queueService)
                .publishSynchronously(anyString(), (byte[]) anyObject());

    }
}
