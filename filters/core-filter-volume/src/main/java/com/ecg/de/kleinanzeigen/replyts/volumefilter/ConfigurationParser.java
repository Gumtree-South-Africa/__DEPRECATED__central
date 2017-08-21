package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;

/**
 * parses volumefilter configurations and returns a list of quotas ordered by score descending.
 *
 * @author mhuttar
 */
class ConfigurationParser {
    private final ImmutableList<Quota> quotas;

    ConfigurationParser(JsonNode jsonNode) {
        List<Quota> quotas = new ArrayList<>();

        ArrayNode rulesArray = (ArrayNode) jsonNode.get("rules");
        Preconditions.checkArgument(rulesArray != null, "given config does not contain a rules element.");
        for(JsonNode n : rulesArray) {
            quotas.add(convert(n));
        }
        Collections.sort(quotas);
        if(newHashSet(quotas).size() != quotas.size()) {
            throw new RuntimeException("Duplicate Rules found - they are not allowed");
        }

        this.quotas = ImmutableList.copyOf(quotas);
    }

    public List<Quota> get() {
        return quotas;
    }

    private Quota convert(JsonNode n) {
        int allowance = n.get("allowance").asInt();
        int perTimeValue = n.get("perTimeValue").asInt();
        TimeUnit perTimeUnit = TimeUnit.valueOf(n.get("perTimeUnit").asText());
        Preconditions.checkArgument(allowance > 0, "allowance must be greater than 0");
        Preconditions.checkArgument(perTimeValue > 0, "perTimeValue must be greater than 0");
        Preconditions.checkArgument(Lists.newArrayList(TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS).contains(perTimeUnit), "perTimeUnit can only be MINUTES, HOURS or DAYS");
        int score = n.get("score").asInt();
        return new Quota(allowance, perTimeValue, perTimeUnit, score);
    }
}
