package com.ecg.messagecenter.kjca.pushmessage.send.client;

import com.ecg.messagecenter.kjca.pushmessage.send.model.SendMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.InputStream;

public class SendMessageCommand extends FailureAwareCommand<SendMessage> {

    private static final String SEND_MESSAGE_BASE_URL = "/notifications";
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public SendMessageCommand(HttpClient httpClient, HttpHost httpHost, SendMessage messageRequest, int hystrixTimeout) {
        super(httpClient, httpHost, hystrixTimeout);

        this.request = new HttpPost(SEND_MESSAGE_BASE_URL);
        try {
            final String json = objectMapper
                    .writerWithView(SendMessage.Views.Request.class)
                    .writeValueAsString(messageRequest);
            final StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            ((HttpPost) this.request).setEntity(entity);
        } catch (IOException e) {
            throw new SendException(SendException.Cause.PARSE, "Could not create the POST request for the message", e);
        }
    }

    @Override
    protected SendMessage successCallback(InputStream responseContent) throws IOException {
        return objectMapper.readerWithView(SendMessage.Views.Response.class)
                .forType(SendMessage.class)
                .readValue(responseContent);
    }

    @Override
    protected String exceptionMessageTemplate() {
        return "{} error occurred while sending message.";
    }
}
