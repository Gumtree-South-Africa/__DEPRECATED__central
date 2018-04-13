package com.ecg.comaas.core.filter.ebayservices.ip2country;

import com.ecg.comaas.core.filter.ebayservices.ip2country.CountryRulesParser;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * User: acharton
 * Date: 12/18/12
 * Time: 10:31 AM
 */
public class CountryRulesParserTest {

    private CountryRulesParser crp;

    @Before
    public void setUp() throws Exception {
        crp = new CountryRulesParser((ObjectNode) JsonObjects.parse("{'DEFAULT': '100', 'DE':0, 'UK':200}"));

    }

    @Test
    public void readDefaultFromConfig() throws Exception {
        assertEquals(100, crp.getDefaultScore());
    }

    @Test
    public void readCountryLowerCase() throws Exception {
        assertEquals(0, crp.getCountryScores().get("de").intValue());
    }

    @Test
    public void readConutryIgnoreUpperCase() throws Exception {
        assertNull(crp.getCountryScores().get("DE"));
    }
}
