package com.ecg.replyts.core.runtime.indexer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class MessageDocumentIdTest {
    @Test
    public void buildsIdCorrectly() throws Exception {
        assertEquals("convid/messageid", new MessageDocumentId("convid", "messageid").build());
    }

    @Test
    public void parsesCorrectly() throws Exception {
        assertEquals(new MessageDocumentId("convid", "messageid"), MessageDocumentId.parse("convid/messageid"));

    }
}
