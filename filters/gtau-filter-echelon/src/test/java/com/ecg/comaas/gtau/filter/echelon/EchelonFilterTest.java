package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author mdarapour
 */
@RunWith(MockitoJUnitRunner.class)
public class EchelonFilterTest {
    public static final String ENDPOINT_URL = "http://localhost/";
    public static final int ENDPOINT_TIMEOUT = 1;
    public static final int SCORE = 50;

    @Mock
    private MessageProcessingContext mpc;

    @Mock
    private Message msg;

    @Mock
    private Conversation conversation;

    @Mock
    private Mail mail;

    @Before
    public void setUp() throws Exception {
        when(msg.getId()).thenReturn("msgid1");
        when(mpc.getMessage()).thenReturn(msg);
        when(msg.getPlainTextBody()).thenReturn("Hello Echelon!");
        when(mpc.getConversation()).thenReturn(conversation);
        when(mpc.getMail()).thenReturn(Optional.of(mail));
        when(conversation.getId()).thenReturn("cid");
        when(mail.getSubject()).thenReturn("this is the subject");
    }

    @Test
    public void ignoresNonBuyerToSellerConversation() {
        List<FilterFeedback> fbs = new EchelonFilter(new EchelonFilterConfiguration(ENDPOINT_URL, ENDPOINT_TIMEOUT, SCORE)).filter(mpc);
        assertEquals(0, fbs.size());
    }

    @Test
    public void capturesFirstMessageOnly() {
        List<Message> messages = new ArrayList<>();
        when(mpc.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        messages.add(msg);
        messages.add(mock(Message.class));

        when(conversation.getMessages()).thenReturn(messages);
        List<FilterFeedback> fbs = new EchelonFilter(new EchelonFilterConfiguration(ENDPOINT_URL, ENDPOINT_TIMEOUT, SCORE)).filter(mpc);
        assertEquals(0, fbs.size());
    }
}
