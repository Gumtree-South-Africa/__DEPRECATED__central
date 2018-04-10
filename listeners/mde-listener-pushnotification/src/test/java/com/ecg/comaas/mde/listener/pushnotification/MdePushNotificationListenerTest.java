package com.ecg.comaas.mde.listener.pushnotification;

import com.ecg.comaas.mde.listener.pushnotification.cassandra.CassandraMessageRepository;
import com.ecg.comaas.mde.listener.pushnotification.model.Message;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import static java.util.Optional.of;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class MdePushNotificationListenerTest {
    @InjectMocks
    private MdePushNotificationListener listener;

    @Mock
    private CassandraMessageRepository cassandraRepository;

    @Mock
    private NotificationSender notificationSender;

    private Gson gson = new Gson();

    @Before
    public void setUp() {
        initMocks(this);
        Message message = new Gson().fromJson(mbMessage, Message.class);
        when(cassandraRepository.getLastMessage(any(), any())).thenReturn(of(message));
    }

    @Test
    public void listenerSendsPushNotification() throws IOException {
        Conversation conversation = getConversation("/messages_buyer_to_seller.json");
        listener.messageProcessed(conversation, Iterables.getLast(conversation.getMessages()));
        MdePushMessagePayload expected = gson.fromJson(resultingPayload, MdePushMessagePayload.class);
        Mockito.verify(notificationSender, times(1)).send(expected);
    }

    @Test
    public void noPushNotificationForOwnMessage() throws IOException {
        Conversation conversation = getConversation("/messages_seller_to_buyer.json");
        listener.messageProcessed(conversation, Iterables.getLast(conversation.getMessages()));
        Mockito.verifyZeroInteractions(notificationSender);
    }

    private Conversation getConversation(String messagesFileName) throws IOException {
        Conversation conversation = gson.fromJson(loadFileAsString("/conversation.json"), ImmutableConversation.class);
        ImmutableMessage[] messages = gson.fromJson(loadFileAsString(messagesFileName), ImmutableMessage[].class);
        return ImmutableConversation.Builder.aConversation(conversation)
                .withMessages(Arrays.asList(messages))
                .withCreatedAt(new DateTime())
                .build();
    }

    private String resultingPayload = "{'conversationId':'b:2srfkgv:287p61161','adId':'4022','message':'Real message','customerId':'222022','title':'Audi A4'}";

    private String mbMessage = "{'id':'979a18e0-203a-11e7-90a3-0fc2760c1d14','type':'EMAIL','metadata':{'text':'Real message','senderUserId':'111022'}}";

    private String loadFileAsString(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }
    }
}
