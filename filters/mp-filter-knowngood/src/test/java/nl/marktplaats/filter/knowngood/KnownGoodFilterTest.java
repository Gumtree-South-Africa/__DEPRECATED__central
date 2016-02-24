package nl.marktplaats.filter.knowngood;

import com.ecg.replyts.core.api.model.conversation.*;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;

import static com.ecg.replyts.core.api.model.conversation.MessageDirection.BUYER_TO_SELLER;
import static com.ecg.replyts.core.api.model.conversation.MessageDirection.SELLER_TO_BUYER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

public class KnownGoodFilterTest {

    private KnownGoodFilter userFilter;

    @Before
    public void setup() {
        userFilter = new KnownGoodFilter("someFilterName", new KnownGoodFilterConfig());
    }

    @Test
    public void testKnownGoodBuyerFirstMessage() {
        MessageProcessingContext messageProcessingContext =
                prepareMockContextWithHeader(BUYER_TO_SELLER, "initiatorgood");

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reason.getResultState()).isEqualTo(FilterResultState.ACCEPT_AND_TERMINATE);
        assertThat(reason.getDescription()).isEqualTo("Sender is known good: KNOWN_USER");
    }

    @Test
    public void testKnownGoodBuyerFirstReply() {
        MessageProcessingContext messageProcessingContext =
                prepareMockContextWithHeader(SELLER_TO_BUYER, "initiatorgood");

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testKnownGoodSellerFirstReply() {
        MessageProcessingContext messageProcessingContext =
                prepareMockContextWithHeader(SELLER_TO_BUYER, "respondergood");

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(1);
        FilterFeedback reason = reasons.get(0);
        assertThat(reason.getDescription()).isEqualTo("Sender is known good: KNOWN_USER");
    }

    @Test
    public void testKnownGoodSellerFirstMessage() {
        MessageProcessingContext messageProcessingContext =
                prepareMockContextWithHeader(BUYER_TO_SELLER, "sellergood");

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testUnknownBuyerFirstReply() {
        MessageProcessingContext messageProcessingContext = prepareMockContext(BUYER_TO_SELLER);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    @Test
    public void testUnknownSellerFirstMessage() {
        MessageProcessingContext messageProcessingContext = prepareMockContext(SELLER_TO_BUYER);

        List<FilterFeedback> reasons = userFilter.filter(messageProcessingContext);

        assertThat(reasons.size()).isEqualTo(0);
    }

    private MessageProcessingContext prepareMockContext(MessageDirection messageDirection) {
        return prepareMockContextWithHeaders(messageDirection, Collections.emptyMap());
    }

    private MessageProcessingContext prepareMockContextWithHeader(MessageDirection messageDirection, String headerName) {
        return prepareMockContextWithHeaders(messageDirection, Collections.singletonMap(headerName, "KNOWN_USER"));
    }

    private MessageProcessingContext prepareMockContextWithHeaders(MessageDirection messageDirection, Map<String, String> headers) {
        Mail mail = mockMail();
        Message message = mockMessage(messageDirection);
        MutableConversation conversation = mockConversation(headers);
        return mockMessageProcessingContext(mail, message, conversation);
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

    private MutableConversation mockConversation(Map<String, String> headers) {
        MutableConversation conversation = mock(MutableConversation.class);
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