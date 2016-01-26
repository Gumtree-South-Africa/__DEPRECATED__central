package com.ecg.replyts.core.api.model.mail;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: gdibella
 * Date: 9/3/13
 */
public class MailAddressTest {
    @Test
    public void testMailContainsGivenDomains() throws Exception {
        assertTrue(new MailAddress("Seller.1234@kijiji.it").isFromDomain(new String[]{"kijiji.it", "ebay.com"}));
        assertTrue(new MailAddress("Seller.1234@ebay.com").isFromDomain(new String[]{"kijiji.it", "ebay.com"}));
    }

    @Test
    public void testInvalidDomainGiven() throws Exception {
        assertFalse(new MailAddress("Seller.1234@ecg.com").isFromDomain(new String[]{"kijiji.it", "ebay.com"}));
    }


}
