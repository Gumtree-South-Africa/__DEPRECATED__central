package com.ecg.messagecenter.core.cleanup.gtau;

import com.ecg.messagecenter.core.cleanup.gtau.TextCleaner;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * Unit test for CleanupGumtreeDealerLeadAdvice
 */
public class CleanupGumtreeDealerLeadAdviceTest {

    private static final String DEALER_LEAD_EML = "dealer-lead.eml";

    private static final String EXPECTED_DEALER_LEAD
          = "Name: Dilbert Magoo\n" +
            "Email: dilbert@ebay.com\n" +
            "Phone: 04 324 32455\n" +
            "Enquiry: Hi MobileDealer, I'm interested in your \"Post Ad in Cars Category, for...\" on Gumtree.\n" +
            "Is there anything I need to know about it? When can I inspect it? Please contact me. Thanks!";


    @Test
    public void testDealerLeadEmail() throws Exception {

        // Setup
        // use this way to get reference to folder because parent directories in Jenkins might have spaces in them
        File mailFolder = FileUtils.toFile(getClass().getResource("plain"));
        File dealerLeadEmail = new File(mailFolder, DEALER_LEAD_EML);
        Assert.assertNotNull(dealerLeadEmail);

        String dealerLeadEmailText = FileUtils.readFileToString(dealerLeadEmail);

        // Test
        String message = TextCleaner.cleanupText(dealerLeadEmailText);

        // Verify
        Assert.assertNotNull(message);
        Assert.assertEquals(EXPECTED_DEALER_LEAD, message);
    }
}
