package com.ecg.replyts.app.postprocessorchain.postprocessors;


import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.MutableConversation;
import com.ecg.replyts.core.api.model.conversation.command.AddMessageCommand;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.mailparser.MessageIdHeaderEncryption;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class MessageIdPreparatorTest {

    @Mock
    private ProcessingTimeGuard processingTimeGuard;

    @Test
    public void messageIdAndReferencesAndInReplyToHeadersGenerated() throws Exception {
        // Set up a conversation with a three messages.
        Map<String, String> msg1Headers = new HashMap<String, String>();
        msg1Headers.put("Delivered-To", "foo@bar.com");
        msg1Headers.put("Message-ID", "<ABC@abc.com>");
        Map<String, String> msg2Headers = new HashMap<String, String>();
        msg2Headers.put("Delivered-To", "foo@bar.com");
        msg1Headers.put("Message-ID", "<STU@xyz.com>");
        msg1Headers.put("References", "<" + new MessageIdHeaderEncryption().encrypt("1:1") + "@test-domain.com>");
        Map<String, String> msg3Headers = new HashMap<String, String>();
        msg3Headers.put("Message-ID", "<DEF@abc.com>");
        msg3Headers.put("Delivered-To", "foo@bar.com");
        msg1Headers.put("References", "<" + new MessageIdHeaderEncryption().encrypt("2:2") + "@test-domain.com>");

        MutableConversation conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "1", "1", "b@abc.com", "s@xyz.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        conversation.applyCommand(new AddMessageCommand(
                "1", "1:1", MessageState.SENT, MessageDirection.BUYER_TO_SELLER, new DateTime().minusHours(2), "<ABC@abc.com>", null, msg1Headers, emptyList(), emptyList()));
        conversation.applyCommand(new AddMessageCommand(
                "1", "2:2", MessageState.SENT, MessageDirection.SELLER_TO_BUYER, new DateTime().minusHours(1), "<STU@xyz.com>", "1:1", msg2Headers, emptyList(), emptyList()));
        conversation.applyCommand(new AddMessageCommand(
                "1", "3:3", MessageState.SENT, MessageDirection.BUYER_TO_SELLER, new DateTime(), "<DEF@abc.com>", "2:2", msg3Headers, emptyList(), emptyList()));

        // The third message is coming in from the seller to the buyer.
        Mail mail = new Mails().readMail(ByteStreams.toByteArray(getClass().getResourceAsStream("MessageIdPreparatorTest-mail.eml")));

        // Preparing the third message.
        MessageProcessingContext context = new MessageProcessingContext(mail, "3:3", processingTimeGuard);
        context.setConversation(conversation);

        String[] domains = {"test-domain.com"};
        new MessageIdPreparator(new MessageIdGenerator(domains)).postProcess(context);

        assertThat(context.getOutgoingMail().getUniqueHeader("Message-ID"), is(not("<DEF@abc.com>")));
        String mimeMessageId = context.getOutgoingMail().getUniqueHeader("Message-ID");
        assertThat(mimeMessageId, startsWith("<"));
        assertThat(mimeMessageId, endsWith("@test-domain.com>"));
        String encrypted = mimeMessageId.substring(1, mimeMessageId.length() - "@test-domain.com>".length());
        String decrypted = new MessageIdHeaderEncryption().decrypt(encrypted);
        assertThat("Message-ID header must contain encrypted message id", decrypted, is("3:3"));

        assertThat("References header must contain Message-ID of message replied to", context.getOutgoingMail().getUniqueHeader("References"), is("<STU@xyz.com>"));
        assertThat("In-Reply-To header must contain Message-ID of message replied to", context.getOutgoingMail().getUniqueHeader("In-Reply-To"), is("<STU@xyz.com>"));
    }

    @Test
    public void messageIdHeaderGeneratedReferencesHeaderNot() throws Exception {
        // Set up a conversation with a two messages.
        Map<String, String> msg1Headers = new HashMap() {{ put("Message-ID", "<ABC@abc.com>"); }};
        Map<String, String> msg2Headers = new HashMap() {{ put("Message-ID", "<STU@xyz.com>"); }};

        MutableConversation conversation = DefaultMutableConversation.create(new NewConversationCommand(
                "1", "1", "b@abc.com", "s@xyz.com", "abc123", "xyz809", new DateTime(), ConversationState.ACTIVE, new HashMap()));
        conversation.applyCommand(new AddMessageCommand(
                "1", "1:1", MessageState.SENT, MessageDirection.BUYER_TO_SELLER, new DateTime().minusHours(2), "<ABC@abc.com>", null, msg1Headers, emptyList(), emptyList()));
        conversation.applyCommand(new AddMessageCommand(
                "1", "2:2", MessageState.SENT, MessageDirection.SELLER_TO_BUYER, new DateTime().minusHours(1), "<STU@xyz.com>", null, msg2Headers, emptyList(), emptyList()));

        // The second message
        Mail mail = new Mails().readMail(ByteStreams.toByteArray(getClass().getResourceAsStream("MessageIdPreparatorTest-mail.eml")));

        // Preparing the second message.
        MessageProcessingContext context = new MessageProcessingContext(mail, "2:2", processingTimeGuard);
        context.setConversation(conversation);

        String[] domains = {"test-domain.com"};
        new MessageIdPreparator(new MessageIdGenerator(domains)).postProcess(context);

        assertNotNull(context.getOutgoingMail().getUniqueHeader("Message-ID"));
        assertNull(context.getOutgoingMail().getUniqueHeader("References"));
        assertNull(context.getOutgoingMail().getUniqueHeader("In-Reply-To"));
    }

}
