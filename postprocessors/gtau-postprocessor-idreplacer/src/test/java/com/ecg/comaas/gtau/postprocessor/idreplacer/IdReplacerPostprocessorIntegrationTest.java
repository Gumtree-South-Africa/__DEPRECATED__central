package com.ecg.comaas.gtau.postprocessor.idreplacer;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdReplacerPostprocessorIntegrationTest {

    private static final String AD_ID = "106";
    private static final String MESSAGE_ID_FORMAT = "<%%MESSAGE_ID%%>";
    private static final String CONVERSATION_ID_FORMAT = "<%%CONVERSATION_ID%%>";
    private static final String HASH_FORMAT = "<%%HASH%%>";
    private static final Mac HMAC_MD5;
    private static final String SECRET_SALT = "X23!=?m(";

    static {
        String algorithm = "HmacMD5";
        try {
            HMAC_MD5 = Mac.getInstance(algorithm);
            final SecretKeySpec key = new SecretKeySpec(SECRET_SALT.getBytes(), algorithm);
            HMAC_MD5.init(key);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid Key: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 provider not found: " + e.getMessage());
        }
    }

    @BeforeClass
    public static void load() {
        System.setProperty("replyts-id-replacer.plugin.order", "250");
    }

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule();

    @Test
    public void testPrepare() throws Exception {
        String body = String.format("raspd=%s_=_%s_=_%s_=_%s", AD_ID, CONVERSATION_ID_FORMAT, MESSAGE_ID_FORMAT, HASH_FORMAT);

        MailInterceptor.ProcessedMail processedMail = rule.deliver(MailBuilder
                .aNewMail()
                .from("buyer@foo.com")
                .to("seller@bar.com")
                .adId(AD_ID)
                .htmlBody(body)
                .header("header", "test"));

        List<TypedContent<String>> contents = processedMail.getOutboundMail().getTextParts(false);

        for (TypedContent<String> content : contents) {
            assertTrue("Content is empty", !content.getContent().isEmpty());
            // Remove 'raspd' then tokenize
            String[] tokens = Pattern.compile("_=_").split(content.getContent().substring(6));
            assertEquals("4 tokens are expected", 4, tokens.length);
            assertEquals("AD ID", AD_ID, tokens[0]);
            assertFalse("Conversation Id was not replaced", Pattern.compile(CONVERSATION_ID_FORMAT).matcher(content.getContent()).find());
            assertFalse("Message Id was not replaced", Pattern.compile(MESSAGE_ID_FORMAT).matcher(content.getContent()).find());
            assertFalse("Hash was not replaced", Pattern.compile(HASH_FORMAT).matcher(content.getContent()).find());
            // AD ID + CONVERSATION ID + MESSAGE ID
            assertEquals(expectedHash(tokens[0] + tokens[1] + tokens[2]), tokens[3]);
        }
    }

    private static String expectedHash(final String input) {
        final byte[] test = HMAC_MD5.doFinal(input.getBytes());
        return Base64.encodeBase64URLSafeString(test);
    }
}
