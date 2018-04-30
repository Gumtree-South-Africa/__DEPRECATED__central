package com.ecg.messagecenter.core.cleanup.gtau;

import com.ecg.messagecenter.core.cleanup.gtau.TextCleaner;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Unit test for CleanupGumtreeReplyToAdAdvice
 */
public class CleanupGumtreeReplyToAdAdviceTest {

    private static final String RESKINNED_REPLY_TO_AD_EML = "reskinned-reply-to-ad.eml";

    private static final String EXPECTED_RESKINNED_REPLY_TO_AD
          = "Hey there hayley, I'm looking for an opportunity like this, I'm not a chef, but\n" +
            "have worked as a cook.\n" +
            "I have food handling.\n" +
            "And I've also got experience in a roadhouse.";



    @Test
    public void testGetReplyToAdMessageOK() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("plain"));
        File replyToAdEmail = new File(mailFolder, RESKINNED_REPLY_TO_AD_EML);
        Assert.assertNotNull(replyToAdEmail);

        String replyToAdEmailText = FileUtils.readFileToString(replyToAdEmail);

        // Test
        String message = TextCleaner.cleanupText(replyToAdEmailText);

        // Verify
        Assert.assertNotNull(message);
        Assert.assertEquals(EXPECTED_RESKINNED_REPLY_TO_AD, message);
    }

}
