package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.integration.test.AwaitMailSentProcessedListener.ProcessedMail;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BankAccountFilterAutomationTest {


    @Rule
    public ReplyTsIntegrationTestRule replyTS = new ReplyTsIntegrationTestRule();

    @Test
    public void firesOnBankAccount() {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("account", "99203205004989123456").attr("bankCode", "987654321").build());
        replyTS.registerConfig(BankAccountFilterFactory.class, JsonObjects.builder().attr("rules", rules).build());
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("1233343").htmlBody("hello world IBAN DE99 - 2032 0500 - 4989 - 1234 56 and code 987654321"));
        assertEquals(MessageState.HELD, processedMail.getMessage().getState());
    }

    @Test
    public void missesOnBankAccount() {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("account", "99203205004989123456").attr("bankCode", "987654321").build());
        replyTS.registerConfig(BankAccountFilterFactory.class, JsonObjects.builder().attr("rules", rules).build());
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("1233343").htmlBody("hello world IBAN DE99 - 2032 0500 - 4989 - 1234 56"));
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());
    }



    @Test
    public void skipsIfNoBankAccount() {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("account", "99203205004989123456").attr("bankCode", "987654321").build());
        replyTS.registerConfig(BankAccountFilterFactory.class, JsonObjects.builder().attr("rules", rules).build());
        ProcessedMail processedMail = replyTS.deliver(MailBuilder.aNewMail().from("foo@bar.com").to("bar@foo.com").adId("1233343").htmlBody("hello world"));
        assertEquals(MessageState.SENT, processedMail.getMessage().getState());
    }

}
