package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ConfigurationParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMissingRulesNode() throws Exception {
        JsonNode input = JsonObjects.parse("{}");
        new ConfigurationParser(input).get();
    }


    @Test
    public void extractsQuotaData() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("allowance", "10").attr("perTimeValue", "1").attr("perTimeUnit", "HOURS").attr("score", "200").build());

        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).build()).get();
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
        rules.add(JsonObjects.builder().attr("allowance", "30").attr("perTimeValue", "1").attr("perTimeUnit", "DAYS").attr("score", "100").build());

        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).build()).get();
        assertEquals(2, quotas.size());
    }


    @Test
    public void ordersQuotasByScoreDesc() throws Exception {
        ArrayNode rules = JsonObjects.newJsonArray();
        rules.add(JsonObjects.builder().attr("allowance", "30").attr("perTimeValue", "1").attr("perTimeUnit", "DAYS").attr("score", "100").build());
        rules.add(JsonObjects.builder().attr("allowance", "10").attr("perTimeValue", "1").attr("perTimeUnit", "HOURS").attr("score", "200").build());

        List<Quota> quotas = new ConfigurationParser(JsonObjects.builder().attr("rules", rules).build()).get();
        assertEquals(200, quotas.get(0).getScore());
    }
}
