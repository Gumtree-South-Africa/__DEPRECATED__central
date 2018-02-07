package com.ecg.au.gumtree.messagelogging;


import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static com.ecg.replyts.core.api.model.conversation.MessageState.SENT;
import static com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder.aNewConversationCommand;
import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class MessageLoggingListenerTest {

    @Mock
    private JdbcTemplate template;

    @Test
    public void loggingListenerCallsJdbcTemplate() throws Exception {
        MessageLoggingListener l = new MessageLoggingListener("com.mysql.jdbc.Driver", "jdbc:mysql://127.0.0.1:3306/replyts", "user", "password");
        l.setTemplate(template);

        DefaultMutableConversation c = DefaultMutableConversation.create(aNewConversationCommand("convId").build());
        Message m = aMessage()
                .withId("msgId")
                .withMessageDirection(SELLER_TO_BUYER)
                .withState(SENT)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withTextParts(newArrayList(""))
                .build();

        l.messageProcessed(c, m);

        verify(template).update(anyString(), (Object[]) anyVararg());
    }
}