package com.ecg.de.mobile.replyts.demand;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;


public class UtilsTest {

    @Test
    public void parsesNullInputForAbTestMapToEmptyResult() throws Exception {
        Map<String, String> actual = Utils.parseAbTestMap(null);

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void parsesEmptyInputForAbTestMappingToEmptyResult() throws Exception {
        Map<String, String> actual = Utils.parseAbTestMap("");

        assertNotNull(actual);
        assertTrue(actual.isEmpty());
    }

    @Test
    public void parsesValidCombinations() throws Exception {
        assertKeyValuePairParsed("key", "key", "");
        assertKeyValuePairParsed("key=", "key", "");
        assertKeyValuePairParsed("key=value", "key", "value");
        assertKeyValuePairParsed("=value", "", "value");
    }

    private void assertKeyValuePairParsed(String input, String key, String expected) {
        Map<String, String> actual = Utils.parseAbTestMap(input);
        assertEquals(expected, actual.get(key));
    }

    @Test
    public void parsesTwoItems() throws Exception {
        Map<String, String> actual = Utils.parseAbTestMap("key1=value1;key2=value2");

        assertEquals("value1", actual.get("key1"));
        assertEquals("value2", actual.get("key2"));
        assertEquals(2, actual.size());
    }

}
