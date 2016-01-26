package com.ecg.replyts.acceptance;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class ConversationResumingTest {

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void resumesConversations() {
        Conversation firstConversation = rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("123").plainBody("xxx")).getConversation();
        Conversation secondConversation= rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("123").plainBody("xxx")).getConversation();

        assertEquals(firstConversation.getId(), secondConversation.getId());
    }

    @Test
    public void resumeConversationsIfParticipantsAreSwapped() {
        Conversation firstConversation = rule.deliver(MailBuilder.aNewMail().from("bar@foo.com").to("foo@bar.com").adId("123").plainBody("xxx")).getConversation();
        Conversation secondConversation= rule.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("123").plainBody("xxx")).getConversation();

        assertEquals(firstConversation.getId(), secondConversation.getId());
    }

    @Test
    public void doesNotResumeConversationsIfArgsDiffer() {
        doesNotResume("foo@bar.com", "bar@foo.com", "123", "sco@bar.com", "bar@foo.com", "123"); // different from
        doesNotResume("foo@bar.com", "bar@foo.com", "123", "foo@bar.com", "plo@foo.com", "123"); // different to
        doesNotResume("foo@bar.com", "bar@foo.com", "123", "foo@bar.com", "bar@foo.com", "321"); // different ad id
        doesNotResume("foo@bar.com", "bar@foo.com", "123", "bar@foo.com", "foo@bar.com", "321"); // swapped to/from, different ad id
    }

    private void doesNotResume(String from1, String to1, String adId1, String from2, String to2, String adId2) {
        Conversation firstConversation = rule.deliver(MailBuilder.aNewMail().from(from1).to(to1).adId(adId1).plainBody("xxx")).getConversation();
        Conversation secondConversation= rule.deliver(MailBuilder.aNewMail().from(from2).to(to2).adId(adId2).plainBody("xxx")).getConversation();
        assertNotEquals(firstConversation.getId(), secondConversation.getId());
    }
}
