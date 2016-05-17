package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Buyer;
import static com.ecg.replyts.core.api.model.conversation.ConversationRole.Seller;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;


public class DescriptionBuilderTest  {
    private static final BankAccountMatch MATCH = new BankAccountMatch("123456", "123456", 100);
    private static final String fromUserId = "123";
    private static final String toUserId = "456";

    @Mock private MailCloakingService mailCloakingService;
    @Mock private Conversation conversation;
    @Mock private Message message;

    private DescriptionBuilder descriptionBuilder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        descriptionBuilder = new DescriptionBuilder(mailCloakingService);
    }

    @Test
    public void descriptionContainsAllReportHeaders() {
        when(conversation.getCustomValues()).thenReturn(new HashMap<String, String>(){{
            put("from-userid", fromUserId);
            put("to-userid", toUserId);
        }});
        when(conversation.getBuyerId()).thenReturn("fraudster@mail.com");
        when(conversation.getSellerId()).thenReturn("victim@mail.com");
        when(conversation.getId()).thenReturn("987654");
        when(conversation.getMessages()).thenReturn(asList(message));
        when(mailCloakingService.createdCloakedMailAddress(Buyer, conversation)).thenReturn(new MailAddress("fraudster-anon@mail.marktplaats.nl"));
        when(message.getId()).thenReturn("message_1");
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(new HashMap<String, String>(){{
            put("Subject", "123456");
            put("From-Ip", "10.1.2.3");
            put("Reply-To", "f.r.audster@mail.com");
        }});

        String description = descriptionBuilder.build(conversation, MATCH, message, 1);

        assertThat(description, is("fraudster@mail.com|100|123456|fraudster-anon@mail.marktplaats.nl|f.r.audster@mail.com|10.1.2.3|victim@mail.com|987654|1|" + fromUserId + "|" + toUserId));
    }

    @Test
    public void descriptionContainsSimplifiedIpAddress() {
        when(conversation.getMessages()).thenReturn(asList(message));
        when(mailCloakingService.createdCloakedMailAddress(Buyer, conversation)).thenReturn(new MailAddress("fraudster-anon@mail.marktplaats.nl"));
        when(message.getId()).thenReturn("message_1");
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(new HashMap<String, String>(){{
            put("Subject", "123456");
            put("X-Originating-Ip", "[10.1.2.3]");
            put("From", "f.r.audster@mail.com");
        }});

        String description = descriptionBuilder.build(conversation, MATCH, message, 1);

        assertThat(description, containsString("|10.1.2.3|"));
    }

    @Test
    public void descriptionInFilterReasonIsCorrectWhenSellerIsFraudster() throws Exception {
        // Create the mails
//        final List<Mail> mails = new ArrayList<>(2);
//        mails.add(createMockMail(0, "automatisch@marktplaats.nl", "buyer@mail.com", "innocent"));
//        mails.add(createMockMailMail(1, "seller@mail.com", null, "give me money! 123.456"));

        when(message.getId()).thenReturn("message_1");
        when(message.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(message.getHeaders()).thenReturn(new HashMap<String, String>(){{
            put("Subject", "123456");
            put("Reply-To", "seller@mail.com");
        }});


        // Create the messages
        List<Message> messages = new ArrayList<>(2);
        messages.add(message);

//        messages.add(crateMockMessage(0, MessageDirection.BUYER_TO_SELLER));
//        messages.add(crateMockMessage(1, MessageDirection.SELLER_TO_BUYER));

        when(conversation.getCustomValues()).thenReturn(new HashMap<String, String>(){{
            put("from-userid", fromUserId);
            put("to-userid", toUserId);
        }});
        when(conversation.getBuyerId()).thenReturn("buyer@mail.com");
        when(conversation.getSellerId()).thenReturn("seller@mail.com");
        when(conversation.getId()).thenReturn("987654");
        when(conversation.getMessages()).thenReturn(messages);
        when(mailCloakingService.createdCloakedMailAddress(Seller, conversation)).thenReturn(new MailAddress("anonymous-seller@mail.marktplaats.nl"));

        String description = descriptionBuilder.build(conversation, MATCH, message, 1);

        assertThat(description, is("seller@mail.com|100|123456|anonymous-seller@mail.marktplaats.nl|seller@mail.com||buyer@mail.com|987654|1|456|123"));
    }

    @Test
    public void descriptionInFilterReasonIsCorrectWhenBuyerIsFraudster() throws Exception {
        when(conversation.getCustomValues()).thenReturn(new HashMap<String, String>(){{
            put("from-userid", fromUserId);
            put("to-userid", toUserId);
        }});
        when(conversation.getBuyerId()).thenReturn("buyer@mail.com");
        when(conversation.getSellerId()).thenReturn("seller@mail.com");
        when(conversation.getId()).thenReturn("987654");
        when(conversation.getMessages()).thenReturn(asList(message));

        when(message.getId()).thenReturn("message_1");
        when(message.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(message.getHeaders()).thenReturn(new HashMap<String, String>(){{
            put("Subject", "123456");
            put("Reply-To", "buyer@mail.com");
        }});

        when(mailCloakingService.createdCloakedMailAddress(Buyer, conversation)).thenReturn(new MailAddress("anonymous-buyer@mail.marktplaats.nl"));

        String description = descriptionBuilder.build(conversation, MATCH, message, 1);

        assertThat(description, is("buyer@mail.com|100|123456|anonymous-buyer@mail.marktplaats.nl|buyer@mail.com||seller@mail.com|987654|1|123|456"));
    }

}