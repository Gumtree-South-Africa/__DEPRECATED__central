package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;


/**
 * Created by reweber on 16/10/15
 */
public class KnownGoodFilterTest {

    private KnownGoodFilter userFilter;

    @Before
    public void setup() {

        userFilter = new KnownGoodFilter("someFilterName", new KnownGoodFilterConfig());
    }


    @Test
    public void testKnownGoodBuyerFirstMessage() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("initiatorgood", "KNOWN_USER");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reason.getResultState()).isEqualTo(FilterResultState.ACCEPT_AND_TERMINATE);
        assertThat(reason.getDescription()).isEqualTo("Sender is known good: KNOWN_USER");

    }

    @Test
    public void testKnownGoodBuyerFirstReply() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("initiatorgood", "KNOWN_USER");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);

    }

    @Test
    public void testKnownGoodSellerFirstReply() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("respondergood", "KNOWN_USER");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reason.getDescription()).isEqualTo("Sender is known good: KNOWN_USER");

    }

    @Test
    public void testKnownGoodSellerFirstMessage() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("sellergood", "KNOWN_USER");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);

    }

    @Test
    public void testUnknownBuyerFirstReply() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.BUYER_TO_SELLER);
        MutableConversation conversation = mockConversation("", "");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);

    }

    @Test
    public void testUnknownSellerFirstMessage() {

        Mail mail = mockMail();
        Message message = mockMessage(MessageDirection.SELLER_TO_BUYER);
        MutableConversation conversation = mockConversation("", "");
        MessageProcessingContext messageProcessingContext = mockMessageProcessingContext(mail, message, conversation);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);

    }

    private Message mockMessage(MessageDirection direction) {
        Message message = mock(Message.class);
        when(message.getMessageDirection()).thenReturn(direction);
        return message;
    }

    private Mail mockMail() {
        Mail mail = mock(Mail.class);
        when(mail.getFrom()).thenReturn("user@hotmail.com");
        return mail;
    }

    private MutableConversation mockConversation(String key, String value) {
        MutableConversation conversation = mock(MutableConversation.class);
        Map<String, String> headers = new HashMap<>();
        headers.put(key, value);
        when(conversation.getCustomValues()).thenReturn(headers);
        return conversation;
    }

    private MessageProcessingContext mockMessageProcessingContext(Mail mail, Message message, MutableConversation conversation) {
        MessageProcessingContext messageProcessingContext = mock(MessageProcessingContext.class);
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(messageProcessingContext.getMessage()).thenReturn(message);
        when(messageProcessingContext.getMail()).thenReturn(mail);
        return messageProcessingContext;
    }
}