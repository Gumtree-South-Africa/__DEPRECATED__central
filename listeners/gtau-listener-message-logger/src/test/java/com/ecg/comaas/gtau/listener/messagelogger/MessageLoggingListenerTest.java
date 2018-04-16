package com.ecg.comaas.gtau.listener.messagelogger;


import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.ORPHANED;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MessageLoggingListenerTest {

    private MessageLoggingListener messageLoggingListener;

    @Mock
    private JdbcTemplate template;

    @Before
    public void setUp() {
        messageLoggingListener = new MessageLoggingListener("url", "user", "password", "250",
                "2048", "true", "true");
        messageLoggingListener.setTemplate(template);
    }

    @Test
    public void loggingListenerCallsJdbcTemplate() throws Exception {
        DefaultMutableConversation c = DefaultMutableConversation.create(aNewConversationCommand("convId").build());
        Message m = messageWithState(SENT);

        messageLoggingListener.messageProcessed(c, m);

        verify(template).update(anyString(), (Object[]) anyVararg());
    }

    @Test
    public void whenMessageIsOrphaned_shouldNotWriteToDb() throws Exception {
        Message m = messageWithState(ORPHANED);

        messageLoggingListener.messageProcessed(null, m);

        verify(template, never()).update(anyString(), (Object[]) anyVararg());
    }

    private static Message messageWithState(MessageState messageState) {
        return aMessage()
                .withId("msgId")
                .withMessageDirection(SELLER_TO_BUYER)
                .withState(messageState)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withTextParts(newArrayList(""))
                .build();
    }
}