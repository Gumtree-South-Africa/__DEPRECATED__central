package com.ecg.comaas.bt.filter.volume;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationParserTest {

    private static final ObjectNode BASIC_QUOTA_RULE = JsonObjects.builder()
            .attr("allowance", "30")
            .attr("perTimeValue", "1")
            .attr("perTimeUnit", "DAYS")
            .attr("score", "100")
            .build();

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRulesNode() throws Exception {
        JsonNode input = JsonObjects.parse("{}");
        new ConfigurationParser(input).getQuotas();
    }

    @Test
    public void extractsQuotaData() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("allowance", "10").attr("perTimeValue", "1").attr("perTimeUnit", "HOURS").attr("score", "200").build());


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

		List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).attr("runFor", runForNode).build()).getQuotas();
        assertEquals(1, quotas.size());
        Quota quota = quotas.get(0);

        assertEquals(10, quota.getAllowance());
        assertEquals(1, quota.getPerTimeValue());
        assertEquals(TimeUnit.HOURS, quota.getPerTimeUnit());
        assertEquals(200, quota.getScore());
    }

    @Test
    public void acceptsMultipleQuotas() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("allowance", "10").attr("perTimeValue", "1").attr("perTimeUnit", "HOURS").attr("score", "200").build());
        rules.add(BASIC_QUOTA_RULE);

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


        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).attr("runFor", runForNode).build()).getQuotas();
        assertEquals(2, quotas.size());
    }

    @Test
    public void ordersQuotasByScoreDesc() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(BASIC_QUOTA_RULE);
        rules.add(JsonObjects.builder().attr("allowance", "10").attr("perTimeValue", "1").attr("perTimeUnit", "HOURS").attr("score", "200").build());

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

        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).attr("runFor", runForNode).build()).getQuotas();
        assertEquals(200, quotas.get(0).getScore());
    }

    @Test
    public void acceptsScoreMemory() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder()
                .attr("allowance", "10")
                .attr("perTimeValue", "1")
                .attr("perTimeUnit", "HOURS")
                .attr("score", "200")
                .attr("scoreMemoryDurationUnit", "MINUTES")
                .attr("scoreMemoryDurationValue", 10)
                .build());

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

        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).attr("runFor", runForNode).build()).getQuotas();
        assertEquals(1, quotas.size());
        assertEquals(10, quotas.get(0).getScoreMemoryDurationValue());
        assertEquals(TimeUnit.MINUTES, quotas.get(0).getScoreMemoryDurationUnit());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBadScoreMemoryValue() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder()
                .attr("allowance", "10")
                .attr("perTimeValue", "1")
                .attr("perTimeUnit", "HOURS")
                .attr("score", "200")
                .attr("scoreMemoryDurationUnit", "MINUTES")
                .attr("scoreMemoryDurationValue", -1)
                .build());

        new ConfigurationParser(JsonObjects.builder().attr("rules", rules).build());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBadScoreMemoryUnit() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder()
                .attr("allowance", "10")
                .attr("perTimeValue", "1")
                .attr("perTimeUnit", "HOURS")
                .attr("score", "200")
                .attr("scoreMemoryDurationUnit", "YEARS")
                .attr("scoreMemoryDurationValue", 1)
                .build());

        new ConfigurationParser(JsonObjects.builder().attr("rules", rules).build());
    }

    @Test
    public void acceptsIgnoreFollowUpsFlag() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(BASIC_QUOTA_RULE);

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

        ConfigurationParser config = new ConfigurationParser(
                JsonObjects.builder()
                        .attr("rules", rules)
                        .attr("runFor", runForNode)
                        .attr("ignoreFollowUps", true)
                        .build());

        assertTrue(config.isIgnoreFollowUps());
    }

    @Test
    public void doNotIgnoreFollowUpsByDefault() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(BASIC_QUOTA_RULE);

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

        ConfigurationParser config = new ConfigurationParser(
                JsonObjects.builder()
                        .attr("rules", rules)
                        .attr("runFor", runForNode)
                        .build());

        assertFalse(config.isIgnoreFollowUps());
    }
}