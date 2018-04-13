package com.ecg.comaas.bt.filter.volume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Sets.newHashSet;

/**
 * parses volumefilter configurations and returns a list of quotas ordered by score descending.
 *
 * @author mhuttar
 */
class ConfigurationParser {
    private static final Set<TimeUnit> ALLOWED_TIME_UNITS = Sets.immutableEnumSet(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);

    private final ImmutableList<Quota> quotas;
    private final boolean ignoreFollowUps;
    
    private final ImmutableList<Integer> exceptCategoriesList;
    private final ImmutableList<Integer> allowedCategoriesList;
    
    public ConfigurationParser(JsonNode jsonNode) {
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
        this.ignoreFollowUps = jsonNode.has("ignoreFollowUps") && jsonNode.get("ignoreFollowUps").asBoolean(false);
        
		JsonNode  runFor = jsonNode.get("runFor");
		Preconditions.checkArgument(runFor != null, "given config does not contain a runFor element.");
		this.exceptCategoriesList   =  ImmutableList.copyOf(loadExceptCategories(runFor));
		this.allowedCategoriesList  = ImmutableList.copyOf(loadAllowedCategories(runFor));
    }

    public List<Quota> getQuotas() {
        return quotas;
    }

    public boolean isIgnoreFollowUps() {
        return ignoreFollowUps;
    }

    private Quota convert(JsonNode n) {
        int allowance = getAllowance(n);
        int perTimeValue = getPerTimeValue(n);
        TimeUnit perTimeUnit = getPerTimeUnit(n);
        int scoreMemoryDurationValue = getScoreMemoryDurationValue(n);
        TimeUnit scoreMemoryDurationUnit = getScoreMemoryDurationUnit(n);
        int score = getScore(n);

        return new Quota(allowance, perTimeValue, perTimeUnit, score, scoreMemoryDurationValue, scoreMemoryDurationUnit);
    }

    private int getAllowance(JsonNode n) {
        int allowance = n.get("allowance").asInt();
        Preconditions.checkArgument(allowance > 0, "allowance must be greater than 0");
        return allowance;
    }

    private int getPerTimeValue(JsonNode n) {
        int perTimeValue = n.get("perTimeValue").asInt();
        Preconditions.checkArgument(perTimeValue > 0, "perTimeValue must be greater than 0");
        return perTimeValue;
    }

    private TimeUnit getPerTimeUnit(JsonNode n) {
        TimeUnit perTimeUnit = TimeUnit.valueOf(n.get("perTimeUnit").asText());
        Preconditions.checkArgument(ALLOWED_TIME_UNITS.contains(perTimeUnit), "perTimeUnit can only be SECONDS, MINUTES, HOURS or DAYS");
        return perTimeUnit;
    }

    private int getScoreMemoryDurationValue(JsonNode n) {
        JsonNode scoreMemoryDurationValueNode = n.get("scoreMemoryDurationValue");
        int scoreMemoryDurationValue = scoreMemoryDurationValueNode == null ? 0 : scoreMemoryDurationValueNode.asInt(0);
        Preconditions.checkArgument(scoreMemoryDurationValue >= 0, "scoreMemoryDurationValue must be greater than or equal to 0");
        return scoreMemoryDurationValue;
    }

    private TimeUnit getScoreMemoryDurationUnit(JsonNode n) {
        JsonNode scoreMemoryDurationUnitNode = n.get("scoreMemoryDurationUnit");
        TimeUnit scoreMemoryDurationUnit = scoreMemoryDurationUnitNode == null ? TimeUnit.MINUTES : TimeUnit.valueOf(scoreMemoryDurationUnitNode.asText());
        Preconditions.checkArgument(ALLOWED_TIME_UNITS.contains(scoreMemoryDurationUnit), "scoreMemoryDurationUnit can only be SECONDS, MINUTES, HOURS or DAYS");
        return scoreMemoryDurationUnit;
    }

    private int getScore(JsonNode n) {
        return n.get("score").asInt();
    }
    
	private List<Integer> loadAllowedCategories(JsonNode runFor) {
		List<Integer> catList = new ArrayList<Integer>();
		JsonNode arrNode = runFor.get("categories");
		if(arrNode.isArray()){
			for(JsonNode objNode : arrNode ){
				catList.add(objNode.asInt());
			}
		}
		return catList;
	}

	private List<Integer> loadExceptCategories(JsonNode runFor) {
		List<Integer> exceptCatList = new ArrayList<Integer>();
		JsonNode arrNode = runFor.get("exceptCategories");
		if(arrNode.isArray()){
			for(JsonNode objNode : arrNode ){
				exceptCatList.add(objNode.asInt());
			}
		}
		return exceptCatList;
	}

	public List<Integer> getExceptCategoriesList() {
		return exceptCategoriesList;
	}

	public ImmutableList<Integer> getAllowedCategoriesList() {
		return allowedCategoriesList;
	}
}