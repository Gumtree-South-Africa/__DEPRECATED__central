package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.userstate;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * User: acharton
 * Date: 12/18/12
 */
public class UserStateConfigParserTest {
    private UserStateConfigParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new UserStateConfigParser((ObjectNode) JsonObjects.parse("{\n" +
                "    'UNKNOWN': 0,\n" +
                "    'CONFIRMED': -50,\n" +
                "    'SUSPENDED': 100\n" +
                "}"));
    }

    @Test
    public void testSizeOkay() throws Exception {
        assertEquals(3, parser.parse().size());
    }

    @Test
    public void scoreMappingConfirmed() throws Exception {
        assertEquals(-50, parser.parse().get("CONFIRMED").intValue());

    }
}
