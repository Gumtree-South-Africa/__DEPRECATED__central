package com.ecg.de.ebayk.messagecenter.cleanup;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CleanupKijijiTemplateAdviceTest {
    private static final String RESKINNED_REPLY_TO_AD_EML = "reskinned-reply-to-ad.eml";
    private static final String RESKINNED_REPLY_TO_AD_EML_FR = "reskinned-reply-to-ad.fr.eml";
    private static final String RESKINNED_REPLY_TO_AD_NO_PHONE_EML = "reskinned-reply-to-ad_no-phone.eml";
    private static final String RESKINNED_REPLY_TO_AD_NO_PHONE_EML_FR = "reskinned-reply-to-ad_no-phone.fr.eml";
    private static final String RESKINNED_REPLY_TO_CONVERSATION_EML = "reskinned-reply-to-conversation.eml";
    private static final String RESKINNED_REPLY_TO_CONVERSATION_EML_FR = "reskinned-reply-to-conversation.fr.eml";

    private static final String EXPECTED_RESKINNED_REPLY_TO_AD =
            "Hey there hayley, I'm looking for an opportunity like this, I'm not a chef, but\n" +
            "have worked as a cook.\n" +
            "I have food handling.\n" +
            "And I've also got experience in a roadhouse.";


    @Test
    public void kijijiReplyTemplate_includesPhoneNumber_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_AD_EML);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

    @Test
    public void kijijiReplyTemplate_doesNotIncludePhoneNumber_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_AD_NO_PHONE_EML);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

    @Test
    public void kijijiFrenchReplyTemplate_includesPhoneNumber_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_AD_EML_FR);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

    @Test
    public void kijijiFrenchReplyTemplate_doesNotIncludePhoneNumber_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_AD_NO_PHONE_EML_FR);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

    @Test
    public void kijijiEnglishConversationReplyTemplate_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_CONVERSATION_EML);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

    @Test
    public void kijijiFrenchConversationTemplate_templatingRemoved() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("mailReceived"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_CONVERSATION_EML_FR);
        assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        assertNotNull(message);
        assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

}
