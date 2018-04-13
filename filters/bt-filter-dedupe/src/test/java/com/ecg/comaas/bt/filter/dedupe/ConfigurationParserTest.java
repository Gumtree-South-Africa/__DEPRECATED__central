package com.ecg.comaas.bt.filter.dedupe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ConfigurationParserTest {
    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRulesNode() throws Exception {
        ConfigurationParser.parse(JsonObjects.parse("{ }"));
    }

    @Test
    public void extractsFilterConfigData() throws Exception {
        ObjectNode rulesNode = JsonObjects.builder().attr("minimumShouldMatch", "80%").attr("lookupInterval", "1")
          .attr("lookupIntervalTimeUnit", "HOURS").attr("score", "200").attr("matchCount", 3).build();

        ArrayNode exceptCategoriesNode = JsonObjects.newJsonArray();

        exceptCategoriesNode.add(8);

        ArrayNode categoriesNode = JsonObjects.newJsonArray();

        categoriesNode.add(4);
        categoriesNode.add(9);
        categoriesNode.add(2);
        categoriesNode.add(6);
        categoriesNode.add(5);

        ObjectNode runForNode = JsonObjects.builder().attr("exceptCategories", exceptCategoriesNode)
          .attr("categories", categoriesNode).build();

        FilterConfig filterConfig = ConfigurationParser.parse(JsonObjects.builder().attr("rules", rulesNode).attr("runFor", runForNode).build());

        assertNotNull(filterConfig);
        assertEquals("80%", filterConfig.getMinShouldMatch());
        assertEquals(1, filterConfig.getLookupInterval());
        assertEquals("HOURS", filterConfig.getLookupIntervalTimeUnit());
        assertEquals(200, filterConfig.getScore());
        assertEquals(3, filterConfig.getMatchCount());

        assertNotNull(filterConfig.getExceptCategories());
        assertEquals(1, filterConfig.getExceptCategories().size());
        assertTrue(8 == filterConfig.getExceptCategories().get(0));

        assertNotNull(filterConfig.getCategories());
        assertEquals(5, filterConfig.getCategories().size());
        assertTrue(4 == filterConfig.getCategories().get(0));
        assertTrue(5 == filterConfig.getCategories().get(4));
    }

    @Test
    public void extractsFilterConfigDataWithoutCategories() throws Exception {
        ObjectNode rulesNode = JsonObjects.builder().attr("minimumShouldMatch", "80%").attr("lookupInterval", "1")
          .attr("lookupIntervalTimeUnit", "HOURS").attr("score", "200").attr("matchCount", 3).build();

        ArrayNode exceptCategoriesNode = JsonObjects.newJsonArray();

        ArrayNode categoriesNode = JsonObjects.newJsonArray();

        ObjectNode runForNode = JsonObjects.builder().attr("exceptCategories", exceptCategoriesNode)
          .attr("categories", categoriesNode).build();

        FilterConfig filterConfig = ConfigurationParser.parse(JsonObjects.builder().attr("rules", rulesNode).attr("runFor", runForNode).build());

        assertNotNull(filterConfig);
        assertEquals("80%", filterConfig.getMinShouldMatch());
        assertEquals(1, filterConfig.getLookupInterval());
        assertEquals("HOURS", filterConfig.getLookupIntervalTimeUnit());
        assertEquals(200, filterConfig.getScore());
        assertEquals(3, filterConfig.getMatchCount());

        assertNotNull(filterConfig.getExceptCategories());
        assertEquals(0, filterConfig.getExceptCategories().size());

        assertNotNull(filterConfig.getCategories());
        assertEquals(0, filterConfig.getCategories().size());
    }
}