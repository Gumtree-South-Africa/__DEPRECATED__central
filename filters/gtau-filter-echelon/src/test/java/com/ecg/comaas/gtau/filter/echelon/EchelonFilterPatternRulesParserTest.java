package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author mdarapour
 */
public class EchelonFilterPatternRulesParserTest {
    @Test(expected = IllegalArgumentException.class)
    public void rejectsEmptJsonObject() {
        EchelonFilterPatternRulesParser.getConfig(JsonObjects.parse("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullEndpoint() {
        EchelonFilterPatternRulesParser.getConfig(JsonObjects.parse("{'endpointUrl':''}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNullEndpointTimeout() {
        EchelonFilterPatternRulesParser.getConfig(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:null}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidEndpointTimeout() {
        EchelonFilterPatternRulesParser.getConfig(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:'2sec'}"));
    }

    @Test
    public void createsEchelonConfig() {
        EchelonFilterConfiguration config = EchelonFilterPatternRulesParser.getConfig(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:1}"));
        assertNotNull(config);

        assertEquals("foo.com", config.getEndpointUrl());
        assertEquals(1, config.getEndpointTimeout());
        assertEquals(0, config.getScore());
    }
}
