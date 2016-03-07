package com.ecg.de.mobile.replyts.uniqueid;

import com.google.common.collect.ImmutableSet;
import junit.framework.TestCase;

public class MailUniqueIdPostProcessorTest extends TestCase {

    public void testGetIgnoredEmailAddresses() throws Exception {

        MailUniqueIdPostProcessor mailUniqueIdPostProcessor = new MailUniqueIdPostProcessor(new UniqueIdGenerator("foo"), "foo@foo.com, a@b.com", "999");

        assertEquals(ImmutableSet.of("foo@foo.com", "a@b.com"), mailUniqueIdPostProcessor.getIgnoredEmailAddresses());

    }
}