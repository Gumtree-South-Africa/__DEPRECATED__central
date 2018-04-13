package com.ecg.comaas.kjca.coremod.shared;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommandBuilder;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TextAnonymizerTest {
    @Mock
    private MailCloakingService cloakingDevice;

    private TextAnonymizer textAnonymizer;
    private DefaultMutableConversation conversation;

    @Before
    public void setUp() throws Exception {
        textAnonymizer = new TextAnonymizer(cloakingDevice);
        conversation = DefaultMutableConversation.create(
                NewConversationCommandBuilder
                        .aNewConversationCommand("cid")
                        .withAdId("adid")
                        .withBuyer("buyer@example.com", "abc123")
                        .withSeller("seller@example.com", "def321")
                        .build()
        );

        when(cloakingDevice.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("abc123@cloaked"));
        when(cloakingDevice.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress("def321@cloaked"));
    }

    @Test
    public void noEmailPresent_notAnonymized() throws Exception {
        String content = "no buyer or seller email here. random@example.com ";
        String result = textAnonymizer.anonymizeText(conversation, content);

        assertEquals(content, result);
    }

    @Test
    public void buyerEmailPresent_anonymized() throws Exception {
        String content = "buyer email buyer@example.com present";
        String result = textAnonymizer.anonymizeText(conversation, content);

        assertEquals("buyer email abc123@cloaked present", result);
    }

    @Test
    public void sellerEmailPresent_anonymized() throws Exception {
        String content = "seller email seller@example.com present";
        String result = textAnonymizer.anonymizeText(conversation, content);

        assertEquals("seller email def321@cloaked present", result);
    }

    @Test
    public void bothEmailsPresentMultipleTimes_anonymized() throws Exception {
        String content = "buyer email 1: buyer@example.com . seller email 1: seller@example.com\n" +
                "buyer email 2: buyer@example.com . seller email 2: seller@example.com";
        String result = textAnonymizer.anonymizeText(conversation, content);

        assertEquals("buyer email 1: abc123@cloaked . seller email 1: def321@cloaked\n" +
                "buyer email 2: abc123@cloaked . seller email 2: def321@cloaked", result);
    }
}
