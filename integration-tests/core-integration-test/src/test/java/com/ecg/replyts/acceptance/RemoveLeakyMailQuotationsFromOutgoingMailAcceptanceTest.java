package com.ecg.replyts.acceptance;

import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class RemoveLeakyMailQuotationsFromOutgoingMailAcceptanceTest {

    @Rule
    public ReplyTsIntegrationTestRule testRule = new ReplyTsIntegrationTestRule();


    @Test
    public void removesConfiguredPatterns() throws Exception {
        MailInterceptor.ProcessedMail result = testRule.deliver(MailBuilder.aNewMail().adId("123").from("oo@as.com").to("as@sdf.com").subject("asd").htmlBody(
                "Helloobfuscator-asdf-pattern-0MyDearobfuscator-234234-PATTERN-1World"
        ));
        String text = result.getOutboundMail().getPlaintextParts().get(0);
        assertEquals("HelloMyDearWorld", text);
    }

    @Test
    public void removesConfiguredPatternsInAllTextParts() throws Exception {
        MailInterceptor.ProcessedMail result = testRule.deliver(MailBuilder.aNewMail().adId("123").from("oo@as.com").to("as@sdf.com").subject("asd")
                .plainBody("Fooobfuscator-asdf-pattern-1Bar")
                .htmlBody("Helloobfuscator-asdf-pattern-0MyDearobfuscator-234234-PATTERN-1World"));
        String text = result.getOutboundMail().getPlaintextParts().get(0);
        assertEquals("FooBar", text);
        text = result.getOutboundMail().getPlaintextParts().get(1);
        assertEquals("HelloMyDearWorld", text);
    }

    @Test
    public void removesRealWordPatternWithBackslashes() {
        MailInterceptor.ProcessedMail deliver = testRule.deliver(MailBuilder.aNewMail().adId("123").from("foo@bar.com").to("kj@hg.com").htmlBody("To: fo.o@bar.com"));
        assertEquals("", deliver.getOutboundMail().getPlaintextParts().get(0));
    }
}
