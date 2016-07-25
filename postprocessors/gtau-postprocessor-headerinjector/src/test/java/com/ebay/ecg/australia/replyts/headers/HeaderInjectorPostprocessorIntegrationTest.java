package com.ebay.ecg.australia.replyts.headers;

import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.internet.MimeMessage;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author mdarapour
 */
public class HeaderInjectorPostprocessorIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @BeforeClass
    public static void load() {
        System.setProperty( "replyts.header-injector.headers", "X-Cust-Http-Url,X-Cust-Http-Account-Name,X-Cust-Http-Account-Password" );
        System.setProperty( "replyts.header-injector.order", "100" );
    }

    @Test
    public void handlesHeadersWithNullValues() {
        Map<String, String> headers = rule.deliver(MailBuilder
                .aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")).getOutboundMail().getCustomHeaders();
        assertEquals("Header must be empty", 0, headers.size());
    }

    @Test
    public void injectsHeadersToOutgoingMail() throws Exception {
        Map<String, String> headers = rule.deliver(MailBuilder
                .aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId("213")
                .htmlBody("hello seller")
                .header("X-CUST-HTTP-URL", "http://localhost")
                .header("X-CUST-HTTP-ACCOUNT-NAME", "account")
                .header("X-CUST-HTTP-ACCOUNT-PASSWORD", "password")).getOutboundMail().getCustomHeaders();
        assertEquals("There must be two header values", 3, headers.size());
        assertEquals("http://localhost", headers.get("http-url"));
        assertEquals("account", headers.get("http-account-name"));
        assertEquals("password", headers.get("http-account-password"));

        MimeMessage mail = rule.waitForMail();
        assertEquals(1, mail.getHeader("X-Cust-Http-Url").length);
        assertEquals("http://localhost", mail.getHeader("X-Cust-Http-Url")[0]);
        assertEquals(1, mail.getHeader("X-Cust-Http-Account-Name").length);
        assertEquals("account", mail.getHeader("X-Cust-Http-Account-Name")[0]);
        assertEquals(1, mail.getHeader("X-Cust-Http-Account-Password").length);
        assertEquals("password", mail.getHeader("X-Cust-Http-Account-Password")[0]);
    }
}
