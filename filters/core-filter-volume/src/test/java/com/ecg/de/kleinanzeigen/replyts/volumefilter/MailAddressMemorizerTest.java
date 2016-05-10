package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MailAddressMemorizerTest {


    private MailAddressMemorizer mailAddressMemorizer;


    @Before
    public void setUp() {
        mailAddressMemorizer = new MailAddressMemorizer(1000000, 100);
    }

    @Test
    public void hasNotSeenWhenNotInside() {

        assertFalse(mailAddressMemorizer.couldBeSeenAlready("foo@bar.com"));
    }

    @Test
    public void hasBeenSeenAfterMarked() {
        mailAddressMemorizer.mark("foo@bar.com");
        assertTrue(mailAddressMemorizer.couldBeSeenAlready("foo@bar.com"));
    }

    @Test
    public void isCaseInsensitive() {
        mailAddressMemorizer.mark("foo@BAR.com");
        assertTrue(mailAddressMemorizer.couldBeSeenAlready("foo@bar.com"));

    }

    @Test
    public void forgetsMailAfterExpiration() throws InterruptedException {
        mailAddressMemorizer = new MailAddressMemorizer(1, 100);
        mailAddressMemorizer.mark("foo@bar.com");
        Thread.sleep(2);
        assertFalse(mailAddressMemorizer.couldBeSeenAlready("foo@bar.com"));
    }
}
