package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters.iprisk;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * User: acharton
 * Date: 12/18/12
 */
public class IpLevelConfigParserTest {
    private IpLevelConfigParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new IpLevelConfigParser((ObjectNode) JsonObjects.parse("{'VERY_BAD': 100,\n" +
                "    'BAD': 100,\n" +
                "    'MEDIUM_BAD': 0,\n" +
                "    'GOOD' : 0}"));
    }

    @Test
    public void mappedValuesSizeIs4() throws Exception {
        assertEquals(4, parser.parse().size());
    }

    @Test
    public void checkBadValue() throws Exception {
        assertEquals(100, parser.parse().get("BAD").intValue());

    }
}
